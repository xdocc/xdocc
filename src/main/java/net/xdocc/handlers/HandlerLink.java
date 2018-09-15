package net.xdocc.handlers;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import net.xdocc.*;

import net.xdocc.Compiler;

public class HandlerLink implements Handler {

    private Compiler compiler;
    
    public static final Map<String, String> MAP = new HashMap<String, String>();
    static{
        MAP.put("link.ftl", "<#list items as item>${item.content}</#list>");
    }

    @Override
    public boolean canHandle(Site site, XPath xPath) {
        return xPath.isCompile() && !xPath.isDirectory()
                && HandlerUtils.knowsExtension(knownExtensions(), xPath);
    }

    @Override
    public List<String> knownExtensions() {
        return Arrays.asList(new String[]{"link", "Link", "LINK"});
    }

    @Override
    public XItem compile(Site site, XPath xPath, Map<String, Integer> filesCounter, Cache cache) throws Exception {

        final XItem doc;
        final Path generatedFile = xPath.resolveTargetFromBasePath(xPath.getTargetURL() + ".html");
        Cache.CacheEntry cached = cache.getCached(site, xPath);
        if (cached != null) {
            doc = cached.xItem();
            if (xPath.getParent().isItemWritten()) {
                Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
            }
        } else {
            Charset charset = HandlerUtils.detectCharset(Paths.get(xPath.path()));
            String input = HandlerUtils.readFile(Paths.get(xPath.path()), charset);
            StringReader reader = new StringReader(input);
            Properties prop = new Properties();

            try {
                // load from input stream
                prop.load(reader);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            List<XItem> documents = findDocuments(site, xPath, prop, "");


            if (documents == null) {
                return null;
            } else {
                for(int i=0;true;i++) {
                    List<XItem> tmpDocuments = findDocuments(site, xPath, prop, ""+i);
                    if(tmpDocuments == null) {
                        break;
                    }
                    documents.addAll(tmpDocuments);
                }

                doc = Utils.createDocument(site, xPath, null, "link");
                doc.setItems(documents);
                
                // always create a single page for that
                if (xPath.getParent().isItemWritten()) {
                    Utils.writeHTML(xPath, doc, generatedFile);
                    Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
                }
                cache.setCached(site, xPath, (Path)null, doc, generatedFile);
            }

        }
        return doc;
    }

    private List<XItem> findDocuments(Site site, XPath xPath, Properties prop, String suffix) throws Exception {

        String url = prop.getProperty("url"+suffix);
        if(url == null) {
            return null;
        }
        int limit = Integer.parseInt(prop.getProperty("limit","-1"));

        List<XPath> founds = Utils.findURL(site, xPath, (String) url);

        if (founds.isEmpty() || (founds.size() > 0 && !founds.get(0).isVisible())) {
            return null;
        } else {
            List<XItem> documents = new ArrayList<>();

            final boolean ascending;
            if (xPath.isAutoSort()) {
                ascending = Utils.guessAutoSort(founds);
            } else {
                ascending = xPath.isAscending();
            }
            Utils.sort2(founds, ascending);

            int counter = 0;
            for (XPath found : founds) {

                if(found.isDirectory()) {
                    CompletableFuture<XItem> list = compiler.compile(Paths.get(found.path()), 0);
                    documents.add(list.get());
                } else {
                    documents.add(compiler.compile(found));
                }
                //enforce limit
                if (limit >= 0 && ++counter >= limit) {
                    break;
                }
            }
            return documents;
        }
    }

    public void compiler(Compiler compiler) {
        this.compiler = compiler;
    }
}
