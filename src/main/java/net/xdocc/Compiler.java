package net.xdocc;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.xdocc.handlers.Handler;

import net.xdocc.handlers.HandlerDirectory;
import net.xdocc.handlers.HandlerLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Compiler {

    private static final Logger LOG = LoggerFactory.getLogger(Compiler.class);

    final private List<Handler> handlers;

    final private Site site;

    final private ExecutorService executorServiceCompiler;
    
    final private Map<String, Integer> filesCounter;
    
    final private Cache cache;

    public Compiler(ExecutorService executorServiceCompiler, Site site, Map<String, Integer> filesCounter, Cache cache) {
        this.executorServiceCompiler = executorServiceCompiler;
        this.handlers = site.handlers();
        for(Handler h:handlers) {
            if(h instanceof HandlerLink) {
                ((HandlerLink)h).compiler(this);
            }
        }
        this.site = site;
        this.filesCounter = filesCounter;
        this.cache = cache;
    }
    
    public CompletableFuture<XItem> compile(final Path path) {
        return compile(path, 0);
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

    public CompletableFuture<XItem> compile(final Path path,
            final int depth) {
        final CompletableFuture<XItem> completableFuture = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {

            try {
                List<XPath> children = Utils.getNonHiddenChildren(site, path);

                LOG.info("compiling: "+children);

                List<CompletableFuture<XItem>> futures = new ArrayList<>();
                final List<XItem> results = new ArrayList<>();

                for (XPath child : children) {
                    final XItem xItem = compile(child);
                    if(xItem != null && !Boolean.TRUE.equals(xItem.getConsumesDirectory())) {
                        xItem.setDepth(depth, depth);
                        results.add(xItem);
                    }

                    if (child.isDirectory() && (xItem == null || !Boolean.TRUE.equals(xItem.getConsumesDirectory()))) {
                        //recursion, only if the directory is not completely consumed by e.g. pandoc
                        futures.add(compile(Paths.get(child.path()), depth + 1));
                    }
                }
                CompletableFuture.allOf(futures.stream()
                                .toArray(size -> new CompletableFuture[size])
                ).thenRunAsync(() -> {
                    results.addAll(
                        futures.stream()
                            .map(v -> v.join())
                            .collect(Collectors.toList())
                    );
                    try {
                        XItem doc = HandlerDirectory.compileList(site,path, results, filesCounter, cache, depth);
                        completableFuture.complete(doc); // or result for flat
                        //completableFuture.complete(results);
                        
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
