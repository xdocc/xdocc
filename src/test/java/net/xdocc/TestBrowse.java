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


public class TestBrowse {

	private static final Logger log = LoggerFactory.getLogger(TestBrowse.class);

	private static final String genString = "/tmp/gen/example-browse";
	private static final String sourceString = "/example-browse";
	
	private static Site site;
	private static File mapCache;

	private static Service service;

	@BeforeClass
	public static void setup() throws IOException, InterruptedException, URISyntaxException {
		// setup source & generated folder
		URL resourceUrl = TestBrowse.class.getResource(sourceString);
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
		if(Files.exists(Paths.get("/tmp/testcache.mapdb.p"))) {
			new File("/tmp/testcache.mapdb.p").delete();
		}
		if(Files.exists(Paths.get("/tmp/testcache.mapdb.t"))) {
			new File("/tmp/testcache.mapdb.t").delete();
		}
		FileUtils.deleteDirectory(new File(genString));
	}

	@Test
	public void testBrowse() {
		Path p = site.getGenerated().resolve("index.html");
		Assert.assertTrue(Files.exists(p));
		p = site.getGenerated().resolve("test/index.html");
		Assert.assertTrue(Files.exists(p));
		p = site.getGenerated().resolve("test/2010-04-23-XDocC preview/index.html");
		Assert.assertTrue(Files.exists(p));
		p = site.getGenerated().resolve("test/2010-05-02-XDocC preview/index.html");
		Assert.assertTrue(Files.exists(p));
	}
	
}
