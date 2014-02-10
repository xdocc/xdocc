package net.xdocc;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The methods in the class runs only if started individually. To make this run,
 * the static classes have to be changed in Service.java
 * 
 * @author Thomas Bocek
 * 
 */
public class TestCompiler {

	private static final String genString = "/tmp/gen";
	private static final String sourceString = "/example|si=50x50|sn=500x500|all";

	private static Site site;
	private static File mapCache;
	private static Service service;

	@Before
	public void setup() throws IOException, InterruptedException,
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

	private void cleanup() throws IOException, InterruptedException {
		service.shutdown();
		mapCache.delete();
		Thread.sleep(1000);
		Path p = Paths.get("/tmp/testcache.mapdb.p");
		if (Files.exists(p)) {
			Files.delete(p);
		}
		p = Paths.get("/tmp/testcache.mapdb.t");
		if (Files.exists(p)) {
			Files.delete(p);
		}
	}

	@Test
	public void testCompilerSingle() throws Exception {
		try {
			Path index = site.getGenerated().resolve(
					"folder0/folder01/folder011/index.html");
			Assert.assertEquals(true, Files.exists(index));
		} finally {
			cleanup();
		}
	}

	@Test
	public void testCompilerCollection() throws Exception {
		try {
			Path index = site.getGenerated().resolve("index.html");
			Assert.assertEquals(true, Files.exists(index));
		} finally {
			cleanup();
		}
	}

	@Test
	public void testCompilerNavigation() throws Exception {
		try {
			Path index = site.getGenerated().resolve("index.html");
			Assert.assertEquals(true, Files.exists(index));
			index = site.getGenerated().resolve("folder0/index.html");
			Assert.assertEquals(true, Files.exists(index));
		} finally {
			cleanup();
		}
	}

	@Test
	public void testCache() throws Exception {
		try {
			Path index = site.getGenerated().resolve("index.html");
			long timestap = Files.getLastModifiedTime(index).toMillis();
			service.compile(site);
			Thread.sleep(1000);
			long timestap2 = Files.getLastModifiedTime(index).toMillis();
			Assert.assertEquals(timestap, timestap2);
			Files.write(index, "3333".getBytes());
			service.compile(site);
			Thread.sleep(1000);
			timestap2 = Files.getLastModifiedTime(index).toMillis();
			Assert.assertNotSame(timestap, timestap2);
		} finally {
			cleanup();
		}
	}
}
