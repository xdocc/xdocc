package net.xdocc;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import net.xdocc.handlers.Handler;
import net.xdocc.handlers.HandlerBean;

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

    public CompletableFuture<List<Document>> compile(final Path pointOfView, final Path path) {
        final CompletableFuture<List<Document>> completableFuture = new CompletableFuture<>();

        completableFuture.runAsync(() -> {

            try {
                List<XPath> children = Utils.getNonHiddenChildren(site, path);
                List<CompletableFuture> stream = new ArrayList<>();
                List<Document> results = new ArrayList<>();

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
                        results.addAll((List<Document>) v.getNow(Collections.emptyList()));
                    });                 

                    HandlerBean handlerBean = new HandlerBean();
                    handlerBean.setSite(site);
                    handlerBean.setxPath(new XPath(site, path));

                    Path generatedFile = handlerBean.getxPath().getTargetPath("index.html");

                    try {
                        Document doc = Utils.createDocument(handlerBean.getSite(), 
                                        handlerBean.getxPath(), handlerBean.getRelativePathToRoot(),
                                        null, "list", "directory");
                        doc.setDocuments(results);
                        Utils.writeHTML(handlerBean.getSite(), handlerBean.getxPath(), handlerBean
                                .getRelativePathToRoot(), doc, generatedFile, "multi");
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

    private Document compile(Handler handler, XPath xPath) throws Exception {

        String relativePathToRoot = Utils.relativePathToRoot(site.source(),
                xPath.path());
        HandlerBean handlerBean = new HandlerBean();
        handlerBean.setSite(site);
        handlerBean.setxPath(xPath);
        handlerBean.setRelativePathToRoot(relativePathToRoot);
        return handler.compile(handlerBean, true);
    }
}
