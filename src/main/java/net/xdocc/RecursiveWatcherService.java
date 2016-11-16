/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.xdocc;

import com.sun.nio.file.SensitivityWatchEventModifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RecursiveWatcherService {

    private static final Logger LOG = LoggerFactory.getLogger(RecursiveWatcherService.class);

    private final WatchService watcher;
    private final ExecutorService executor;
    private final Site site;
    private final Listener listener;
    private final BlockingQueue<Boolean> queue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    public RecursiveWatcherService(Site site, Listener listener) throws IOException {
        this.site = site;
        this.listener = listener;
        watcher = FileSystems.getDefault().newWatchService();
        executor = Executors.newFixedThreadPool(2);
        startRecursiveWatcher();
    }

    public void shutdown() {
        LOG.info("Stoping Recursive Watcher");
        running = false;
        queue.add(Boolean.FALSE);
        try {
            watcher.close();
        } catch (IOException e) {
            LOG.error("Error closing watcher service", e);
        }
        executor.shutdownNow();
    }

    private void startRecursiveWatcher() throws IOException {
        LOG.info("Starting Recursive Watcher");

        final Map<WatchKey, Path> keys = new HashMap<>();

        Consumer<Path> register = p -> {
            if (!p.toFile().exists() || !p.toFile().isDirectory()) {
                throw new RuntimeException("directory " + p + " does not exist or is not a directory");
            }
            try {
                Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        LOG.info("registering {} in watcher service", dir);
                        WatchKey watchKey = dir.register(watcher,
                                new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY},
                                SensitivityWatchEventModifier.HIGH);
                        keys.put(watchKey, dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException("Error registering path " + p, e);
            }
        };

        register.accept(site.source());
                
        executor.submit(() -> {
            while (running) {
                final WatchKey key;
                try {
                    key = watcher.take(); // wait for a key to be available
                } catch (InterruptedException ex) {
                    return;
                }

                final Path dir = keys.get(key);
                if (dir == null) {
                    LOG.error("WatchKey {} not recognized!", key);
                    continue;
                }

                key.pollEvents().stream()
                        .filter(e -> (e.kind() != OVERFLOW))
                        .map(e -> ((WatchEvent<Path>) e).context())
                        .forEach(p -> {
                            final Path absPath = dir.resolve(p);
                            if (absPath.toFile().isDirectory()) {
                                register.accept(absPath);
                            }
                            LOG.debug("File/directory changed {}", absPath);
                            queue.add(Boolean.TRUE);
                        });

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        });
        
        executor.submit(() -> {
            while (running) {
                try {
                    queue.take();
                    Thread.sleep(1000);
                    Boolean any;
                    if((any = queue.poll()) != null) {
                        queue.clear();
                        queue.offer(any);
                        continue;
                    }
                } catch (InterruptedException ex) {
                    return;
                }
                listener.filesChanged(site);
            }
        });
    }
    
    public interface Listener {
        void filesChanged(Site site);
    }
}
