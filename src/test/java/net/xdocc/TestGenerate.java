package net.xdocc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGenerate {
	private static Site site;
	private static final List<Site> sites = new ArrayList<Site>();

	@BeforeClass
	public static void setup() throws Exception {
		Path source = Paths
				.get("/home/draft/workspace/XDocC2/build/var/lib/xdocc/source");

		Path generated = Paths.get("/tmp/test");
		Files.createDirectories(generated);
		site = new Site(source, generated, Service.findHandlers(), null);
		sites.add(site);

		for (Site site : sites) {
			Service.compile(site, site.getSource(), new HashMap<String, Object>());
		}
	}

	@AfterClass
	public static void shutdown() throws IOException {
		sites.clear();
		Service.shutdown();
		Utils.deleteDirectory(site.getGenerated());
	}

	@Test
	public void testGenerate() throws InterruptedException {
		System.out.println("start");
		Thread.sleep(Long.MAX_VALUE);
	}
}
