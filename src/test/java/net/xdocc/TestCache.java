/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.xdocc;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

public class TestCache {

    private static Path gen;
    private static Path src;
    private static Path cache;

    @Before
    public void setup() throws IOException {
        src = Files.createTempDirectory("src");
        gen = Files.createTempDirectory("gen");
        cache = Files.createTempDirectory("cache").resolve("cache");
        Files.createDirectories(src.resolve(".templates"));
        TestUtils.createFile(src, ".templates/page.ftl", "${content}");
    }

    @After
    public void tearDown() throws IOException {
        TestUtils.deleteDirectories(gen, src, cache);
    }

    @Test
    public void testCache() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-test.txt", "this is a text file");
        TestUtils.createFile(src, ".templates/text.ftl", "${content}");
        TestUtils.createFile(src, ".templates/list.ftl", "${depth}/${promotedepth}<#list items as item>[${item.content}]</#list>");
        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        for(int i=0;i<100;i++) {
            Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r");
            Assert.assertEquals(2, Service.service().cache().hits());
        }
    }

    @Test
    public void testImage() throws IOException, InterruptedException, ExecutionException {
        TestUtils.copyFile("imgs/label-1.jpg", src, "1-dir1.vis.prm/1-label.jpg");
        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r");
        Assert.assertEquals(3, Service.service().cache().hits());

    }

    @Test
    public void testPromoteCache() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-test.txt", "1");
        TestUtils.createFile(src, "1-dir|prm/1-test2|prm.txt", "2");
        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-x");
        while(Service.service().runCounter() < 1) {
            Thread.sleep(200);
        }
        Assert.assertEquals("2", FileUtils.readFileToString(gen.resolve("dir/index.html").toFile()));
        Assert.assertEquals("2", FileUtils.readFileToString(gen.resolve("dir/test2.html").toFile()));
        Assert.assertEquals("21", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        TestUtils.replaceFile(src, "1-dir|prm/1-test2|prm.txt", "3");
        while(Service.service().runCounter() < 2) {
            Thread.sleep(200);
        }

        Assert.assertEquals("3", FileUtils.readFileToString(gen.resolve("dir/index.html").toFile()));
        Assert.assertEquals("3", FileUtils.readFileToString(gen.resolve("dir/test2.html").toFile()));
        Assert.assertEquals("31", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
    }
}
