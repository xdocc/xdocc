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

public class TestCache {
    
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
        TestUtils.deleteDirectories(gen, src);
    }
    
    @Test
    public void testCache() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-test.txt", "this is a text file");
        TestUtils.createFile(src, ".templates/text.ftl", "${content}");
        TestUtils.createFile(src, ".templates/list.ftl", "${depth}/${promotedepth}<#list items as item>[${item.content}]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Cache cache =  Service.service().cache();
        for(int i=0;i<100;i++) {
            Service.restart(cache, "-w", src.toString(), "-o", gen.toString(), "-r", "-x");
            Assert.assertEquals(i+1, cache.hits());
        }
    }
    
    @Test
    public void testTextileImage() throws IOException, InterruptedException, ExecutionException {
        TestUtils.copyFile("imgs/label-1.jpg", src, "1-dir1/label-1.jpg");
        TestUtils.copyFile("imgs/label-2.jpg", src, "1-dir2/label-2.jpg");
        TestUtils.copyFile("imgs/label-3.jpg", src, "1-dir3/label-3.jpg");
        TestUtils.createFile(src, "1-dir1/1-read.textile", "!label-1.jpg!");
        TestUtils.createFile(src, "1-dir2/1-me.textile", "!label-2.jpg:thumb!");
        TestUtils.createFile(src, "1-dir3/1-first.textile", "!label-3.jpg:thumb!:../dir1/read.html");
        
        TestUtils.createFile(src, "1-dir4.prm/4-dir5.prm/1-link.link", "url=../../dir1 \nurl=../../dir2 \nurl=../../dir3");
        
        TestUtils.createFile(src, ".templates/link.ftl", "<#list items as item>(${item.content})</#list>");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        TestUtils.createFile(src, ".templates/wikitext.ftl", "${content}");
        
        TestUtils.createFile(src, ".templates/image_thumb.ftl", "thumb:<img src=${path}>");
        TestUtils.createFile(src, ".templates/image_norm.ftl", "norm:<img src=${path}>");
        TestUtils.createFile(src, ".templates/image_orig.ftl", "orig:<img src=${path}>");
        TestUtils.createFile(src, ".templates/image.ftl", "");
        
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Cache cache =  Service.service().cache();
        Service.restart(cache, "-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        
        Assert.assertEquals(29, cache.hits());
    }
}
