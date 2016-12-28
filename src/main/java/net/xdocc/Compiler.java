package net.xdocc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
                List<CompletableFuture<List<XItem>>> futures = new ArrayList<>();
                List<CompletableFuture<List<XItem>>> futuresNoPromote = new ArrayList<>();
                List<XItem> results = new ArrayList<>();

                for (XPath child : children) {
                    if (child.isDirectory()) {
                        if(child.isPromoted()) {
                            futures.add(compile(child.path(), child.path()));
                        } else {
                            futuresNoPromote.add(compile(child.path(), child.path()));
                        }
                    } else {
                        for (Handler handler : handlers) {
                            if (handler.canHandle(site, child)) {
                                results.add(compile(handler, child));
                                break;
                            }
                        }
                    }
                }
                CompletableFuture.allOf(Stream
                                .concat(futures.stream(), futuresNoPromote.stream())
                                .toArray(size -> new CompletableFuture[size])
                ).thenRunAsync(() -> {
                    results.addAll(
                        futures.stream()
                            .map(v -> v.getNow(Collections.emptyList()))
                            .flatMap(List::stream)
                            .collect(Collectors.toList())
                    );

                    XPath xPath = new XPath(site, path);
                    if(!xPath.isNoIndex()) {
                        
                        Path generatedFile = xPath.resolveTargetFromPath("index.html");
                        try {
                            XList doc = Utils.createList(site, xPath);
                            doc.setItems(results);
                            Utils.writeListHTML(site, xPath, "", doc, generatedFile);
                        } catch (Throwable t) {
                            LOG.error("compiler error", t);
                            completableFuture.completeExceptionally(t);
                        }
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
