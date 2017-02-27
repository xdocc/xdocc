package net.xdocc;

import com.google.common.collect.HashBiMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import java.util.stream.Collectors;

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
    
    private Cache cache;
    private static Service service;

    public static void main(String... args) throws IOException, InterruptedException, ExecutionException {
        service = new Service();
        service.addShutdownHook();
        service.cmdLine(args);
        service.doMain();
    }
    
    public static void restart(Cache cache, String... args) throws IOException, InterruptedException, ExecutionException {
        service = new Service(cache);
        service.addShutdownHook();
        service.cmdLine(args);
        service.doMain();
    }

    private Service(Cache cache) {
        this.cache = cache;
    }
    
    public Service() {
        this.cache = new Cache(new HashMap<>());
    }
    
    public void cmdLine(String[] args) {
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
    }

    public void doMain() throws IOException, InterruptedException, ExecutionException {
        
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
                        Map<Path, Integer> filesCounter = Collections.synchronizedMap(Files.walk(site.
                                generated()).collect(Collectors.toMap(p -> p, p -> 0)));
                        compile(site, filesCounter, cache).get();
                        filesCounter.entrySet().stream().
                                sorted((f1, f2) -> f2.getKey().compareTo(f1.getKey())).filter(f1 -> f1.
                                getValue() <= 0 && Utils.isChild(f1.getKey(), site.generated())).forEach(
                                        f1 -> {try {Files.delete(f1.getKey());} catch (IOException ex) {LOG.error("cannot delete", ex);}});

                        LOG.info("compiling done in {} ms of {}", (System.currentTimeMillis() - start), site);
                    } catch (Throwable t) {
                        LOG.error("file changed, but could not compile", t);
                    }

                }
            });
        }
        LOG.info("compiling start: {}", site);
        final long start = System.currentTimeMillis();
        Map<Path, Integer> filesCounter = Collections.synchronizedMap(Files.walk(site.generated()).collect(
                Collectors.toMap(p -> p, p -> 0)));
        compile(site, filesCounter, cache).get();
        filesCounter.entrySet().stream().sorted((f1, f2) -> f2.getKey().compareTo(f1.getKey())).filter(
                f1 -> f1.getValue() <= 0 && Utils.isChild(f1.getKey(), site.generated())).forEach(
                        f1 -> {try {Files.delete(f1.getKey());} catch (IOException ex) {LOG.error("cannot delete", ex);}});

        LOG.info("compiling done in {} ms of {}", (System.currentTimeMillis() - start), site);
        if (isDaemon) {
            startAfterFirstRun.countDown();
        } else {
            shutdown();
        }
    }
    
    public Cache cache() {
        return cache;
    }
    
    public static Service service() {
        return service;
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

    public CompletableFuture<List<XItem>> compile(Site site, Map<Path, Integer> filesCounter, Cache cache) throws IOException, InterruptedException, ExecutionException {
        Compiler c = new Compiler(executorServiceCompiler, site, filesCounter, cache);
        return c.compile(site.source());
    }
}
