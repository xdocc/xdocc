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

    @Before
    public void setup() throws IOException {
        src = Files.createTempDirectory("src");
        gen = Files.createTempDirectory("gen");
        Files.createDirectories(src.resolve(".templates"));
    }

    @After
    public void tearDown() throws IOException {
        Utils.deleteDirectories(gen, src);
    }
    
    @Test
    public void testNavigation() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-test.txt", "this is a text file");
        Utils.createFile(src, "1-dir1.nav/1-test.txt", "this is a 1.1nd text file");
        Utils.createFile(src, "2-dir2.nav/1-test.txt", "this is a 1.2nd text file");
        Utils.createFile(src, "2-dir2.nav/2-subdir1.nav/1-test.txt", "this is a 1.2nd text file");
        Utils.createFile(src, "2-dir2.nav/2-subdir2.nav/1-test.txt", "this is a 1.2nd text file");
        Utils.createFile(src, "2-dir2.nav/3-subdir2/1-test.txt", "this is a 1.2nd text file");
        Utils.createFile(src, "2-dir2.nav/3-subdir2/1-subsubdir.nav/1-test.txt", "this is a 1.2nd text file");
        
        Utils.createFile(src, ".templates/text.ftl", "text template <br><br> -- (${content})");
        Utils.createFile(src, ".templates/list.ftl", "<#list navigation as link>[${link.url}(<#list link.children as link>[${link.url}]</#list>)]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("[dir1()][dir2([dir2/subdir1][dir2/subdir2])]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("[dir1()][dir2([dir2/subdir1][dir2/subdir2])]", FileUtils.readFileToString(gen.resolve("dir2/index.html").toFile()));
    }
    
    @Test
    public void testLoacalNavigation() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-test.txt", "this is a text file");
        Utils.createFile(src, "1-dir1.nav/1-test.txt", "this is a 1.1nd text file");
        Utils.createFile(src, "2-dir2.nav/1-test.txt", "this is a 1.2nd text file");
        Utils.createFile(src, "2-dir2.nav/2-subdir1.nav/1-test.txt", "this is a 1.2nd text file");
        Utils.createFile(src, "2-dir2.nav/2-subdir2.nav/1-test.txt", "this is a 1.2nd text file");
        Utils.createFile(src, "2-dir2.nav/3-subdir2/1-test.txt", "this is a 1.2nd text file");
        Utils.createFile(src, "2-dir2.nav/3-subdir2/1-subsubdir.nav/1-test.txt", "this is a 1.2nd text file");
        
        Utils.createFile(src, ".templates/text.ftl", "text template <br><br> -- (${content})");
        Utils.createFile(src, ".templates/list.ftl", "<#if localnav??><#list localnav as link>[${link.url}]</#list></#if>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("[dir2/subdir2/subsubdir]", FileUtils.readFileToString(gen.resolve("dir2/subdir2/index.html").toFile()));
    }
}
