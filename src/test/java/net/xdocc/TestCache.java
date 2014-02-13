package net.xdocc;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestCache {
	private static final String genString = "/tmp/gen/example-cache";
	private static final String sourceString = "/example-cache";

	private static Site site;
	private static File mapCache;
	private static Service service;

	@BeforeClass
	public static void setup() throws IOException, InterruptedException,
			URISyntaxException {
		// setup source & generated folder
		URL resourceUrl = TestTags.class.getResource(sourceString);
		Path source = Paths.get(resourceUrl.toURI());
		Path generated = Paths.get(genString);
		Files.createDirectories(generated);

		// setup cache
		mapCache = new File("/tmp/testcache.mapdb");
		service = new Service();
		service.setupCache(mapCache);
		site = new Site(service, source, generated, service.findHandlers(),
				null);
		service.compile(site);
		service.waitFor(site.getSource());
	}

	@AfterClass
	public static void cleanup() throws IOException {
		service.shutdown();
		mapCache.delete();
		if (Files.exists(Paths.get("/tmp/testcache.mapdb.p"))) {
			new File("/tmp/testcache.mapdb.p").delete();
		}
		if (Files.exists(Paths.get("/tmp/testcache.mapdb.t"))) {
			new File("/tmp/testcache.mapdb.t").delete();
		}
		FileUtils.deleteDirectory(new File(genString));
	}
	
	@Test
	public void testCache() throws Exception {
		Path index = site.getGenerated().resolve("index.html");
		long timestap = Files.getLastModifiedTime(index).toMillis();
		service.compile(site);
		Thread.sleep(2000);
		long timestap2 = Files.getLastModifiedTime(index).toMillis();
		Assert.assertEquals(timestap, timestap2);
		Files.write(index, "3333".getBytes());
		service.compile(site);
		Thread.sleep(2000);
		timestap2 = Files.getLastModifiedTime(index).toMillis();
		Assert.assertNotSame(timestap, timestap2);

		Path pic = site.getGenerated().resolve(
				"test/urltest2.png");
		long timestamp = Files.getLastModifiedTime(pic).toMillis();

		service.compile(site);
		Thread.sleep(2000);
		long timestamp2 = Files.getLastModifiedTime(pic).toMillis();

		service.compile(site);
		Thread.sleep(2000);
		long timestamp3 = Files.getLastModifiedTime(pic).toMillis();

		Assert.assertEquals(timestamp, timestamp2);
		Assert.assertEquals(timestamp2, timestamp3);
		Assert.assertEquals(timestamp, timestamp3);
	}
	
}
