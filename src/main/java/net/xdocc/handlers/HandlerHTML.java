package net.xdocc.handlers;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.xdocc.Cache;

import net.xdocc.XItem;
import net.xdocc.Site;
import net.xdocc.Utils;
import net.xdocc.XPath;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

public class HandlerHTML implements Handler {

    // final private static Logger LOG = LoggerFactory.getLogger(
    // HandlerText.class );
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
    public XItem compile(Site site, XPath xPath, Map<Path, Integer> filesCounter, Cache cache) throws Exception {

        final XItem doc;
        final Path generatedFile = xPath.resolveTargetFromBasePath(xPath.getTargetURL() + ".html");
        Cache.CacheEntry cached = cache.getCached(xPath);
        if (cached != null) {
            doc = cached.xItem();
            if (xPath.getParent().isItemWritten()) {
                Utils.increase(filesCounter, Utils.listPaths(site, generatedFile));
            }
        } else {
            Charset charset = HandlerUtils.detectCharset(xPath.path());
            String all = FileUtils.readFileToString(xPath.path().toFile(), charset);

            org.jsoup.nodes.Document docj = Jsoup.parse(all);
            Elements e = docj.getElementsByTag("body");

            String htmlContent = e.toString();
            doc = Utils.createDocument(site, xPath, htmlContent, "text");
            // always create a single page for that

            if (xPath.getParent().isItemWritten()) {
                Utils.writeHTML(xPath, doc, generatedFile);
                Utils.increase(filesCounter, Utils.listPaths(site, generatedFile));
            }
            cache.setCached(xPath, doc, generatedFile);
        }
        return doc;
    }
}
