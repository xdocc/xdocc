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

public class TestDelete {
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
    public void testDelete() throws IOException, InterruptedException, ExecutionException {
        TestUtils.copyFile("imgs/label-2.jpg", src, "1-dir1/label-2.jpg");
        TestUtils.createFile(gen, "dir1/del1.me", "h1. A headline");
        TestUtils.createFile(gen, "del2.me", "h1. A headline");
        TestUtils.createFile(gen, "dir1/read.html", "h1. A headline");
        TestUtils.createFile(src, "1-dir1/1-read.textile", "!label-2.jpg!");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        TestUtils.createFile(src, ".templates/wikitext.ftl", "${content}");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertEquals("<p><img border=\"0\" src=\"./label-2.jpg\"/></p>", FileUtils.readFileToString(gen.resolve("dir1/read.html").toFile()));
        Assert.assertFalse(Files.exists(gen.resolve("dir1/del1.me")));
    }
    
    @Test
    public void testDeleteNoImage() throws IOException, InterruptedException, ExecutionException {
        TestUtils.copyFile("imgs/label-2.jpg", src, "1-dir1/label-2.jpg");
        TestUtils.createFile(gen, "dir1/del1.me", "h1. A headline");
        TestUtils.createFile(gen, "del2.me", "h1. A headline");
        TestUtils.createFile(gen, "dir1/read.html", "h1. A headline");
        TestUtils.createFile(src, "1-dir1/1-read.textile", "!label-2.jpg! \"test\":label-2.jpg");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        TestUtils.createFile(src, ".templates/wikitext.ftl", "${content}");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertTrue(Files.exists(gen.resolve("dir1/label-2.jpg")));
        Assert.assertEquals("<p><img border=\"0\" src=\"./label-2.jpg\"/> <a href=\"./label-2.jpg\">test</a></p>", FileUtils.readFileToString(gen.resolve("dir1/read.html").toFile()));
    }
    
    @Test
    public void testDeleteNoImage2() throws IOException, InterruptedException, ExecutionException {
        TestUtils.copyFile("imgs/label-2.jpg", src, "1-dir1/label-2.jpg");
        TestUtils.copyFile("imgs/label-3.jpg", src, "1-dir1/label-3.jpg");
        TestUtils.createFile(gen, "dir1/del1.me", "h1. A headline");
        TestUtils.createFile(gen, "del2.me", "h1. A headline");
        TestUtils.createFile(gen, "dir1/read.html", "h1. A headline");
        TestUtils.createFile(src, "1-dir1/1-read.textile", "!label-2.jpg! !label-3.jpg! \"test\":label-2.jpg");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        TestUtils.createFile(src, ".templates/wikitext.ftl", "${content}");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertTrue(Files.exists(gen.resolve("dir1/label-2.jpg")));
        Assert.assertTrue(Files.exists(gen.resolve("dir1/label-3.jpg")));
    }
    
    @Test
    public void testDeleteDirector() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(gen, "dir1/del1.me", "h1. A headline");
        TestUtils.createFile(gen, "dir2/del2.me", "h1. A headline");
        TestUtils.createFile(gen, "del2.me", "h1. A headline");
        TestUtils.createFile(gen, "dir1/read.html", "h1. A headline");
        TestUtils.createFile(src, "1-dir1/1-read.textile", "hallo");
        
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        TestUtils.createFile(src, ".templates/wikitext.ftl", "${content}");
        TestUtils.copyFile("imgs/label-2.jpg", gen, "dir1/label-2.jpg");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertFalse(Files.exists(gen.resolve("dir2")));
        Assert.assertFalse(Files.exists(gen.resolve("dir2/del2.me")));
        Assert.assertFalse(Files.exists(gen.resolve("dir2/label-2.jpg")));



    }
}
