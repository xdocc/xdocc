package net.xdocc;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.Test;

public class TestNewXPath {
	
	private static final String genString = "/tmp/gen";

	@Test
	public void test() throws IOException {
		Site site = new Site(new Service(), "/tmp|tagroot=a|.nav", genString, null, null);
		Path p = Paths
				.get("/tmp|tagroot=a|.nav/1-url123|tag1=x|tag2=y|tag3=z|name=Myname|.txt");
		Path nav = Paths.get("/tmp|tagroot=a|.nav");
		XPath x = new XPath(site, p);
		XPath xNav = new XPath(site, nav);
		Assert.assertEquals("url123", x.getUrl());
		Assert.assertEquals("Myname", x.getName());
		Assert.assertEquals(true, xNav.isNavigation());
		
		site = new Site(new Service(), "/tmp|.nav", genString, null, null);
		p = Paths.get("/tmp|.nav/1-url123|tag1=x|tag2=y|tag3=z|name=Myname|.txt");
		nav = Paths.get("/tmp|.nav");
		x = new XPath(site, p);
		xNav = new XPath(site, nav);
		Assert.assertEquals("url123", x.getUrl());
		Assert.assertEquals("Myname", x.getName());
		Assert.assertEquals(true, xNav.isNavigation());
		
		site = new Site(new Service(), "/tmp", genString, null, null);
		p = Paths.get("/tmp/1-url123|tag1=x|tag2=y|tag3=z|name=Myname|.txt");
		x = new XPath(site, p);
		Assert.assertEquals("url123", x.getUrl());
		Assert.assertEquals("Myname", x.getName());
		
		site = new Site(new Service(), "/tmp", genString, null, null);
		p = Paths.get("/tmp/1-url1/1-url2|tag1=x|tag2=y|tag3=z|name=Myname|.txt");
		x = new XPath(site, p);
		Assert.assertEquals("url2", x.getUrl());
		Assert.assertEquals("Myname", x.getName());
		
		p = Paths.get("/tmp/1-url1");
		x = new XPath(site, p);
		Assert.assertEquals("url1", x.getUrl());
		
		p = Paths.get("/tmp/2014/2-test|.txt");
		x = new XPath(site, p);
		Assert.assertEquals("test", x.getUrl());
		
		p = Paths.get("/tmp/2014");
		x = new XPath(site, p);
		Assert.assertEquals("1faux7l", x.getUrl());
	}
	
}
