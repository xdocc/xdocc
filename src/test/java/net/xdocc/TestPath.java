package net.xdocc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

public class TestPath {
	@Test
	public void testPath1() {
		Path source = Paths.get("/tmp/");
		Path path = Paths.get("/tmp/test");
		String result = Utils.relativePathToRoot(source, path);
		Assert.assertEquals("../", result);
	}

	@Test
	public void testPath2() {
		Path source = Paths.get("/tmp/");
		Path path = Paths.get("/tmp2/test");
		String result = Utils.relativePathToRoot(source, path);
		Assert.assertEquals(null, result);
	}

	@Test
	public void testPath3() {
		Path source = Paths.get("/tmp/");
		Path path = Paths.get("/tmp/");
		String result = Utils.relativePathToRoot(source, path);
		Assert.assertEquals("", result);
	}

	@Test
	public void testPath4() {
		Path source = Paths.get("/tmp/");
		Path path = Paths.get("/tmp");
		String result = Utils.relativePathToRoot(source, path);
		Assert.assertEquals("", result);
	}

	@Test
	public void testPath5() throws IOException {
		Path source = Paths.get("/tmp/");
		Path path = Paths.get("/tmp/test/hallo/");
		Files.createDirectories(path);
		String result = Utils.relativePathToRoot(source, path, false);
		Assert.assertEquals("../../", result);
	}

	@Test
	public void testLinkToRoot() throws IOException {
		Path source = Paths.get("/tmp");
		Path generated = Files.createTempDirectory("tmp");
		Site site = new Site(new Service(), source, generated, null, null);
		Path path1 = Paths.get("/tmp/");
		XPath xPath1 = new XPath(site, path1);
		Path path2 = Paths.get("/tmp/1");
		XPath xPath2 = new XPath(site, path2);
		Path path3 = Paths.get("/tmp/1/2");
		XPath xPath3 = new XPath(site, path3);
		Link rootLink = new Link(xPath1, null);
		Link link1 = new Link(xPath2, rootLink);
		Link link2 = new Link(xPath3, link1);
		List<Link> retVal = Utils.linkToRoot(site.getSource(), xPath3);
		// Assert.assertEquals( rootLink, retVal.get( 0 ) );
		Assert.assertEquals(link1, retVal.get(0));
		Assert.assertEquals(link2, retVal.get(1));
	}
}
