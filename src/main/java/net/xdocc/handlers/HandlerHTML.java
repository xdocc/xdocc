package net.xdocc.handlers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.xdocc.Cache;

import net.xdocc.XItem;
import net.xdocc.Site;
import net.xdocc.Utils;
import net.xdocc.XPath;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeFilter;

public class HandlerHTML implements Handler {
    
    public static final Map<String, String> MAP = new HashMap<String, String>();
    static{
        MAP.put("html.ftl", "${content}");
    }
            
    @Override
    public boolean canHandle(Site site, XPath xPath) {
        return xPath.isCompile() && !xPath.isDirectory()
                && HandlerUtils.knowsExtension(knownExtensions(), xPath);
    }

    @Override
    public List<String> knownExtensions() {
        return Arrays.asList(new String[]{"html", "HTML", "htm",
            "HTM"});
    }

    @Override
    public XItem compile(Site site, XPath xPath, Map<String, Integer> filesCounter, Cache cache) throws Exception {

        final XItem doc;
        final Path generatedFile = xPath.resolveTargetFromBasePath(xPath.getTargetURL() + ".html");
        Cache.CacheEntry cached = cache.getCached(site, xPath);
        if (cached != null) {
            doc = cached.xItem();
            if (xPath.getParent().isItemWritten() && xPath.isItemWritten()) {
                Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
            }
        } else {
            String htmlContent = htmlContent(xPath.path());
            doc = Utils.createDocument(site, xPath, htmlContent, "text");
            // always create a single page for that

            if (xPath.getParent().isItemWritten() && xPath.isItemWritten()) {
                Utils.writeHTML(xPath, doc, generatedFile);
                Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
            }
            cache.setCached(site, xPath, (Path)null, doc, generatedFile);
        }
        return doc;
    }

    public static String htmlContent(String file) throws IOException {
        Charset charset = HandlerUtils.detectCharset(Paths.get(file));
        String all = HandlerUtils.readFile(Paths.get(file), charset);
        org.jsoup.nodes.Document docj = Jsoup.parse(all);
        Elements e = docj.getElementsByTag("body");
        e = e.filter(new NodeFilter() {
            @Override
            public FilterResult head(Node node, int i) {
                if(node.nodeName().equals("header")) {
                    return FilterResult.REMOVE;
                }
                if(node.nodeName().equals("div") && node.attr("class").equals("NAVHEADER")) {
                    return FilterResult.REMOVE;
                }
                if(node.nodeName().equals("div") && node.attr("class").equals("NAVFOOTER")) {
                    return FilterResult.REMOVE;
                }
                return FilterResult.CONTINUE;
            }

            @Override
            public FilterResult tail(Node node, int i) {
                return FilterResult.CONTINUE;
            }
        });
        return e.toString();
    }
}
