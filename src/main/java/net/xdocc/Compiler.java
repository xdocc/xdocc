package net.xdocc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import net.xdocc.CompileResult.Key;
import net.xdocc.handlers.Handler;
import net.xdocc.handlers.HandlerBean;
import net.xdocc.handlers.HandlerCopy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Compiler  {

    private static final Logger LOG = LoggerFactory.getLogger(Compiler.class);

    final private List<Handler> handlers;

    final private Site site;

    final private ExecutorService executorServiceCompiler;

    public Compiler(ExecutorService executorServiceCompiler, Site site) {
        this.executorServiceCompiler = executorServiceCompiler;
        this.handlers = site.handlers();
        this.site = site;
    }

    public CompletableFuture<List<CompileResult>> compile(final Path pointOfView, final Path path) {
        final CompletableFuture<List<CompileResult>> completableFuture = new CompletableFuture<>();

        completableFuture.runAsync(() -> {

            try {
                List<XPath> children = Utils.getNonHiddenChildren(site, path);
                List<CompletableFuture> stream = new ArrayList<>();
                List<CompileResult> results = new ArrayList<>();
                
                for (XPath item : children) {
                    if (item.isDirectory()) {
                        stream.add(compile(item.path(), item.path()));
                    } else {
                        for (Handler handler : handlers) {
                            if (handler.canHandle(site, item)) {
                                results.add(compile(handler, item));
                            }
                        }
                    }
                }
                completableFuture.allOf(stream.toArray(new CompletableFuture[0])).thenRunAsync(() -> {
                    stream.stream().forEach((CompletableFuture v) -> {
                        results.addAll((List<CompileResult>)v.getNow(Collections.emptyList()));
                    });
                    //now we have all files and folders
                    completableFuture.complete(results);
                }, executorServiceCompiler);
            } catch (Throwable t) {
                completableFuture.completeExceptionally(t);
            }

        }, executorServiceCompiler);

        return completableFuture;
    }

    private CompileResult compile(Handler handler, XPath xPath) throws Exception {

        String relativePathToRoot = Utils.relativePathToRoot(site.source(),
                xPath.path());
        HandlerBean handlerBean = new HandlerBean();
        handlerBean.setSite(site);
        handlerBean.setxPath(xPath);
        handlerBean.setRelativePathToRoot(relativePathToRoot);
        return handler.compile(handlerBean, true);
    }
}
