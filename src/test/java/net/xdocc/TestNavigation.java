/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.xdocc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestNavigation {
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
    public void testNavigation() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-test.txt", "this is a text file");
        TestUtils.createFile(src, "1-dir1.nav/1-test.txt", "this is a 1.1nd text file");
        TestUtils.createFile(src, "2-dir2.nav/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, "2-dir2.nav/2-subdir1.nav/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, "2-dir2.nav/2-subdir2.nav/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, "2-dir2.nav/3-subdir3/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, "2-dir2.nav/3-subdir3/1-subsubdir.nav/1-test.txt", "this is a 1.2nd text file");
        
        TestUtils.createFile(src, ".templates/text.ftl", "text template <br><br> -- (${content})");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list globalnav as link>[${link.url}(<#list link.children as link>[${link.url}]</#list>)]</#list>");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertEquals("[dir1()][dir2([dir2/subdir1][dir2/subdir2])]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("[dir1()][dir2([dir2/subdir1][dir2/subdir2])]", FileUtils.readFileToString(gen.resolve("dir2/index.html").toFile()));
    }
    
    @Test
    public void testLoacalNavigation() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-test.txt", "this is a text file");
        TestUtils.createFile(src, "1-dir1.nav/1-test.txt", "this is a 1.1nd text file");
        TestUtils.createFile(src, "2-dir2.nav/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, "2-dir2.nav/2-subdir1.nav/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, "2-dir2.nav/2-subdir2.nav/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, "2-dir2.nav/3-subdir3/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, "2-dir2.nav/3-subdir3/1-subsubdir.nav/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, ".templates/list.ftl", "<#if !isglobalnav><#if localnav??><#list localnav as link>[${link.url}]</#list></#if></#if>");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertEquals("", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("", FileUtils.readFileToString(gen.resolve("dir2/index.html").toFile()));
        Assert.assertEquals("[dir2/subdir3/subsubdir]", FileUtils.readFileToString(gen.resolve("dir2/subdir3/index.html").toFile()));
    }
    
    @Test
    public void testBreadcrumb() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-test.txt", "this is a text file");
        TestUtils.createFile(src, "1-dir1.nav/1-test.txt", "this is a 1.1nd text file");
        TestUtils.createFile(src, "2-dir2.nav/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, "2-dir2.nav/2-subdir1.nav/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, "2-dir2.nav/2-subdir2.nav/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, "2-dir2.nav/3-subdir3/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, "2-dir2.nav/3-subdir3/1-subsubdir.nav/1-test.txt", "this is a 1.2nd text file");
        
        TestUtils.createFile(src, ".templates/text.ftl", "text template <br><br> -- (${content})");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list breadcrumb as link>[${link.url}]</#list>");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertEquals("", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("[dir2][dir2/subdir3][dir2/subdir3/subsubdir]", FileUtils.readFileToString(gen.resolve("dir2/subdir3/subsubdir/index.html").toFile()));
    }
    
    @Test
    public void testCurrentNav() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-test.txt", "this is a text file");
        TestUtils.createFile(src, "1-dir1.nav/1-test.txt", "this is a 1.1nd text file");
        TestUtils.createFile(src, "2-dir2.nav/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, "2-dir2.nav/1-subdir1.nav/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, "2-dir2.nav/2-subdir2.nav/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, "2-dir2.nav/3-subdir3/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, "2-dir2.nav/3-subdir3/1-subsubdir.nav/1-test.txt", "this is a 1.2nd text file");
        
        TestUtils.createFile(src, ".templates/list.ftl", "<#if currentnav??>[${currentnav.url}]</#if>");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertEquals("", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("", FileUtils.readFileToString(gen.resolve("dir2/subdir3/index.html").toFile()));
        Assert.assertEquals("[dir2/subdir3/subsubdir]", FileUtils.readFileToString(gen.resolve("dir2/subdir3/subsubdir/index.html").toFile()));
        Assert.assertEquals("[dir2/subdir2]", FileUtils.readFileToString(gen.resolve("dir2/subdir2/index.html").toFile()));
    }

    @Test
    public void testCurrentNav2() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "2-dir2.nav/3-subdir3/1-subsubdir.nav/1-test.txt", "this is a 1.2nd text file");
        TestUtils.createFile(src, ".templates/list.ftl", "<#if currentnav??>[${currentnav.url}]</#if>");
        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertEquals("[dir2/subdir3/subsubdir]", FileUtils.readFileToString(gen.resolve("dir2/subdir3/subsubdir/index.html").toFile()));
    }
    
}
