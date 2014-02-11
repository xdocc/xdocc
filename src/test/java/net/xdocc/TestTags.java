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
public class TestTags {

	private static final Logger log = LoggerFactory.getLogger(TestTags.class);

	private static final String genString = "/tmp/gen/example";
	private static final String sourceString = "/example|si=50x50|sn=500x500|all";
	
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
		// h011
		Path h011 = site.getGenerated().resolve("folder0/folder01/folder011/index.html");
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

	@Test
	public void testImageTags() throws IOException {
		// testpng assert width or height of resized images
		Path sn = site.getGenerated().resolve("folder0/folder01/folder010/testpng_n.png");
		Path si = site.getGenerated().resolve("folder0/folder01/folder010/testpng_t.png");
		BufferedImage img_sn = ImageIO.read(sn.toFile());
		BufferedImage img_si = ImageIO.read(si.toFile());
		int sn_height = img_sn.getHeight();
		int si_height = img_si.getHeight();
		int sn_width = img_sn.getWidth();
		int si_width = img_si.getWidth();
		Assert.assertTrue(sn_height == 500 || sn_width==500);		
		Assert.assertTrue(si_height==50 || si_width==50);
	}
	
	@Test
	public void testPaging() throws IOException {
		CompileResult cr = service.getCompileResult(site.getSource().resolve("1-folder0|l=x|n=Folder 0|.nav/1-folder00|l2=z|n=Folder 00|/1-folder000|p=3|n=Folder 000|"));
		List<Document> docs = cr.getDocument().getDocuments();
		Assert.assertEquals(3, docs.size());
		Path h000 = site.getGenerated().resolve("folder0/folder00/folder000/index_1.html");
		String file000 = new String(Files.readAllBytes(h000));
		Assert.assertTrue(Files.exists(h000));
	}
	
	@Test
	public void testAllVisible() {
		
	}
	
}
