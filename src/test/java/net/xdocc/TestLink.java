package net.xdocc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import junit.framework.Assert;
import net.xdocc.CompileResult.Key;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TestLink {

	private static final Logger log = LoggerFactory.getLogger(TestLink.class);

	private static final String genString = "/tmp/gen/example-link";
	private static final String sourceString = "/example-link";
	
	private static Site site;
	private static File mapCache;

	private static Service service;

	@BeforeClass
	public static void setup() throws IOException, InterruptedException, URISyntaxException {
		// setup source & generated folder
		URL resourceUrl = TestLink.class.getResource(sourceString);
		Path source = Paths.get(resourceUrl.toURI());
		Path generated = Paths.get(genString);
		Files.createDirectories(generated);

		// setup cache
		mapCache = new File("/tmp/testcache.mapdb");
		service = new Service();
		Service.setFileListener(true);
		service.setupCache(mapCache);
		site = new Site(service, source, generated, service.findHandlers(),
				null);
		service.compile(site);
		Key<Path> crk = new Key<Path>(site.getSource(), site.getSource());
		service.waitFor(crk);
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
	public void testLink() throws InterruptedException {
		Path p = site.getGenerated().resolve("index.html");
		Assert.assertTrue(Files.exists(p));
		Path link = site.getSource().resolve("1-mylink.link");
		Path linkTarget = site.getSource().resolve("1-mylink.link");
		Key<Path> crk = new Key<Path>(link, linkTarget);
		service.waitFor(crk);
		CompileResult result = service.getCompileResult(crk);
		Assert.assertTrue(result.getDocument().getDocuments().size() == 3);
	}
	
}
