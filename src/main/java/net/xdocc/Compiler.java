package net.xdocc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import net.xdocc.handlers.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Compiler {

    private static final Logger LOG = LoggerFactory.getLogger(Compiler.class);

    final private List<Handler> handlers;

    final private Site site;

    final private ExecutorService executorServiceCompiler;

    public Compiler(ExecutorService executorServiceCompiler, Site site) {
        this.executorServiceCompiler = executorServiceCompiler;
        this.handlers = site.handlers();
        this.site = site;
    }

    public CompletableFuture<List<XItem>> compile(final Path pointOfView, final Path path) {
        final CompletableFuture<List<XItem>> completableFuture = new CompletableFuture<>();

        completableFuture.runAsync(() -> {

            try {
                List<XPath> children = Utils.getNonHiddenChildren(site, path);
                List<CompletableFuture> stream = new ArrayList<>();
                List<XItem> results = new ArrayList<>();

                for (XPath item : children) {
                    if (item.isDirectory()) {
                        stream.add(compile(item.path(), item.path()));
                    } else {
                        for (Handler handler : handlers) {
                            if (handler.canHandle(site, item)) {
                                results.add(compile(handler, item));
                                break;
                            }
                        }
                    }
                }
                completableFuture.allOf(stream.toArray(new CompletableFuture[0])).thenRunAsync(() -> {
                    stream.stream().forEach((CompletableFuture v) -> {
                        results.addAll((List<XItem>) v.getNow(Collections.emptyList()));
                    });                 

                    XPath xPath = new XPath(site, path);

                    Path generatedFile = xPath.resolveTargetFromPath("index.html");

                    try {
                        XList doc = Utils.createList(site, xPath);
                        doc.setItems(results);
                        
                        Utils.writeListHTML(site, xPath, "", doc, generatedFile);
                    } catch (Throwable t) {
                        LOG.error("compiler error", t);
                        completableFuture.completeExceptionally(t);
                    }
                    
                    completableFuture.complete(results);
                    
                }, executorServiceCompiler);
            } catch (Throwable t) {
                LOG.error("compiler error", t);
                completableFuture.completeExceptionally(t);
            }

        }, executorServiceCompiler);

        return completableFuture;
    }

    private XItem compile(Handler handler, XPath xPath) throws Exception {

        String relativePathToRoot = Utils.relativePathToRoot(site.source(),
                xPath.path());
       
        return handler.compile(site, xPath, new HashMap<String, Object>(), relativePathToRoot);
    }
}
