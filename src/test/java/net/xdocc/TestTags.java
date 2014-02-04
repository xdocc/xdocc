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

/**
 * This test case aims to validate the tags which can be specified on directory
 * and file level. So far these tags are under test:
 * 
 * - layout (l)
 * 
 * 
 * hierarchy:
 * 
 * 1|folder0|Folder 0|l=x <-- layout x set
 * 
 * --1|folder00|Folder 00|l2=z <-- layout z set ----1|folder000|Folder 000 <--
 * layout z inherited ----2|folder001|Folder 001 <-- layout z inherited
 * ------1|folder0010|Folder 0010 <-- layout z inherited
 * 
 * --2|folder01|Folder 01 <-- layout x inherited ----1|folder010|Folder 010 <--
 * layout default ----2|folder011|Folder 011|l=z <-- layout z set
 * 
 * .templates: --collection.ftl --collection_x.ftl --page.ftl --page_x.ftl
 * --wikitext.ftl --wikitext_z.ftl
 * 
 */
public class TestTags {

	private static final Logger log = LoggerFactory.getLogger(TestTags.class);

	private static final String genString = "/tmp/gen";
	private static final String sourceString = "/example|example site";
	
//	private static final String hierarchy0 = "/1|folder0|Folder 0|l=x|.nav";
//	private static final String hierarchy00 = hierarchy0
//			+ "/1|folder00|Folder 00|l2=z";
//	private static final String hierarchy01 = hierarchy0
//			+ "/2|folder01|Folder 01";
//	private static final String hierarchy000 = hierarchy00
//			+ "/1|folder000|Folder 000";
//	private static final String hierarchy001 = hierarchy00
//			+ "/2|folder001|Folder 001";
//	private static final String hierarchy010 = hierarchy01
//			+ "/1|folder010|Folder 010";
//	private static final String hierarchy011 = hierarchy01
//			+ "/2|folder011|Folder 011|l=z";
//	private static final String hierarchy0010 = hierarchy001
//			+ "/1|folder0010|Folder 0010";

	private static Site site;
	private static File mapCache;

	private static Service service;

	@BeforeClass
	public static void setup() throws IOException, InterruptedException, URISyntaxException {
		// setup source & generated folder
		URL resourceUrl = TestTags.class.getResource(sourceString);
		Path source = Paths.get(resourceUrl.toURI());
		Path generated = Paths.get(genString);
		Files.createDirectories(generated);

		// setup cache
		mapCache = File.createTempFile("mapdb", "xdocc");

		service = new Service();
		service.setupCache(mapCache);
		site = new Site(service, source, generated, service.findHandlers(),
				null);
		service.compile(site);
		// wait a bit...
		Thread.sleep(5000);
		// check for completion
	}

	@AfterClass
	public static void cleanup() throws IOException {
		Path generated = Paths.get(genString);
		FileUtils.deleteDirectory(generated.toFile());
		site.service().shutdown();
		mapCache.delete();
	}

	@Test
	public void testLayoutX() throws IOException, InterruptedException {
		Path index = site.getGenerated().resolve("index.html");
		// root (default layout)
		String root = new String(Files.readAllBytes(index));
		Assert.assertTrue(Files.exists(index));
		Assert.assertFalse(root.contains("<div class=\"layout_x\">"));
		// h0
		Path h0 = site.getGenerated().resolve("folder0/index.html");
		String file0 = new String(Files.readAllBytes(h0));
		Assert.assertTrue(Files.exists(h0));
		Assert.assertTrue(file0.contains("<div class=\"layout_x\">"));
		// h01
		Path h01 = site.getGenerated().resolve("folder0/folder01/index.html");
		String file01 = new String(Files.readAllBytes(h01));
		Assert.assertTrue(Files.exists(h01));
		Assert.assertTrue(file01.contains("<div class=\"layout_x\">"));
		// h0010
		Path h0010 = site.getGenerated().resolve(
				"folder0/folder00/folder001/folder0010/index.html");
		String file0010 = new String(Files.readAllBytes(h0010));
		Assert.assertTrue(Files.exists(h0010));
		Assert.assertTrue(file0010.contains("<div class=\"layout_x\">"));
	}

	@Test
	public void testLayoutZ() throws IOException, InterruptedException {
		// h00
		Path h00 = site.getGenerated().resolve("folder0/folder00/index.html");
		String file00 = new String(Files.readAllBytes(h00));
		Assert.assertTrue(Files.exists(h00));
		Assert.assertTrue(file00.contains("<div class=\"layout_z\">"));
		Path h011 = site.getGenerated().resolve(
				"folder0/folder01/folder011/index.html");
		String file011 = new String(Files.readAllBytes(h011));
		Assert.assertTrue(Files.exists(h011));
		Assert.assertTrue(file011.contains("<div class=\"layout_z\">"));
		// h000
		Path h000 = site.getGenerated().resolve(
				"folder0/folder00/folder000/index.html");
		String file000 = new String(Files.readAllBytes(h000));
		Assert.assertTrue(Files.exists(h000));
		Assert.assertTrue(file000.contains("<div class=\"layout_z\">"));
		// h0010
		Path h0010 = site.getGenerated().resolve(
				"folder0/folder00/folder001/folder0010/index.html");
		String file0010 = new String(Files.readAllBytes(h0010));
		Assert.assertTrue(Files.exists(h0010));
		Assert.assertTrue(file0010.contains("<div class=\"layout_z\">"));
	}

	// @Test
	public void debugloop() {
		while (true) {
		}
	}
}
