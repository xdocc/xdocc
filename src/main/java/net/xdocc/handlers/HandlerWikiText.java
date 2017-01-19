package net.xdocc.handlers;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import net.xdocc.XItem;
import net.xdocc.Site;
import net.xdocc.Site.TemplateBean;
import net.xdocc.Utils;
import net.xdocc.XPath;

import org.apache.commons.io.FileUtils;
import org.eclipse.mylyn.wikitext.confluence.core.ConfluenceLanguage;
import org.eclipse.mylyn.wikitext.core.parser.Attributes;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder;
import org.eclipse.mylyn.wikitext.mediawiki.core.MediaWikiLanguage;
import org.eclipse.mylyn.wikitext.textile.core.TextileLanguage;
import org.eclipse.mylyn.wikitext.tracwiki.core.TracWikiLanguage;
import org.eclipse.mylyn.wikitext.twiki.core.TWikiLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlerWikiText implements Handler {

    private static final Logger LOG = LoggerFactory
            .getLogger(HandlerWikiText.class);

    final private static String[] MEDIA_WIKI_EXT = {"mediawiki", "Mediawiki",
        "MediaWiki", "MEDIAWIKI"};
    final private static String[] TEXTILE_EXT = {"textile", "Textile",
        "TEXTILE"};
    final private static String[] TRAC_WIKI_EXT = {"tracwiki", "Tracwiki",
        "TracWiki", "TRACWIKI"};
    final private static String[] T_WIKI_EXT = {"twiki", "twiki", "tWiki",
        "TWIKI"};
    final private static String[] CONFLUENCE_EXT = {"confluence",
        "Confluence", "CONFLUENCE"};

    @Override
    public boolean canHandle(Site site, XPath xPath) {
        return xPath.isCompile() && !xPath.isDirectory()
                && HandlerUtils.knowsExtension(knownExtensions(), xPath);
    }

    @Override
    public XItem compile(Site site, XPath xPath)
            throws Exception {
        //do Utils.createDocument manually here		
        TemplateBean templateText = site.getTemplate("wikitext", xPath.getLayoutSuffix());
        WikiTextDocumentGenerator documentGenerator = new WikiTextDocumentGenerator(
                templateText, site, xPath);

        
        XItem doc = documentGenerator.xItem();
                
                
        doc.setTemplate("wikitext");

        // always create a single page for that
        if (xPath.getParent().isItemWritten()) {
            Path generatedFile = xPath
                    .resolveTargetFromBasePath(xPath.getTargetURL() + ".html");
            Utils.writeHTML(site, xPath, doc, generatedFile);
        }
        return doc;
    }

    @Override
    public List<String> knownExtensions() {
        return Arrays.asList(join(MEDIA_WIKI_EXT, TEXTILE_EXT, TRAC_WIKI_EXT,
                T_WIKI_EXT, CONFLUENCE_EXT));
    }

    public static String[] join(String[]... parms) {
        // calculate size of target array
        int size = 0;
        for (String[] array : parms) {
            size += array.length;
        }

        String[] result = new String[size];

        int j = 0;
        for (String[] array : parms) {
            for (String s : array) {
                result[j++] = s;
            }
        }
        return result;
    }

    //https://github.com/eclipse/mylyn.docs/tree/master/org.eclipse.mylyn.wikitext.core/src/org/eclipse/mylyn/wikitext/core/parser/builder
    private static class XdoccHtmlDocumentBuilder extends HtmlDocumentBuilder {

        final private Site site;
        final private XItem xItem;
        private String htmlNsUri = "http://www.w3.org/1999/xhtml";

        public XdoccHtmlDocumentBuilder(Writer out, Site site, XItem xItem) {
            super(out);
            this.site = site;
            this.xItem = xItem;
        }

        @Override
        public void image(Attributes attributes, String imageUrl) {
            if (imageUrl != null) {
                //if relative only!
                if (!imageUrl.contains("://")) {
                    if (imageUrl.endsWith(":thumb")) {
                        String tmpImageUrl = imageUrl.replaceAll(":thumb$", "");
                        //convert
                        HandlerImage handler = new HandlerImage();
                        try {
                            XPath img = xItem.xPath().getParent().resolveSource(tmpImageUrl);
                            XItem item = handler.convertNorm(site, img, false);
                            String href = item.getLink();
                            super.imageLink(attributes, href, imageUrl);
                            return;
                        } catch (TemplateException | IOException | InterruptedException ex) {
                            LOG.error("cannot convert", ex);
                            imageUrl = imageUrl.replaceAll(":thumb$", "");
                        }
                    } else {
                        imageUrl = xItem.getPath() + "/" + imageUrl;
                    }
                }
            }
            super.image(attributes, imageUrl);
        }

        @Override
        public void imageLink(Attributes linkAttributes, Attributes imageAttributes, String href, String imageUrl) {
            if (imageUrl != null) {
                //if relative only!
                if (!imageUrl.contains("://")) {
                    if (imageUrl.endsWith(":thumb")) {
                        imageUrl = imageUrl.replaceAll(":thumb$", "");
                        //convert
                        HandlerImage handler = new HandlerImage();
                        try {
                            XPath img = xItem.xPath().getParent().resolveSource(imageUrl);
                            XItem item = handler.convertThumb(site, img, true);
                            imageUrl = item.getOriginalPath();
                        } catch (TemplateException | IOException | InterruptedException ex) {
                            LOG.error("cannot convert", ex);
                        }
                    } else {
                        imageUrl = xItem.getPath() + "/" + imageUrl;
                    }
                }
            }
            super.imageLink(linkAttributes, imageAttributes, href, imageUrl);
        }
        
        

        @Override
        protected void emitAnchorHref(String href) {
            if (href != null) {
                //if relative only!
                if (!href.contains("://")) {
                    href = xItem.getPath() + "/" + href;        
                }
            }
            super.emitAnchorHref(href);
        }
    }

    private class WikiTextDocumentGenerator extends XItem.FillGenerator {

        private static final long serialVersionUID = -6008311072604987744L;
        final private Site site;
        final private XPath xPath;
        final private XItem xItem;

        public WikiTextDocumentGenerator(TemplateBean templateText, Site site, XPath xPath) {
            super(site, templateText);
            this.site = site;
            this.xPath = xPath;
            this.xItem = new XItem(xPath, this);
        }

        public String generate() {
            try {
                fillModel();
                return super.generate();
            } catch (IOException e) {
                LOG.warn("cannot generate wiki document {}.", templateBean().file().getFileName(), e);
            }
            return null;
        }

        private void fillModel() throws IOException {
            StringWriter writer = new StringWriter();
            HtmlDocumentBuilder builder = new XdoccHtmlDocumentBuilder(writer,
                    site, xItem);

            // avoid the <html> and <body> tags
            MarkupParser parser = null;
            builder.setEmitAsDocument(false);
            for (String ext : TEXTILE_EXT) {
                if (xPath.extensionList().contains(ext)) {
                    parser = new MarkupParser(new TextileLanguage());
                    //type = "textile";
                }
            }
            for (String ext : MEDIA_WIKI_EXT) {
                if (xPath.extensionList().contains(ext)) {
                    parser = new MarkupParser(new MediaWikiLanguage());
                    //type = "mediawiki";
                }
            }
            for (String ext : TRAC_WIKI_EXT) {
                if (xPath.extensionList().contains(ext)) {
                    parser = new MarkupParser(new TracWikiLanguage());
                    //type = "tracwiki";
                }
            }
            for (String ext : T_WIKI_EXT) {
                if (xPath.extensionList().contains(ext)) {
                    parser = new MarkupParser(new TWikiLanguage());
                    //type = "twiki";
                }
            }
            for (String ext : CONFLUENCE_EXT) {
                if (xPath.extensionList().contains(ext)) {
                    parser = new MarkupParser(new ConfluenceLanguage());
                    //type = "confluence";
                }
            }
            if (parser == null) {
                LOG.error("unknown type for {}", xPath);
                return;
            }
            parser.setBuilder(builder);
            Charset charset = HandlerUtils.detectCharset(xPath.path());
            String rawFileContent = FileUtils.readFileToString(xPath.path()
                    .toFile(), charset);
            parser.parse(rawFileContent);
            model().put(XItem.CONTENT, writer.toString());
        }

        @Override
        public String toString() {
            return "WIKI" + super.toString();
        }

        private XItem xItem() {
            return xItem;
        }
    }
}
