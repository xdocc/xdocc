package net.xdocc.filenotify;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.xdocc.Site;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatchService {
	private static final Logger LOG = LoggerFactory
			.getLogger(WatchService.class);

	private static final ExecutorService executorServiceWatcher = Executors
			.newSingleThreadExecutor();

	private static final ExecutorService executorServiceNotifier = Executors
			.newSingleThreadExecutor();

	private static final FileNotifier notifier = new FileNotifier();

	private static Watcher watcher;

	public static void startWatch(List<Site> sites) {
		try {
			// we create a thread because we don't want to notify every time a
			// file
			// changes - e.g. copy multiple files
			executorServiceNotifier.execute(notifier);
			watcher = new Watcher(sites, notifier);
			executorServiceWatcher.execute(watcher);
		} catch (IOException e) {
			LOG.error("cannot watch directories" + e);
			e.printStackTrace();
		}
	}

	public static FileNotifier getFileNotifier() {
		return notifier;
	}

	public static void shutdown() {
		if (watcher != null) {
			watcher.shutdown();
		}
		notifier.shutdown();
		executorServiceWatcher.shutdown();
		executorServiceNotifier.shutdown();
	}
}
