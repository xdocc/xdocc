package net.xdocc.filenotify;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.xdocc.XPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileNotifier implements Runnable {
	private static final Logger LOG = LoggerFactory
			.getLogger(FileNotifier.class);
	private static final int WAIT = 1000;
	private long eventTriggered = 0;
	private Set<XPath> changedSet = new HashSet<>();
	private Set<XPath> createdSet = new HashSet<>();
	private Set<XPath> deletedSet = new HashSet<>();
	private List<FileListener> listeners = new ArrayList<>();
	private volatile boolean running = true;

	@Override
	public void run() {
		Thread.currentThread().setName("FileNotifier");
		while (running) {
			synchronized (this) {
				long now = System.currentTimeMillis();
				if (!changedSet.isEmpty() || !createdSet.isEmpty()
						|| !deletedSet.isEmpty()) {
					// we got some changes, check if its time to notify
					if (eventTriggered + WAIT <= now) {
						System.err.println("trigger " + (eventTriggered + WAIT)
								+ " <= " + now);
						// trigger
						for (FileListener listener : listeners) {
							try {
								listener.filesChanged(new ArrayList<XPath>(
										changedSet), new ArrayList<XPath>(
										createdSet), new ArrayList<XPath>(
										deletedSet));
							} catch (Throwable t) {
								if (LOG.isDebugEnabled()) {
									LOG.debug("a listener failed " + t);
								}
								t.printStackTrace();
							}
							changedSet.clear();
							createdSet.clear();
							deletedSet.clear();
						}
					} else {
						try {
							wait(WAIT - (now - eventTriggered));
						} catch (InterruptedException e) {
							running = false;
						}
					}
				} else {
					// no changes, wait for it
					try {
						wait();
					} catch (InterruptedException e) {
						running = false;
					}
				}
			}
		}
	}

	public void shutdown() {
		running = false;
		synchronized (this) {
			notifyAll();
		}
	}

	public void notifyChanged(XPath changed) {
		synchronized (this) {
			System.err.println("changed " + changed);
			eventTriggered = System.currentTimeMillis();
			changedSet.add(changed);
			notifyAll();
		}
	}

	public void notifyDeleted(XPath changed) {
		synchronized (this) {
			System.err.println("deleted " + changed);
			eventTriggered = System.currentTimeMillis();
			deletedSet.add(changed);
			notifyAll();
		}
	}

	public void notifyCreated(XPath changed) {
		synchronized (this) {
			System.err.println("created " + changed);
			eventTriggered = System.currentTimeMillis();
			createdSet.add(changed);
			notifyAll();
		}
	}

	public void addListener(FileListener fileListener) {
		listeners.add(fileListener);
	}
}
