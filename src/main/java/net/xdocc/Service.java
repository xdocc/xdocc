package net.xdocc;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        if (!runOnce) {
            startWatch(site, new RecursiveWatcherService.Listener() {
                @Override
                public void filesChanged(Site site) {
                    try {
                        LOG.debug("file changed");
                        startAfterFirstRun.await();
                        compile(site);
                    } catch (Throwable t) {
                        LOG.error("file changed, but could not compile", t);
                    }

                }
            });
        }
        compile(site);
        startAfterFirstRun.countDown();
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
        executorServiceCompiler.shutdownNow();
    }

    public void compile(Site site) throws IOException, InterruptedException, ExecutionException {
        compile(site, site.source(), new HashMap<String, Object>());
    }

    public void compile(Site site, Path path, Map<String, Object> model)
            throws IOException, InterruptedException, ExecutionException {
        LOG.info("compiling start: {} / {}", site, path);
        final long start = System.currentTimeMillis();
        Compiler c = new Compiler(executorServiceCompiler, site);
        c.compile(path, path).get();
        LOG.info("compiling done in {} ms of {} / {}", (System.currentTimeMillis() - start), site, path);
    }

}
