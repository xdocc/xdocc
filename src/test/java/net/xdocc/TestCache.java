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
    public void testImage() throws IOException, InterruptedException, ExecutionException {
        TestUtils.copyFile("imgs/label-1.jpg", src, "1-dir1.vis.prm/1-label.jpg");
        TestUtils.createFile(src, ".templates/image.ftl", "<#if items[0].link??>${items[0].content}</#if>|<#if items[1].link??>${items[1].content}</#if>|<#if items[2].link??>${items[2].content}</#if>");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        TestUtils.createFile(src, ".templates/image_thumb.ftl", "thumb:<img src=${path}>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Cache cache =  Service.service().cache();
        
        Service.restart(cache, "-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals(2, cache.hits());
        
    }
}
