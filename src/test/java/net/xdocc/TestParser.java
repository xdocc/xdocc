package net.xdocc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import junit.framework.Assert;

import org.junit.Test;

public class TestParser {
	@Test
	public void testParserVisible() throws Exception {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/2011-10-10|test");
		XPath xPath = new XPath(site, path);
		Assert.assertEquals(true, xPath.isVisible());
	}

	@Test
	public void testParserNonVisible() throws Exception {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/2011-1-10|test");
		XPath xPath = new XPath(site, path);
		Assert.assertEquals(false, xPath.isVisible());
	}

	@Test
	public void testParserDateTime() throws Exception {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/2011-10-10_11:11:00|test");
		XPath xPath = new XPath(site, path);
		Assert.assertEquals(true, xPath.isVisible());
	}

	@Test
	public void testParserNumber() throws Exception {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/2011|test");
		XPath xPath = new XPath(site, path);
		Assert.assertEquals(true, xPath.isVisible());
	}

	@Test
	public void testParserName() throws Exception {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/2011|test");
		XPath xPath = new XPath(site, path);
		Assert.assertEquals("test", xPath.getUrl());
	}

	@Test
	public void testParserName2() throws Exception {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/2011|test|");
		XPath xPath = new XPath(site, path);
		Assert.assertEquals("test", xPath.getUrl());
	}

	@Test
	public void testParserURL() throws Exception {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/2011|test|uurl");
		XPath xPath = new XPath(site, path);
		Assert.assertEquals("test", xPath.getUrl());
		Assert.assertEquals("uurl", xPath.getName());
	}

	@Test
	public void testParserURL2() throws Exception {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/2011|test|uurl|");
		XPath xPath = new XPath(site, path);
		Assert.assertEquals("test", xPath.getUrl());
		Assert.assertEquals("uurl", xPath.getName());
	}

	@Test
	public void testParserURLCRC() throws Exception {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/2011|");
		XPath xPath = new XPath(site, path);
		Assert.assertEquals("obuxqw", xPath.getUrl());
	}

	@Test
	public void testParserGallery() throws Exception {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/2|Gallery|gallery|.nav");
		XPath xPath = new XPath(site, path);
		Assert.assertEquals("gallery", xPath.getName());
		Assert.assertEquals("Gallery", xPath.getUrl());
		Assert.assertEquals(".nav", xPath.getExtensions());
	}

	@Test
	public void testParserTags() throws Exception {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/2011|url|test|test=hallo,me=2|.done");
		XPath xPath = new XPath(site, path);
		Assert.assertEquals("hallo", xPath.getProperties().get("test"));
		Assert.assertEquals("2", xPath.getProperties().get("me"));
		Assert.assertEquals(".done", xPath.getExtensions());
	}

	@Test
	public void testParserTags2() throws Exception {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths
				.get("/tmp/2011|url|test|test=hallo,key,no2=yes|.done");
		XPath xPath = new XPath(site, path);
		Assert.assertEquals("hallo", xPath.getProperties().get("test"));
		Assert.assertEquals(true, xPath.getProperties().containsKey("key"));
		Assert.assertEquals(null, xPath.getProperties().get("no"));
		Assert.assertEquals(".done", xPath.getExtensions());
	}

	@Test
	public void testParserTags3() throws Exception {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths
				.get("/tmp/2011|url|test|test=hallo,key,no2=yes|.done");
		XPath xPath = new XPath(site, path);
		Assert.assertEquals("hallo", xPath.getProperties().get("test"));
		Assert.assertEquals(true, xPath.getProperties().containsKey("key"));
		Assert.assertEquals(true, xPath.getProperties().containsKey("no2"));
	}

	@Test
	public void testParserText() throws Exception {
		// we need to find the txt handler for this test
		Site site = new Site(new Service(),"/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/2011|test.txt");
		XPath xPath = new XPath(site, path);
		Assert.assertEquals(null, xPath.getName());
		Assert.assertEquals("test", xPath.getUrl());
		Assert.assertEquals(".txt", xPath.getExtensions());
	}

	@Test
	public void testRegexpLevel() throws IOException {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/1|name|url|l1=m/2|test.me");
		try {
			Files.createDirectories(path.getParent());
			Files.createFile(path);
			XPath xPath = new XPath(site, path);
			String prefix = xPath.getLayoutSuffix();
			Assert.assertEquals("_m", prefix);
		} finally {
			Files.delete(path);
		}
	}

	@Test
	public void testRegexpLevel2() throws IOException {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/1|url|name|l=m/2|test.me");
		try {
			Files.createDirectories(path.getParent());
			Files.createFile(path);
			XPath xPath = new XPath(site, path);
			String prefix = xPath.getLayoutSuffix();
			Assert.assertEquals("_m", prefix);
		} finally {
			Files.delete(path);
		}
	}

	@Test
	public void testRegexpLevel3() throws IOException {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/1|url|name|l0=m/2|test.me");
		try {
			Files.createDirectories(path.getParent());
			Files.createFile(path);
			XPath xPath = new XPath(site, path);
			String prefix = xPath.getLayoutSuffix();
			Assert.assertEquals("_m", prefix);
		} finally {
			Files.delete(path);
		}
	}

	@Test
	public void testRegexpLevel4() throws IOException {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/1|name|url|l9=m/2|name2|url2|l/3|test.me");
		try {
			Files.createDirectories(path.getParent());
			Files.createFile(path);
			XPath xPath = new XPath(site, path);
			String prefix = xPath.getLayoutSuffix();
			Assert.assertEquals("", prefix);
		} finally {
			Files.delete(path);
		}
	}

	@Test
	public void testRegexpLevel5() throws IOException {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/1|name|url|l9=m/2|name2|url2|/3|test.me");
		try {
			Files.createDirectories(path.getParent());
			Files.createFile(path);
			XPath xPath = new XPath(site, path);
			String prefix = xPath.getLayoutSuffix();
			Assert.assertEquals("_m", prefix);
		} finally {
			Files.delete(path);
		}
	}

	@Test
	public void testRegexpLevel6() throws IOException {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths.get("/tmp/1|url|name|l=m/2|name2|url2|/3|test.me");
		try {
			Files.createDirectories(path.getParent());
			Files.createFile(path);
			XPath xPath = new XPath(site, path);
			String prefix = xPath.getLayoutSuffix();
			Assert.assertEquals("_m", prefix);
		} finally {
			Files.delete(path);
		}
	}

	@Test
	public void testName() throws IOException {
		Site site = new Site(new Service(), "/tmp", "/tmp", null, null);
		Path path = Paths
				.get("/tmp/2012-04-22|tomp2p_4.1.2|TomP2P 4.1.2-preview|.textile");
		XPath xPath = new XPath(site, path);
		Assert.assertEquals("tomp2p_4.1.2", xPath.getUrl());
	}
}
