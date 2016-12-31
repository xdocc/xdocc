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
        return compile(pointOfView, path, 0, 0);
    }

    public CompletableFuture<List<XItem>> compile(final Path pointOfView, final Path path, 
            final int depth, final int promoteDepth) {
        final CompletableFuture<List<XItem>> completableFuture = new CompletableFuture<>();

        completableFuture.runAsync(() -> {

            try {
                List<XPath> children = Utils.getNonHiddenChildren(site, path);
                List<CompletableFuture<List<XItem>>> futures = new ArrayList<>();
                List<CompletableFuture<List<XItem>>> futuresNoPromote = new ArrayList<>();
                final List<XItem> results = new ArrayList<>();

                for (XPath child : children) {
                    if (child.isDirectory()) {
                        if(child.isPromoted()) {
                            futures.add(compile(child.path(), child.path(), depth + 1, promoteDepth + 1));
                        } else {
                            futuresNoPromote.add(compile(child.path(), child.path(), depth + 1, 0));
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
                    
                    final boolean ascending;
                    if (xPath.isAutoSort()) {
                        ascending = Utils.guessAutoSort1(results);
                    } else {
                        ascending = xPath.isAscending();
                    }
                    Utils.sort3(results, ascending);

                    try {
                        XItem doc = Utils.createDocument(site, xPath, null, "list");
                        doc.setItems(results);
                        doc.setDepth(depth, promoteDepth);
                        
                        if(!xPath.isNoIndex()) {
                            Path generatedFile = xPath.resolveTargetFromPath("index.html");
                            Utils.writeListHTML(site, xPath, doc, generatedFile);
                        }
                        
                        List<XItem> results2 = new ArrayList<>(1);
                        results2.add(doc);
                        completableFuture.complete(results2); // or result for flat
                        
                    } catch (Throwable t) {
                        LOG.error("compiler error", t);
                        completableFuture.completeExceptionally(t);
                    }
                    
                    
                }, executorServiceCompiler);
            } catch (Throwable t) {
                LOG.error("compiler error", t);
                completableFuture.completeExceptionally(t);
            }

        }, executorServiceCompiler);

        return completableFuture;
    }

    private XItem compile(Handler handler, XPath xPath) throws Exception {

        String relativePathToRoot = Utils.relativePathToRoot(site.source(), xPath.path());
        return handler.compile(site, xPath, new HashMap<String, Object>(), relativePathToRoot);
    }
}
