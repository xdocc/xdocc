package net.xdocc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import net.xdocc.handlers.Handler;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

/**
 * The methods in the class runs only if started individually. To make this run,
 * the static classes have to be changed in Service.java
 * 
 * @author Thomas Bocek
 * 
 */
public class TestCompiler {

	private File mapCache;

	private Site setup(boolean inverse, List<Handler> handlers)
			throws Exception {
		Path source = Paths.get("/tmp/Test Page||" + (inverse ? "i" : "")
				+ "|.full");
		Files.createDirectories(source);
		Utils.deleteDirectory(source);
		Path generated = Paths.get("/tmp/gen.xdocc");
		Files.createDirectories(generated);
		Utils.deleteDirectory(generated);

		Utils.createFile(source, ".templates/documents.ftl", "|collection|"
				+ "|${path}|" + "<#list documents as document>"
				+ "|${document.content}|[${document.url}]" + "</#list>"
				+ "|end collection|");
		Utils.createFile(source, ".templates/document.ftl",
				"|single|[${document.url}]|[n:${navigation.name}]" + "${path}"
						+ "|${document.content}|" + "|end single|");

		Utils.createFile(source, ".templates/text.ftl", "content:|${content}|");

		mapCache = File.createTempFile("mapdb", "xdocc");
		Service.setupCache(mapCache);

		return new Site(source, generated, handlers, null);

	}

	private void cleanup(boolean inverse) throws IOException {
		Path source = Paths.get("/tmp/Test Page||" + (inverse ? "i" : "")
				+ "|.full");
		Path generated = Paths.get("/tmp/gen.xdocc");
		FileUtils.deleteDirectory(source.toFile());
		FileUtils.deleteDirectory(generated.toFile());
		mapCache.delete();
		File f1 = new File(mapCache.toString() + ".p");
		File f2 = new File(mapCache.toString() + ".p");
		f1.delete();
		f2.delete();
	}

	private void setupNav(Site site) throws IOException {
		Link navigation = Service.readNavigation(site);
		site.setNavigation(navigation);
	}

	@Test
	public void testCompilerSingle() throws Exception {
		Site site = setup(false, Service.findHandlers());
		try {
			Utils.createFile(site.getSource(), "1|index|.txt", "hello");
			List<Site> sites = new ArrayList<>();
			sites.add(site);
			Service.compile(site);
			// wait a bit...
			Thread.sleep(1000);
			// check for completion
			Path index = site.getGenerated().resolve("index.html");
			Assert.assertEquals(true, Files.exists(index));
		} finally {
			cleanup(false);
		}
	}

	@Test
	public void testCompilerCollection() throws Exception {
		Site site = setup(false, Service.findHandlers());
		try {
			Utils.createFile(site.getSource(), "1|index|.txt", "1111");
			Utils.createFile(site.getSource(), "2|index|.txt", "2222");
			Service.compile(site);
			// wait a bit...
			Thread.sleep(1000);
			// check for completion
			Path index = site.getGenerated().resolve("index.html");
			Assert.assertEquals(true, Files.exists(index));
			String content = new String(Files.readAllBytes(index));
			Assert.assertEquals(true,
					content.indexOf("2222") > content.indexOf("1111"));
		} finally {
			cleanup(false);
		}
	}

	@Test
	public void testCompilerCollectionReverse() throws Exception {
		Site site = setup(true, Service.findHandlers());
		try {
			Utils.createFile(site.getSource(), "1|index|.txt", "1111");
			Utils.createFile(site.getSource(), "2|index|.txt", "2222");
			Service.compile(site);
			// wait a bit...
			Thread.sleep(1000);
			// check for completion
			Path index = site.getGenerated().resolve("index.html");
			Assert.assertEquals(true, Files.exists(index));
			String content = new String(Files.readAllBytes(index));
			Assert.assertEquals(true,
					content.indexOf("2222") > content.indexOf("1111"));
		} finally {
			cleanup(true);
		}
	}

	@Test
	public void testCompilerNavigation() throws Exception {
		Site site = setup(true, Service.findHandlers());
		try {
			Utils.createFile(site.getSource(), "1|index|.txt", "1111");
			Utils.createFile(site.getSource(), "1|Dir1|dir1|.nav/1|index|.txt",
					"222");
			Utils.createFile(site.getSource(),
					"1|Dir1|dir1|.nav/1|dirx|dirx|.nav/1|index|.txt", "xxx");
			Utils.createFile(site.getSource(), "2|dir2|dir2|.nav/2|index|.txt",
					"333");
			setupNav(site);
			Service.compile(site);
			// wait a bit...
			Thread.sleep(1000);
			// check for completion
			Path index = site.getGenerated().resolve("index.html");
			Assert.assertEquals(true, Files.exists(index));
			String content = new String(Files.readAllBytes(index));
			Assert.assertEquals(true, content.contains("n:Test Page"));
			//
			index = site.getGenerated().resolve("dir1/index.html");
			Assert.assertEquals(true, Files.exists(index));
			content = new String(Files.readAllBytes(index));
			Assert.assertEquals(true, content.contains("n:Test Page"));
		} finally {
			cleanup(true);
		}
	}

	@Test
	public void testCache() throws Exception {
		Site site = setup(true, Service.findHandlers());
		try {
			Utils.createFile(site.getSource(), "1|index|.txt", "1111");
			Utils.createFile(site.getSource(), "2|index|.txt", "2222");
			Service.compile(site);
			// wait a bit..
			Thread.sleep(1000);
			Path index = site.getGenerated().resolve("index.html");
			long timestap = Files.getLastModifiedTime(index).toMillis();
			Service.compile(site);
			Thread.sleep(1500);
			long timestap2 = Files.getLastModifiedTime(index).toMillis();
			Assert.assertEquals(timestap, timestap2);
			Files.write(index, "3333".getBytes());
			Service.compile(site);
			Thread.sleep(1500);
			timestap2 = Files.getLastModifiedTime(index).toMillis();
			Assert.assertNotSame(timestap, timestap2);
			
		} finally {
			cleanup(true);
		}
	}
}
