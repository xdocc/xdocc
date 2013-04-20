package net.xdocc.filenotify;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.xdocc.Site;
import net.xdocc.Utils;
import net.xdocc.XPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Watcher implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(Watcher.class);
	final private WatchService watcher;
	final private Map<WatchKey, Path> keyPathMap = new HashMap<>();
	final private FileNotifier notifier;
	final private List<Site> sites;

	public Watcher(List<Site> sites, FileNotifier notifier) throws IOException {
		this.notifier = notifier;
		this.sites = sites;
		watcher = FileSystems.getDefault().newWatchService();
		for (Site site : sites) {
			Path dir = site.getSource();
			register(dir);
			walk(dir);
		}
	}

	private void walk(Path dir) throws IOException {
		Files.walkFileTree(dir, new FileVisitor<Path>() {
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
					throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult preVisitDirectory(Path dir,
					BasicFileAttributes attrs) throws IOException {
				register(dir);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc)
					throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private void register(Path dir) throws IOException {
		WatchKey watchKey = dir.register(watcher,
				StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_DELETE,
				StandardWatchEventKinds.ENTRY_MODIFY);
		keyPathMap.put(watchKey, dir);
	}

	public void shutdown() {
		try {
		watcher.close();
		} catch (IOException e) {
			LOG.error("problem in shutdown", e);
		}
	}

	@Override
	public void run() {
		Thread.currentThread().setName("Watcher");
		for (;;) {
			// wait for key to be signaled
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException | ClosedWatchServiceException e) {
				return;
			}
			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind<?> kind = event.kind();
				if (kind == StandardWatchEventKinds.OVERFLOW) {
					continue;
				}
				// The filename is the context of the event.
				@SuppressWarnings("unchecked")
				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				Path filename = ev.context();
				// create the real file
				Path base = keyPathMap.get(key);
				if (base == null) {
					continue;
				}
				Path resolved = base.resolve(filename);
				if (!Files.exists(resolved)
						&& kind != StandardWatchEventKinds.ENTRY_DELETE) {
					continue;
				}
				if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
					// test if its a new directory
					if (Files.isDirectory(resolved)) {
						try {
							register(resolved);
							walk(resolved);
							if (LOG.isDebugEnabled()) {
								LOG.debug("added newly created directory ["
										+ resolved + "]the watch list");
							}
						} catch (IOException e) {
							LOG.error("cannot watch directory " + filename
									+ " - " + e);
						}
					}
					XPath changed = Utils.find(resolved, sites);
					notifier.notifyCreated(changed);
				}
				if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
					XPath changed = Utils.find(resolved, sites);
					notifier.notifyDeleted(changed);
				}
				if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
					XPath changed = Utils.find(resolved, sites);
					notifier.notifyChanged(changed);
				}
			}
			// Reset the key -- this step is critical if you want to
			// receive further watch events. If the key is no longer valid,
			// the directory is inaccessible so exit the loop.
			boolean valid = key.reset();
			if (!valid) {
				keyPathMap.remove(key);

				// all directories are inaccessible
				if (keyPathMap.isEmpty()) {
					break;
				}
			}
		}
	}
}
