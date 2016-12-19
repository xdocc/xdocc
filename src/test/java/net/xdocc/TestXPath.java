package net.xdocc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.junit.Test;

public class TestXPath {

    private static Path gen;
    private static Path src;

    @BeforeClass
    public static void setup() throws IOException {
        src = Files.createTempDirectory("src");
        gen = Files.createTempDirectory("gen");
        Files.createDirectories(src.resolve(".templates"));
    }

    @AfterClass
    public static void tearDown() throws IOException {
        Utils.deleteDirectories(gen, src);
    }

    @Test
    public void testRoot() throws IOException, InstantiationException, IllegalAccessException {
        Service service = new Service();
        Site site = new Site(service, src, gen);
        XPath root = new XPath(site, src);
        Assert.assertEquals(false, root.isNavigation());
        service.shutdown();
    }

    @Test
    public void testFile() throws IOException, InstantiationException,
            IllegalAccessException {
        Service service = new Service();
        Site site = new Site(service, src, gen);
        testFileAssert(site, src.toString()+"/1-url123|tag1=x|tag2=y|tag3=z|name=Myname|.txt");
        testFileAssert(site, src.toString()+"/1-url123|tag1=x|tag2=y|tag3=z|name=Myname.txt");
        service.shutdown();
    }

    private void testFileAssert(Site site, String name) throws IOException, InstantiationException,
            IllegalAccessException {
        Path p = Paths.get(name);
        XPath x = new XPath(site, p);
        Assert.assertEquals("url123", x.url());
        Assert.assertEquals("Myname", x.name());
        Assert.assertEquals(false, x.isNavigation());
        Assert.assertEquals(1, x.nr());
        Assert.assertEquals(3, x.properties().size());
        Assert.assertEquals("txt", x.extensionList().get(0));
        Assert.assertEquals(1, x.extensionList().size());
        Assert.assertEquals(true, x.isVisible());
    }

    @Test
    public void testFileRegular() throws IOException, InstantiationException,
            IllegalAccessException {
        Service service = new Service();
        Site site = new Site(service, src, gen);
        Path p = Paths.get(src.toString()+"/1-url123|tag1=x|tag2=y|tag3=z|name=Myname.bla");
        XPath x = new XPath(site, p);
        Assert.assertEquals("1-url123|tag1=x|tag2=y|tag3=z|name=Myname.bla", x.url());
        Assert.assertEquals("", x.name());
        Assert.assertEquals(false, x.isNavigation());
        Assert.assertEquals(0, x.nr());
        Assert.assertEquals(0, x.properties().size());
        Assert.assertEquals(0, x.extensionList().size());
        Assert.assertEquals(false, x.isVisible());
        service.shutdown();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testException() throws IOException {
        Service service = new Service();
        Site site = new Site(service, src, gen);
        Path p = Paths.get("/tmp/1.txt");
        new XPath(site, p);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testException2() throws IOException {
        Service service = new Service();
        Site site = new Site(service, src, gen);
        Path p = Paths.get(src.toString()+"1.txt");
        new XPath(site, p);
    }

    @Test
    public void testNumber() throws IOException {
        Service service = new Service();
        Site site = new Site(service, src, gen);
        Path p = Paths.get(src.toString()+"/1.txt");
        XPath x = new XPath(site, p);
        Assert.assertFalse(x.isVisible());

        p = Paths.get(src.toString()+"/1-.txt");
        x = new XPath(site, p);
        Assert.assertTrue(x.isVisible());
        service.shutdown();
    }

    @Test
    public void testDate() throws IOException {
        Service service = new Service();
        Site site = new Site(service, src, gen);

        Path p = Paths.get(src.toString()+"/2014.txt");
        XPath x = new XPath(site, p);
        Assert.assertFalse(x.isVisible());

        p = Paths.get(src.toString()+"/2014-.txt");
        x = new XPath(site, p);
        Assert.assertTrue(x.isVisible());

        p = Paths.get(src.toString()+"/2014-01-01.txt");
        x = new XPath(site, p);
        Assert.assertFalse(x.isVisible());

        p = Paths.get(src.toString()+"/2014-01-01-.txt");
        x = new XPath(site, p);
        Assert.assertTrue(x.isVisible());

        p = Paths.get(src.toString()+"/2014-01-01_15:15:15.txt");
        x = new XPath(site, p);
        Assert.assertFalse(x.isVisible());

        p = Paths.get(src.toString()+"/2014-01-01_15:15:15-.txt");
        x = new XPath(site, p);
        Assert.assertTrue(x.isVisible());

        p = Paths.get(src.toString()+"/2014-myurl.txt");
        x = new XPath(site, p);
        Assert.assertTrue(x.isVisible());
        Assert.assertEquals("myurl", x.url());

        p = Paths.get(src.toString()+"/2014-|tag=value|.txt");
        x = new XPath(site, p);
        Assert.assertTrue(x.isVisible());

        p = Paths.get(src.toString()+"/2014-|tag=value.txt");
        x = new XPath(site, p);
        Assert.assertTrue(x.isVisible());

        service.shutdown();
    }
}
