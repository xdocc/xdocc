package net.xdocc.handlers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
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
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.ext.ins.InsExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlerMarkdown implements Handler {

    private static final Logger LOG = LoggerFactory.getLogger(HandlerMarkdown.class);
    
    public static final Map<String, String> MAP = new HashMap<String, String>();
    static{
        MAP.put("markdown.ftl", "${content}");
    }

    private static final List<Extension> extensions = Arrays.asList(
            TablesExtension.create(),
            AutolinkExtension.create(),
            StrikethroughExtension.create(),
            HeadingAnchorExtension.create(),
            InsExtension.create());

    @Override
    public boolean canHandle(Site site, XPath xPath) {
        return xPath.isCompile() && !xPath.isDirectory()
                && HandlerUtils.knowsExtension(knownExtensions(), xPath);
    }

    @Override
    public List<String> knownExtensions() {
        return Arrays.asList(new String[]{"md", "MD", "markdown", "Markdown",
            "MARKDOWN"});
    }

    @Override
    public XItem compile(Site site, XPath xPath, Map<String, Integer> filesCounter, Cache cache) throws Exception {
        final XItem doc;
        final Path generatedFile = xPath.resolveTargetFromBasePath(xPath.getTargetURL() + ".html");
        Cache.CacheEntry cached = cache.getCached(site, xPath);
        if (cached != null) {
            LOG.debug("returning cached markedown entry");
            doc = cached.xItem();
            if (xPath.getParent().isItemWritten() && xPath.isItemWritten()) {
                Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
            }
        } else {
            try (Writer out = new StringWriter()) {
                Charset charset = HandlerUtils.detectCharset(Paths.get(xPath.path()));
                String input = HandlerUtils.readFile(Paths.get(xPath.path()), charset);
                transform(input, out);
                String htmlContent = out.toString();
                doc = Utils.createDocument(site, xPath, htmlContent, "markdown");
                if (xPath.getParent().isItemWritten() && xPath.isItemWritten()) {
                    Utils.writeHTML(xPath, doc, generatedFile);
                    Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
                }
                cache.setCached(site, xPath, (Path)null, doc, generatedFile);
            }
        }
        return doc;
    }

    private void transform(String input, Writer out) {
        Parser parser = Parser.builder().extensions(extensions).build();
        Node document = parser.parse(input);
        HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).build();
        renderer.render(document, out);
    }
}
