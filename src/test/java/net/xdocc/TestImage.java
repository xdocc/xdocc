package net.xdocc;


import net.xdocc.handlers.HandlerImage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TestImage {

    private static Path gen;
    private static Path src;
    private static Path cache;

    @Before
    public void setup() throws IOException {
        src = Files.createTempDirectory("src");
        gen = Files.createTempDirectory("gen");
        cache = Files.createTempDirectory("cache").resolve("cache");
        Files.createDirectories(src.resolve(".templates"));
    }

    @After
    public void tearDown() throws IOException {
        TestUtils.deleteDirectories(gen, src, cache);
    }

    @Test
    public void readAspectImage() throws IOException, InterruptedException {
        Path image = TestUtils.copyFile("imgs/large-example.jpg", src, "img/example.jpg");
        String size = HandlerImage.executeGetAspectSize(image.toString(), "16/9");
        Assert.assertEquals("3985.78x2242", size);
    }

    @Test
    public void readSizeImage() throws IOException, InterruptedException {
        Path image = TestUtils.copyFile("imgs/large-example.jpg", src, "img/example.jpg");
        String size = HandlerImage.executeGetSize(image.toString());
        Assert.assertEquals("3992x2242", size);
    }

    @Test
    public void cropImage() throws IOException, InterruptedException {
        Path image = TestUtils.copyFile("imgs/large-example.jpg", src, "img/example.jpg");
        String size = HandlerImage.executeGetAspectSize(image.toString(), "9/16");
        String tmp = HandlerImage.executeCropResize(image.toString(), 631, 1121, "/tmp/test.jpg" );
        size = HandlerImage.executeGetSize("/tmp/test.jpg");
        Assert.assertEquals("631x1121", size);
    }

    @Test
    public void cropImages() throws IOException, InterruptedException {
        Path image = TestUtils.copyFile("imgs/large-example.jpg", src, "img/example.jpg");
        Site site = new Site(src, gen);
        XPath xPath = XPath.get(site, image);
        List<Pair<Path,String>> list = HandlerImage.cropImages(xPath, "9/16", 100);
        Assert.assertEquals(4, list.size());
        Assert.assertEquals("1261w", list.get(0).element1());
        Assert.assertEquals("631w", list.get(1).element1());
        Assert.assertEquals("315w", list.get(2).element1());
        Assert.assertEquals("158w", list.get(3).element1());
    }

    @Test
    public void resizeImages() throws IOException, InterruptedException {
        Path image = TestUtils.copyFile("imgs/large-example.jpg", src, "img/example.jpg");
        Site site = new Site(src, gen);
        XPath xPath = XPath.get(site, image);
        List<Pair<Path,String>> list = HandlerImage.resizeImages(xPath, 100);
        Assert.assertEquals(5, list.size());
        Assert.assertEquals("3992w", list.get(0).element1());
        Assert.assertEquals("1996w", list.get(1).element1());
        Assert.assertEquals("998w", list.get(2).element1());
        Assert.assertEquals("499w", list.get(3).element1());
        Assert.assertEquals("250w", list.get(4).element1());
    }

    @Test
    public void compileImages() throws IOException, InterruptedException, ExecutionException {
        Path image = TestUtils.copyFile("imgs/large-example.jpg", src, "img/1-example.jpg");
        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
    }

    @Test
    public void compileImages2() throws IOException, InterruptedException, ExecutionException {
        Path image = TestUtils.copyFile("imgs/large-example.jpg", src, "img/1-example|link.jpg");
        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
    }

    @Test
    public void compileImages3() throws IOException, InterruptedException, ExecutionException {
        Path image = TestUtils.copyFile("imgs/large-example.jpg", src, "img/1-example|link|crop=1-1.jpg");
        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
    }
}
