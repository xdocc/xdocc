package net.xdocc;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import junit.framework.Assert;

import org.junit.Test;

public class TestNewXPath {

	@Test
	public void test() throws IOException {
		Site site = new Site(new Service(), "/tmp|tagroot=a|.nav", "/tmp", null, null);
		Path p = Paths
				.get("/tmp|tagroot=a|.nav/1-url123|tag1=x|tag2=y|tag3=z|name=Myname|.txt");
		XPath x = new XPath(site, p);
		Assert.assertEquals("url123", x.getUrl());
		Assert.assertEquals("Myname", x.getName());
	}

}
