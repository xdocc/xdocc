package net.xdocc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    
    final private Map<Path, Integer> filesCounter;
    
    final private Cache cache;

    public Compiler(ExecutorService executorServiceCompiler, Site site, Map<Path, Integer> filesCounter, Cache cache) {
        this.executorServiceCompiler = executorServiceCompiler;
        this.handlers = site.handlers();
        this.site = site;
        this.site.compiler(this);
        this.filesCounter = filesCounter;
        this.cache = cache;
    }
    
    public CompletableFuture<List<XItem>> compile(final Path path) {
        return compile(path, 0, 0);
    }
    
    public XItem compile(XPath child) throws Exception {
        for (Handler handler : handlers) {
            if (handler.canHandle(site, child)) {
                final XItem xItem = handler.compile(site, child, filesCounter, cache);
                if(xItem != null) {
                    return xItem;
                }
            }
        }
        return null;
    }

    public CompletableFuture<List<XItem>> compile(final Path path, 
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
                            futures.add(compile(child.path(), depth + 1, promoteDepth + 1));
                        } else {
                            futuresNoPromote.add(compile(child.path(), depth + 1, 0));
                        }
                    } else {
                        final XItem xItem = compile(child);
                        if(xItem != null) {
                            xItem.setDepth(depth, promoteDepth);
                            results.add(xItem);
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
                            Utils.writeListHTML(xPath, doc, generatedFile);
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
}
