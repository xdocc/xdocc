package net.xdocc.handlers;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.xdocc.Cache;

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
    
    public static final Map<String, String> MAP = new HashMap<String, String>() {{
        put("wikitext.ftl", "${content}");
    }};

    private static final Logger LOG = LoggerFactory.getLogger(HandlerWikiText.class);

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
    public XItem compile(Site site, XPath xPath, Map<Path, Integer> filesCounter, Cache cache)
            throws Exception {
        final XItem doc;
        final Path generatedFile = xPath.resolveTargetFromBasePath(xPath.getTargetURL() + ".html");
        Cache.CacheEntry cached = cache.getCached(xPath);
        if (cached != null) {
            doc = cached.xItem();
            if (xPath.getParent().isItemWritten()) {
                Utils.increase(filesCounter, Utils.listPaths(site, generatedFile));
            }
        } else {

            TemplateBean templateText = site.getTemplate("wikitext", xPath.getLayoutSuffix());
            WikiTextDocumentGenerator documentGenerator = new WikiTextDocumentGenerator(
                    templateText, site, xPath, filesCounter, cache);

            doc = documentGenerator.xItem();
            doc.setTemplate("wikitext");

            // always create a single page for that
            if (xPath.getParent().isItemWritten()) {                
                Utils.writeHTML(xPath, doc, generatedFile);
                Utils.increase(filesCounter, Utils.listPaths(site, generatedFile));
            }

            cache.setCached(xPath, doc, generatedFile);
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
        final private Map<Path, Integer> filesCounter;
        final private Cache cache;

        public XdoccHtmlDocumentBuilder(Writer out, Site site, XItem xItem, Map<Path, Integer> filesCounter,
                Cache cache) {
            super(out);
            this.site = site;
            this.xItem = xItem;
            this.filesCounter = filesCounter;
            this.cache = cache;
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
                            XItem item = handler.convertNorm(site, img, false, filesCounter, cache);
                            String href = item.getLink();
                            super.imageLink(attributes, href, imageUrl);
                            return;
                        } catch (TemplateException | IOException | InterruptedException ex) {
                            LOG.error("cannot convert", ex);
                            imageUrl = imageUrl.replaceAll(":thumb$", "");
                        }
                    } else {
                        XPath img = xItem.xPath().getParent().resolveSource(imageUrl);
                        imageUrl = xItem.getPath() + "/" + imageUrl;
                        Path generatedFile = img.resolveTargetFromBasePath(img.getTargetURL() + img
                                .extensions());
                        Utils.increase(filesCounter, Utils.listPaths(site, generatedFile));
                    }
                }
            }
            super.image(attributes, imageUrl);
        }

        @Override
        public void imageLink(Attributes linkAttributes, Attributes imageAttributes, String href,
                String imageUrl) {
            if (imageUrl != null) {
                //if relative only!
                if (!imageUrl.contains("://")) {
                    if (imageUrl.endsWith(":thumb")) {
                        imageUrl = imageUrl.replaceAll(":thumb$", "");
                        //convert
                        HandlerImage handler = new HandlerImage();
                        try {
                            XPath img = xItem.xPath().getParent().resolveSource(imageUrl);
                            XItem item = handler.convertThumb(site, img, true, filesCounter, cache);
                            item = Utils.adjust(img, item);
                            imageUrl = xItem.getPath() + "/" + item.getPath();
                            //the original image gets copied, but we don't want that, since we have requested
                            //to show a thumbnail with a normal sized image
                            Path generatedFile = img.resolveTargetFromBasePath(img.getTargetURL() + img
                                    .extensions());
                            Utils.decrease(filesCounter, Utils.listPaths(site, generatedFile));
                        } catch (TemplateException | IOException | InterruptedException ex) {
                            LOG.error("cannot convert", ex);
                        }
                    } else {
                        XPath img = xItem.xPath().getParent().resolveSource(imageUrl);
                        imageUrl = xItem.getPath() + "/" + imageUrl;
                        Path generatedFile = img.resolveTargetFromBasePath(img.getTargetURL() + img
                                .extensions());
                        Utils.increase(filesCounter, Utils.listPaths(site, generatedFile));
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
                    try {
                        XPath img = xItem.xPath().getParent().resolveSource(href);
                        href = xItem.getPath() + "/" + href;
                        Path generatedFile = img.resolveTargetFromBasePath(img.getTargetURL() + img.extensions());
                        Utils.increase(filesCounter, Utils.listPaths(site, generatedFile));
                    } catch (IllegalArgumentException e) {
                        //try to find a source to for conversion, TODO: check what to do here
                    }
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
        final private Map<Path, Integer> filesCounter;
        final private Cache cache;

        public WikiTextDocumentGenerator(TemplateBean templateText, Site site, XPath xPath,
                Map<Path, Integer> filesCounter, Cache cache) {
            super(site, templateText);
            this.site = site;
            this.xPath = xPath;
            this.xItem = new XItem(xPath, this);
            this.filesCounter = filesCounter;
            this.cache = cache;
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
                    site, xItem, filesCounter, cache);

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
