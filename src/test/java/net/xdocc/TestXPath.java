package net.xdocc;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import net.xdocc.handlers.Handler;
import net.xdocc.handlers.HandlerCopy;

import org.junit.Test;
import org.reflections.Reflections;

public class TestXPath {

	private static final String genString = "/tmp/gen";

	@Test
	public void test() throws IOException, InstantiationException,
			IllegalAccessException {
		Service service = new Service();
		Site site = new Site(service, "/tmp|tagroot=a|.nav", genString,
				null, null);
		Path p = Paths
				.get("/tmp|tagroot=a|.nav/1-url123|tag1=x|tag2=y|tag3=z|name=Myname|.txt");
		Path nav = Paths.get("/tmp|tagroot=a|.nav");
		XPath x = new XPath(site, p);
		XPath xNav = new XPath(site, nav);
		Assert.assertEquals("url123", x.getUrl());
		Assert.assertEquals("Myname", x.getName());
		Assert.assertEquals(true, xNav.isNavigation());

		 
		site = new Site(service, "/tmp|.nav", genString, null, null);
		p = Paths
				.get("/tmp|.nav/1-url123|tag1=x|tag2=y|tag3=z|name=Myname|.txt");
		nav = Paths.get("/tmp|.nav");
		x = new XPath(site, p);
		xNav = new XPath(site, nav);
		Assert.assertEquals("url123", x.getUrl());
		Assert.assertEquals("Myname", x.getName());
		Assert.assertEquals(true, xNav.isNavigation());

		site = new Site(service, "/tmp", genString, null, null);
		p = Paths.get("/tmp/1-url123|tag1=x|tag2=y|tag3=z|name=Myname|.txt");
		x = new XPath(site, p);
		Assert.assertEquals("url123", x.getUrl());
		Assert.assertEquals("Myname", x.getName());

		site = new Site(service, "/tmp", genString, null, null);
		p = Paths
				.get("/tmp/1-url1/1-url2|tag1=x|tag2=y|tag3=z|name=Myname|.txt");
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
		Assert.assertEquals("2014", x.getUrl());

		p = Paths.get("/tmp/test1234");
		x = new XPath(site, p);
		Assert.assertEquals("test1234", x.getUrl());

		p = Paths.get("/tmp/404.html");
		x = new XPath(site, p);
		Assert.assertFalse(x.isVisible());

		p = Paths.get("/tmp/1.txt");
		x = new XPath(site, p);
		Assert.assertFalse(x.isVisible());

		site = new Site(service, "/tmp", genString, findHandlers(), null);

		p = Paths.get("/tmp/1.txt");
		x = new XPath(site, p);
		Assert.assertFalse(x.isVisible());

		p = Paths.get("/tmp/1-.txt");
		x = new XPath(site, p);
		Assert.assertTrue(x.isVisible());

		p = Paths.get("/tmp/-.txt");
		x = new XPath(site, p);
		Assert.assertFalse(x.isVisible());

		p = Paths.get("/tmp/2014.test");
		x = new XPath(site, p);
		Assert.assertFalse(x.isVisible());

		p = Paths.get("/tmp/2014-.test");
		x = new XPath(site, p);
		Assert.assertTrue(x.isVisible());

		p = Paths.get("/tmp/2014-01-01.test");
		x = new XPath(site, p);
		Assert.assertFalse(x.isVisible());

		p = Paths.get("/tmp/2014-01-01-.test");
		x = new XPath(site, p);
		Assert.assertTrue(x.isVisible());

		p = Paths.get("/tmp/2014-01-01_15:15:15.test");
		x = new XPath(site, p);
		Assert.assertFalse(x.isVisible());

		p = Paths.get("/tmp/2014-01-01_15:15:15-.test");
		x = new XPath(site, p);
		Assert.assertTrue(x.isVisible());

		p = Paths.get("/tmp/2014-myurl.test");
		x = new XPath(site, p);
		Assert.assertTrue(x.isVisible());
		Assert.assertEquals("myurl", x.getUrl());

		p = Paths.get("/tmp/2014-|tag=value|.test");
		x = new XPath(site, p);
		Assert.assertTrue(x.isVisible());

		p = Paths.get("/tmp/2014-|tag=value.test");
		x = new XPath(site, p);
		Assert.assertTrue(x.isVisible());
		
		service.shutdown();
	}

	public List<Handler> findHandlers() throws InstantiationException,
			IllegalAccessException {
		Reflections reflections = new Reflections("net.xdocc");
		Set<Class<? extends Handler>> subTypes = reflections
				.getSubTypesOf(Handler.class);
		final List<Handler> handlers = new ArrayList<>();
		for (Class<? extends Handler> clazz : subTypes) {
			if (!clazz.equals(HandlerCopy.class)) {
				handlers.add(clazz.newInstance());
			}
		}
		return handlers;
	}
}
