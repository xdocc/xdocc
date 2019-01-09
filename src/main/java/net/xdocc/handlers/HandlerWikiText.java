package net.xdocc.handlers;

import net.xdocc.*;
import org.eclipse.mylyn.wikitext.confluence.ConfluenceLanguage;
import org.eclipse.mylyn.wikitext.mediawiki.MediaWikiLanguage;
import org.eclipse.mylyn.wikitext.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.parser.builder.HtmlDocumentBuilder;
import org.eclipse.mylyn.wikitext.textile.TextileLanguage;
import org.eclipse.mylyn.wikitext.tracwiki.TracWikiLanguage;
import org.eclipse.mylyn.wikitext.twiki.TWikiLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandlerWikiText implements Handler {
    
    public static final Map<String, String> MAP = new HashMap<String, String>();
    static{
        MAP.put("wikitext.ftl", "${content}");
    }

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
    public XItem compile(Site site, XPath xPath, Map<String, Integer> filesCounter, Cache cache)
            throws Exception {
        final XItem doc;
        final Path generatedFile = xPath.resolveTargetFromBasePath(xPath.getTargetURL() + ".html");
        Cache.CacheEntry cached = cache.getCached(site, xPath);
        if (cached != null) {
            doc = cached.xItem();
            if (xPath.getParent().isItemWritten() && xPath.isItemWritten()) {
                Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
            }
        } else {
            String htmlContent = createHTML(xPath);
            doc = Utils.createDocument(site, xPath, htmlContent, "wikitext");

            // always create a single page for that
            if (xPath.getParent().isItemWritten() && xPath.isItemWritten()) {
                Utils.writeHTML(xPath, doc, generatedFile);
                Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
            }

            cache.setCached(site, xPath, (Path)null, doc, generatedFile);
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

        private String createHTML(XPath xPath) throws IOException {
            StringWriter writer = new StringWriter();
            HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer);

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
                return "no parser found for "+ xPath;
            }
            parser.setBuilder(builder);
            Charset charset = HandlerUtils.detectCharset(Paths.get(xPath.path()));
            String rawFileContent = HandlerUtils.readFile(Paths.get(xPath.path()), charset);
            parser.parse(rawFileContent);
            return writer.toString();
        }

        @Override
        public String toString() {
            return "WIKI" + super.toString();
        }



}
