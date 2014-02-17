package net.xdocc.filenotify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import net.xdocc.Service;
import net.xdocc.Site;
import net.xdocc.Utils;
import net.xdocc.XPath;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestNotify {
	private static Site site;
	private static final List<Site> sites = new ArrayList<Site>();
	private static FileNotifier fileNotifier;

	@BeforeClass
	public static void setup() throws Exception {
		Path dir = Files.createTempDirectory("xdocc");
		Path source = Files.createTempDirectory(dir, "source");
		Files.createDirectories(source.resolve(".templates"));
		Path generated = Files.createTempDirectory(dir, "generated");
		Service service = new Service();
		site = new Site(service, source, generated, null, null);
		sites.add(site);
		WatchService.startWatch(sites);
		fileNotifier = WatchService.getFileNotifier();
	}

	@AfterClass
	public static void shutdown() throws IOException {
		sites.clear();
		WatchService.shutdown();
		Utils.deleteDirectory(site.getSource());
		Utils.deleteDirectory(site.getGenerated());
	}

	@Test
	public void testChange() throws IOException, InterruptedException {
		final AtomicInteger countChanged = new AtomicInteger();
		final AtomicInteger countCreated = new AtomicInteger();
		final AtomicInteger countDeleted = new AtomicInteger();
		fileNotifier.addListener(new FileListener() {
			@Override
			public void filesChanged(List<XPath> changedSet,
					List<XPath> createdSet, List<XPath> deletedSet) {
				if (!changedSet.isEmpty()) {
					countChanged.addAndGet(changedSet.size());
				}
				if (!createdSet.isEmpty()) {
					countCreated.addAndGet(createdSet.size());
				}
				if (!deletedSet.isEmpty()) {
					countDeleted.addAndGet(deletedSet.size());
				}
			}
		});
		Path source = site.getSource();
		// create two dirs
		Path dir1 = Files.createTempDirectory(source, "dir1");
		Thread.sleep(100);
		Path dir2 = Files.createTempDirectory(dir1, "dir2");
		// this is created too fast, so we don't get a notification, but we
		// watch it anyway.
		Path dir3 = Files.createTempDirectory(dir2, "dir3");
		Assert.assertEquals(0, countChanged.get());
		Assert.assertEquals(0, countCreated.get());
		Assert.assertEquals(0, countDeleted.get());
		Thread.sleep(1100);
		Assert.assertEquals(0, countChanged.get());
		Assert.assertEquals(2, countCreated.get());
		Assert.assertEquals(0, countDeleted.get());
		// create two files
		Path file1 = Files.createTempFile(dir3, "test1", "test1");
		Path file2 = Files.createTempFile(dir3, "test2", "test2");
		Assert.assertEquals(0, countChanged.get());
		Assert.assertEquals(2, countCreated.get());
		Assert.assertEquals(0, countDeleted.get());
		Thread.sleep(1100);
		Assert.assertEquals(0, countChanged.get());
		Assert.assertEquals(4, countCreated.get());
		Assert.assertEquals(0, countDeleted.get());
		// change two files
		Files.write(file1, new byte[] { 0, 0, 0 });
		Files.write(file2, new byte[] { 0, 0, 0 });
		Assert.assertEquals(0, countChanged.get());
		Assert.assertEquals(4, countCreated.get());
		Assert.assertEquals(0, countDeleted.get());
		Thread.sleep(1100);
		// we get two changes, for the first file, no idea why, but that ok
		Assert.assertEquals(true, countChanged.get() >= 2);
		Assert.assertEquals(4, countCreated.get());
		Assert.assertEquals(0, countDeleted.get());
		// delete one file and dir1
		Files.delete(file1);
		Thread.sleep(1100);
		Utils.deleteDirectory(dir1);
		Assert.assertEquals(true, countChanged.get() >= 2);
		Assert.assertEquals(4, countCreated.get());
		Assert.assertEquals(1, countDeleted.get());
		Thread.sleep(1100);
		Assert.assertEquals(true, countChanged.get() >= 2);
		Assert.assertEquals(4, countCreated.get());
		Assert.assertEquals(5, countDeleted.get());
	}

	@Test
	public void testFind() {
		Path dir = site.getSource();
		Path dir2 = dir.resolve("test");
		XPath xPath = Utils.find(dir2, sites);
		Assert.assertEquals("test", xPath.getPath().getFileName().toString());
	}


}
