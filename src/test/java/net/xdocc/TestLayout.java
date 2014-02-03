package net.xdocc;

import java.io.File;
import java.io.IOException;
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
 * hierarchy:
 * 
 * 1|folder0|Folder 0|l=x 			<-- layout x set
 * 
 * --1|folder00|Folder 00 			<-- layout x inherited
 * ----1|folder000|Folder 000 		<-- layout default
 * ----2|folder001|Folder 001		<-- layout default
 * 
 * --2|folder01|Folder 01 			<-- layout x inherited
 * ----1|folder010|Folder 010		<-- layout default
 * ----2|folder011|Folder 011|l=z 	<-- layout z set
 * 
 * .templates:
 * --collection.ftl
 * --collection_x.ftl
 * --page.ftl
 * --page_x.ftl
 * --wikitext.ftl
 * --wikitext_z.ftl
 * 
 */
public class TestLayout {
	
	private static final Logger log = LoggerFactory.getLogger(TestLayout.class);

	private static final String sourceString = "/tmp/example|example site";
	private static final String genString = "/tmp/gen";
	private static final String hierarchy0 = "/1|folder0|Folder 0|l=x|.nav";
	private static final String hierarchy00 = hierarchy0+"/1|folder00|Folder 00";
	private static final String hierarchy01 = hierarchy0+"/2|folder01|Folder 01";
	private static final String hierarchy000 = hierarchy00+"/1|folder000|Folder 000";
	private static final String hierarchy001 = hierarchy00+"/2|folder001|Folder 001";
	private static final String hierarchy010 = hierarchy01+"/1|folder010|Folder 010";
	private static final String hierarchy011 = hierarchy01+"/2|folder011|Folder 011|l=z";
	
	private static Site site;
	private static File mapCache;

	private static Service service;
	
	@BeforeClass
	public static void setup() throws IOException, InterruptedException {
		// setup source & generated folder
		Path source = Paths.get(sourceString);
		Path generated = Paths.get(genString);
		Files.createDirectories(source);
		Files.createDirectories(generated);
		
		// setup hierarchy
		Path h000 = Paths.get(sourceString+hierarchy000);
		Path h001 = Paths.get(sourceString+hierarchy001);
		Path h010 = Paths.get(sourceString+hierarchy010);
		Path h011 = Paths.get(sourceString+hierarchy011);

		Files.createDirectories(h000);
		Files.createDirectories(h001);
		Files.createDirectories(h010);
		Files.createDirectories(h011);

		//root
		Utils.createFile(source, "2|test0|.textile", "*test0*");
		// h0
		Path h0 = Paths.get(sourceString+hierarchy0);
		Utils.createFile(h0, "3|test1|.textile", "*test1*");
		// h00
		Path h00 = Paths.get(sourceString+hierarchy00);
		Utils.createFile(h00, "3|test2|.textile", "*test3*");
		// h01
		Path h01 = Paths.get(sourceString+hierarchy01);
		Utils.createFile(h01, "3|test3|.textile",  "*test4*");
		// h000
		Utils.createFile(h000, "1|test4|.textile",  "*test5*");
		// h001
		Utils.createFile(h001, "1|test5|.textile",  "*test6*");
		// h010
		Utils.createFile(h010, "1|test6|.textile",  "*test7*");
		// h001
		Utils.createFile(h011, "1|test7|.textile",  "*test8*");

		// setup cache
		mapCache = File.createTempFile("mapdb", "xdocc");

		// setup templates
		Utils.createFile(source, ".templates/collection.ftl", ""
				+ "<#list documents as document>"
				+ " ${document.generate}"
				+ "</#list>"
				);
		Utils.createFile(source, ".templates/collection_x.ftl", ""
				+ "<#list documents as document>"
				+ "<div class=\"layout_x\">"
				+ " ${document.generate}"
				+ "</div>"
				+ "</#list>"
				);		
		Utils.createFile(source, ".templates/page.ftl", ""
				+ "${document.generate}"
				);
		Utils.createFile(source, ".templates/page_x.ftl", ""
				+ "<div class=\"layout_x\">"
				+ "${document.generate}"
				+ "</div>"
				);
		Utils.createFile(source, ".templates/wikitext.ftl", ""
				+ "${content}"
				);
		Utils.createFile(source, ".templates/wikitext_z.ftl", ""
				+ "<div class=\"layout_z\">"
				+ "${content}"
				+ "</div>"
				);
			
		service = new Service();
		service.setupCache(mapCache);
		site =  new Site(service, source, generated, service.findHandlers(), null);
		service.compile(site);
		// wait a bit...
		Thread.sleep(5000);
		// check for completion
	}
	
	@AfterClass
	public static void cleanup() throws IOException {
		Path source = Paths.get(sourceString);
		Path generated = Paths.get(genString);
		FileUtils.deleteDirectory(source.toFile());
//		FileUtils.deleteDirectory(generated.toFile());
		site.service().shutdown();
		mapCache.delete();	
	}

	
	@Test
	public void testLayoutX() throws IOException, InterruptedException {
		Path index = site.getGenerated().resolve("index.html");
		//root (default layout)
		String root = new String(Files.readAllBytes(index));
		Assert.assertTrue(Files.exists(index));
		Assert.assertFalse(root.contains("<div class=\"layout_x\">"));
		// h0
		Path h0 = site.getGenerated().resolve("folder0/index.html");
		String file0 = new String(Files.readAllBytes(h0));
		Assert.assertTrue(Files.exists(h0));
		Assert.assertTrue(file0.contains("<div class=\"layout_x\">"));
		// h00
		Path h00 = site.getGenerated().resolve("folder0/folder00/index.html");
		String file00 = new String(Files.readAllBytes(h00));
		Assert.assertTrue(Files.exists(h00));
		Assert.assertTrue(file00.contains("<div class=\"layout_x\">"));
		// h000
		Path h000 = site.getGenerated().resolve("folder0/folder00/folder000/index.html");
		String file000 = new String(Files.readAllBytes(h000));
		Assert.assertTrue(Files.exists(h000));
		Assert.assertFalse(file000.contains("<div class=\"layout_x\">"));
	}
	
	@Test
	public void testLayoutZ() throws IOException, InterruptedException {
		// h011
		Path h011 = site.getGenerated().resolve("folder0/folder01/folder011/index.html");
		String file011 = new String(Files.readAllBytes(h011));
		Assert.assertTrue(Files.exists(h011));
		Assert.assertFalse(file011.contains("<div class=\"layout_x\">"));
		Assert.assertTrue(file011.contains("<div class=\"layout_z\">"));

	}

}
