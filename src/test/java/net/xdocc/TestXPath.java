package net.xdocc;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        TestUtils.deleteDirectories(gen, src);
    }

    @Test
    public void testRoot() throws IOException, InstantiationException, IllegalAccessException {
        Site site = new Site(src, gen);
        XPath root = XPath.get(site, src);
        Assert.assertEquals(false, root.isNavigation());
    }

    @Test
    public void testFile() throws IOException, InstantiationException,
            IllegalAccessException {
        Site site = new Site(src, gen);
        testFileAssert(site, src.toString()+"/1-url123|tag1=x|tag2=y|tag3=z|name=Myname|.txt");
        testFileAssert(site, src.toString()+"/1-url123|tag1=x|tag2=y|tag3=z|name=Myname.txt");
        testFileAssert(site, src.toString()+"/1-url123[Myname]|tag1=x|tag2=y|tag3=z||.txt");
        testFileAssert(site, src.toString()+"/1-url123[Myname]|tag1=x|tag2=y|tag3=z.txt");
        testFileAssert(site, src.toString()+"/1-url123[Myname]tag1=x|tag2=y|tag3=z.txt");
    }

    private void testFileAssert(Site site, String name) throws IOException, InstantiationException,
            IllegalAccessException {
        Path p = Paths.get(name);
        XPath x = XPath.get(site, p);
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
        Site site = new Site(src, gen);
        Path p = Paths.get(src.toString()+"/1-url123|tag1=x|tag2=y|tag3=z|name=Myname.bla");
        XPath x = XPath.get(site, p);
        Assert.assertEquals("1-url123|tag1=x|tag2=y|tag3=z|name=Myname.bla", x.url());
        Assert.assertEquals("", x.name());
        Assert.assertEquals(false, x.isNavigation());
        Assert.assertEquals(0, x.nr());
        Assert.assertEquals(0, x.properties().size());
        Assert.assertEquals(0, x.extensionList().size());
        Assert.assertEquals(false, x.isVisible());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testException() throws IOException {
        Site site = new Site(src, gen);
        Path p = Paths.get("/tmp/1.txt");
        XPath.get(site, p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testException2() throws IOException {
        Site site = new Site(src, gen);
        Path p = Paths.get(src.toString()+"1.txt");
        XPath.get(site, p);
    }

    @Test
    public void testNumber() throws IOException {
        Site site = new Site(src, gen);
        Path p = Paths.get(src.toString()+"/1.txt");
        XPath x = XPath.get(site, p);
        Assert.assertFalse(x.isVisible());

        p = Paths.get(src.toString()+"/1-.txt");
        x = XPath.get(site, p);
        Assert.assertTrue(x.isVisible());
    }

    @Test
    public void testURL() throws IOException {
        Site site = new Site(src, gen);
        Path p = Paths.get(src.toString()+"/label-1.jpg");
        XPath x = XPath.get(site, p);
        Assert.assertEquals("label-1",x.url());
    }

    @Test
    public void testDate() throws IOException {
        Site site = new Site(src, gen);

        Path p = Paths.get(src.toString()+"/2014.txt");
        XPath x = XPath.get(site, p);
        Assert.assertFalse(x.isVisible());

        p = Paths.get(src.toString()+"/2014-.txt");
        x = XPath.get(site, p);
        Assert.assertTrue(x.isVisible());

        p = Paths.get(src.toString()+"/2014-01-01.txt");
        x = XPath.get(site, p);
        Assert.assertFalse(x.isVisible());

        p = Paths.get(src.toString()+"/2014-01-01-.txt");
        x = XPath.get(site, p);
        Assert.assertTrue(x.isVisible());

        p = Paths.get(src.toString()+"/2014-01-01_15:15:15.txt");
        x = XPath.get(site, p);
        Assert.assertFalse(x.isVisible());

        p = Paths.get(src.toString()+"/2014-01-01_15:15:15-.txt");
        x = XPath.get(site, p);
        Assert.assertTrue(x.isVisible());

        p = Paths.get(src.toString()+"/2014-myurl.txt");
        x = XPath.get(site, p);
        Assert.assertTrue(x.isVisible());
        Assert.assertEquals("myurl", x.url());

        p = Paths.get(src.toString()+"/2014-|tag=value|.txt");
        x = XPath.get(site, p);
        Assert.assertTrue(x.isVisible());

        p = Paths.get(src.toString()+"/2014-|tag=value.txt");
        x = XPath.get(site, p);
        Assert.assertTrue(x.isVisible());
    }

    @Test
    public void testProperty() throws IOException {
        Site site = new Site(src, gen);
        Path p = Paths.get(src.toString()+"/2-files[File Structure]nav.md");
        XPath x = XPath.get(site, p);
        Assert.assertEquals("files", x.url());
        Assert.assertEquals("File Structure", x.name());
        Assert.assertEquals(2, x.nr());
        Assert.assertTrue(x.containsExtension("md"));
        Assert.assertTrue(x.isNavigation());
    }

    //@Test
    //public void test
}
