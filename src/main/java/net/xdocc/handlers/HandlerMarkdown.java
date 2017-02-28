package net.xdocc.handlers;

import static org.tautua.markdownpapers.util.Utils.escape;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.xdocc.Cache;

import net.xdocc.XItem;
import net.xdocc.Site;
import net.xdocc.Utils;
import net.xdocc.XPath;

import org.tautua.markdownpapers.HtmlEmitter;
import org.tautua.markdownpapers.ast.Image;
import org.tautua.markdownpapers.ast.Resource;
import org.tautua.markdownpapers.parser.ParseException;
import org.tautua.markdownpapers.parser.Parser;

public class HandlerMarkdown implements Handler {
    
    public static final Map<String, String> MAP = new HashMap<String, String>() {{
        put("md.ftl", "${content}");
    }};

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
            try (Writer out = new StringWriter();
                    Reader in = new BufferedReader(new FileReader(xPath.path()
                            .toFile()))) {
                transform(in, out);
                String htmlContent = out.toString();
                doc = Utils.createDocument(site, xPath, htmlContent, "markdown");
                if (xPath.getParent().isItemWritten()) {
                    Utils.writeHTML(xPath, doc, generatedFile);
                    Utils.increase(filesCounter, Utils.listPaths(site, generatedFile));
                }
                cache.setCached(xPath, doc, generatedFile);
            }
        }
        return doc;
    }

    private void transform(Reader in, Writer out) throws ParseException {
        Parser parser = new Parser(in);
        HtmlEmitter emitter = new MyHtmlEmitter(out);
        org.tautua.markdownpapers.ast.Document document = parser.parse();
        document.accept(emitter);
    }

    private static class MyHtmlEmitter extends HtmlEmitter {

        private Appendable buffer;

        public MyHtmlEmitter(Appendable buffer) {
            super(buffer);
            this.buffer = buffer;
        }

        public void visit(Image node) {
            Resource resource = node.getResource();
            if (resource == null) {
                myAppend("<img src=\"\" alt=\"");
                myEscapeAndAppend(node.getText());
                myAppend("\"/>");
            } else {
                myAppend("<img");
                myAppend(" src=\"");
                myEscapeAndAppend(resource.getLocation());
                if (node.getText() != null) {
                    myAppend("\" alt=\"");
                    myEscapeAndAppend(node.getText());
                }
                if (resource.getHint() != null) {
                    myAppend("\" title=\"");
                    myEscapeAndAppend(resource.getHint());
                }
                myAppend("\"/>");
            }
        }

        private void myAppend(String val) {
            try {
                buffer.append(val);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void myEscapeAndAppend(String val) {
            for (char character : val.toCharArray()) {
                myAppend(escape(character));
            }
        }

    }
}
