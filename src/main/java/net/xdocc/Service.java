package net.xdocc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import com.google.common.base.Strings;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerJava;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.xdocc.Cache.CacheEntry;

import java.util.stream.Collectors;

public class Service {

    private static final Logger LOG = LoggerFactory.getLogger(Service.class);

    private final ExecutorService executorServiceCompiler = Executors
            .newCachedThreadPool();

    private final List<RecursiveWatcherService> watchServices = new ArrayList<>();

    @Option(name = "-s", required = true, usage = "set the source directory to watch and recompile on the fly.")
    private String watchDirectory = null;

    @Option(name = "-g", usage = "set the directory to store generated files.")
    private String outputDirectory = null;

    @Option(name = "-c", usage = "set the directory to store the cached data")
    private String cacheDirectory = null; //set in the constructor

    @Option(name = "-r", usage = "run only once")
    private boolean runOnce = false;

    @Option(name = "-x", usage = "clear the cache at startup")
    private boolean clearCache = false;
    
    private Cache cache;
    private static Service service;
    private DB db;

    private int runCounter = 0;

    public static void main(String... args) throws IOException, InterruptedException, ExecutionException {
        service = new Service()
                .addShutdownHook()
                .cmdLine(args)
                .initCache()
                .doMain();
    }

    public Service() throws IOException {
        cacheDirectory = Files.createTempDirectory("cache").resolve("cache").toString();
        outputDirectory = Files.createTempDirectory("xdocc").toString();
    }

    private Service initCache() {
        db = DBMaker.fileDB(cacheDirectory).make();
        @SuppressWarnings("unchecked")
		ConcurrentMap<String, CacheEntry> map = db.hashMap("map", Serializer.STRING, new SerializerJava()).createOrOpen();
        if(clearCache) {
            map.clear();
        }
        this.cache = new Cache(map);
        return this;
    }

    public Service cmdLine(String[] args) {
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
            System.exit(0);
        }
        return this;
    }

    public Service doMain() throws IOException, InterruptedException, ExecutionException {
        
        final CountDownLatch startAfterFirstRun = new CountDownLatch(1);
        final Site site = new Site(Paths.get(watchDirectory), Paths.get(outputDirectory));
        final boolean isDaemon = !runOnce;

        if (isDaemon) {
            startWatch(site, () -> {
                try {
                    LOG.debug("file changed");
                    startAfterFirstRun.await();
                    LOG.info("compiling start: {}", site);
                    //load global navigation, otherwise when we change the name of a navigation
                    //item or we rename, then the old name will be visible
                    site.reloadGlobalNavigation();
                    site.reloadTemplates();
                    final long start = System.currentTimeMillis();
                    Map<String, Integer> filesCounter = Collections.synchronizedMap(Files.walk(Paths.get(site.
                            generated())).collect(Collectors.toMap(p -> p.toString(), p -> 0)));
                    compile(site, filesCounter, cache).get();

                    deleteUnusedFiles(site, filesCounter);
                    postProcessing(site);
                    runCounter ++;

                    LOG.info("compiling done in {} ms of {}", (System.currentTimeMillis() - start), site);
                } catch (Throwable t) {
                    LOG.error("file changed, but could not compile", t);
                }
            });
        }
        LOG.info("compiling start: {}", site);
        final long start = System.currentTimeMillis();
        Map<String, Integer> filesCounter = Collections.synchronizedMap(Files.walk(Paths.get(site.generated())).collect(
                Collectors.toMap(p -> p.toString(), p -> 0)));
        compile(site, filesCounter, cache).get();
        deleteUnusedFiles(site, filesCounter);
        postProcessing(site);
        runCounter ++;

        LOG.info("compiling done in {} ms of {}", (System.currentTimeMillis() - start), site);
        if (isDaemon) {
            startAfterFirstRun.countDown();
        } else {
            shutdown();
        }
        return this;
    }

    private void deleteUnusedFiles(Site site, Map<String, Integer> filesCounter) {
        filesCounter.entrySet().stream().
                sorted((f1, f2) -> f2.getKey().compareTo(f1.getKey())).filter(f1 -> f1.
                getValue() <= 0 && Utils.isChild(Paths.get(f1.getKey()), Paths.get(site.generated()))).forEach(
                f1 -> {try {Files.delete(Paths.get(f1.getKey()));} catch (IOException ex) {LOG.error("cannot delete", ex);}});

    }

    private void postProcessing(Site site) throws IOException, InterruptedException {
        XPath xPath = XPath.get(site, Paths.get(site.source()));
        String command = xPath.getPostProcessing();
        if(!Strings.isNullOrEmpty(command)) {
            String cmdOutput = Utils.executeAndOutput(new ProcessBuilder(command, xPath.path()), site.generated());
            LOG.info("cmd output: {}", cmdOutput);
        }
    }

    public static Service service() {
        return service;
    }

    public Cache cache() {
        return cache;
    }

    public int runCounter() {
        return runCounter;
    }

    private Service addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdown();
            }
        });
        return this;
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
        if(db!=null) {
            db.close();
        }
    }

    public CompletableFuture<XItem> compile(Site site, Map<String, Integer> filesCounter, Cache cache) throws IOException, InterruptedException, ExecutionException {
        Compiler c = new Compiler(executorServiceCompiler, site, filesCounter, cache);
        return c.compile(Paths.get(site.source()));
    }
}
