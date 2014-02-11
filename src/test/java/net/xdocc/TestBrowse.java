package net.xdocc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.imageio.ImageIO;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test case aims to validate the tags which can be specified on directory
 * and file level. So far these tags are under test:
 * 
 * - layout (l)
 * - size_icon (si)
 * - size_normal (sn)
 * - paging (p)
 * - all (a)
 * 
 */
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
	public static void cleanup() {
		service.shutdown();
		mapCache.delete();
		new File("/tmp/testcache.mapdb.p");
	}

	@Test
	public void testBrowse() {
		
	}
	
}
