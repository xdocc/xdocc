package net.xdocc;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

public class Service {

    private static final Logger LOG = LoggerFactory.getLogger(Service.class);

    private final ExecutorService executorServiceCompiler = Executors
            .newCachedThreadPool();

    private final List<RecursiveWatcherService> watchServices = new ArrayList<>();

    @Option(name = "-w", required = true, usage = "set the directory to watch and recompile on the fly.")
    private String watchDirectory = ".";

    @Option(name = "-o", required = true, usage = "set the directory to store generated files.")
    private String outputDirectory = "/tmp";

    @Option(name = "-c", usage = "set the directory to store the cached data")
    private String cacheDirectory = "/tmp";

    @Option(name = "-r", usage = "run only once")
    private boolean runOnce = false;

    @Option(name = "-x", usage = "clear the cache at startup")
    private boolean clearCache = false;

    public static void main(String... args) throws IOException, InterruptedException, ExecutionException {
        final Service service = new Service();
        service.addShutdownHook();
        service.doMain(args);
    }

    public void doMain(String[] args) throws IOException, InterruptedException, ExecutionException {
        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);

        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.println("xdocc [options...] arguments...");
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();
            // print option sample. This is useful some time
            System.err.println("  Example: xdocc" + parser.printExample(OptionHandlerFilter.ALL));
            shutdown();
            return;
        }

        final CountDownLatch startAfterFirstRun = new CountDownLatch(1);
        final Site site = new Site(this, Paths.get(watchDirectory), Paths.get(outputDirectory));
        final boolean isDaemon = !runOnce;
        if (isDaemon) {
            startWatch(site, new RecursiveWatcherService.Listener() {
                @Override
                public void filesChanged(Site site) {
                    try {
                        LOG.debug("file changed");
                        startAfterFirstRun.await();
                        LOG.info("compiling start: {}", site);
                        final long start = System.currentTimeMillis();
                        compile(site).get();
                        LOG.info("compiling done in {} ms of {}", (System.currentTimeMillis() - start), site);
                    } catch (Throwable t) {
                        LOG.error("file changed, but could not compile", t);
                    }

                }
            });
        }
        LOG.info("compiling start: {}", site);
        final long start = System.currentTimeMillis();
        compile(site).get();
        LOG.info("compiling done in {} ms of {}", (System.currentTimeMillis() - start), site);
        if (isDaemon) {
            startAfterFirstRun.countDown();
        } else {
            shutdown();
        }
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdown();
            }
        });
    }

    public void startWatch(Site site, RecursiveWatcherService.Listener listener) throws IOException {
        RecursiveWatcherService recursiveWatcherService = new RecursiveWatcherService(site, listener);
        watchServices.add(recursiveWatcherService);
    }

    public void shutdown() {
        for (RecursiveWatcherService recursiveWatcherService : watchServices) {
            recursiveWatcherService.shutdown();
        }
        executorServiceCompiler.shutdown();
    }

    public CompletableFuture<List<XItem>> compile(Site site) throws IOException, InterruptedException, ExecutionException {
        Compiler c = new Compiler(executorServiceCompiler, site, new HashSet<>());
        return c.compile(site.source());
    }
}
