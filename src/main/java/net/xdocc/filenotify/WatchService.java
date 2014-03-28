package net.xdocc.filenotify;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.xdocc.Site;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatchService {
	private final Logger LOG = LoggerFactory
			.getLogger(WatchService.class);

	private final ExecutorService executorServiceWatcher = Executors
			.newSingleThreadExecutor();

	private final ExecutorService executorServiceNotifier = Executors
			.newSingleThreadExecutor();

	private final FileNotifier notifier = new FileNotifier();

	private Watcher watcher;

	public void startWatch(List<Site> sites) {
		try {
			executorServiceNotifier.execute(notifier);
			watcher = new Watcher(sites, notifier);
			executorServiceWatcher.execute(watcher);
		} catch (IOException e) {
			LOG.error("cannot watch directories" + e);
			e.printStackTrace();
		}
	}

	public FileNotifier getFileNotifier() {
		return notifier;
	}

	public void shutdown() {
		if (watcher != null) {
			watcher.shutdown();
		}
		notifier.shutdown();
		executorServiceWatcher.shutdown();
		executorServiceNotifier.shutdown();
	}
}
