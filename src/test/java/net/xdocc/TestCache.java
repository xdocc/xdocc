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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCache {
	
	private static final Logger log = LoggerFactory.getLogger(TestCache.class);
	
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
		log.info("testcompile done 1");
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
		Thread.sleep(2000);
		service.compile(site);
		service.waitFor(site.getSource());
		Thread.sleep(2000);
		log.info("testcompile done 2");
		long timestap2 = Files.getLastModifiedTime(index).toMillis();
		Assert.assertEquals(timestap, timestap2);
		Files.write(index, "3333".getBytes());
		Thread.sleep(2000);
		service.compile(site);
		service.waitFor(site.getSource());
		Thread.sleep(2000);
		log.info("testcompile done 3");
		timestap2 = Files.getLastModifiedTime(index).toMillis();
		Assert.assertNotSame(timestap, timestap2);
	}
	
}
