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

public class TestHandler {
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
    public void testPath() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-test.txt", "this is a text file <a href=\"${path}\">current</a>(<a href=\"${pathtoroot}\">back</a>)");
        Utils.createFile(src, "2-dir1.nav/1-test.txt", "this is a 2nd text file <a href=\"${path}\">current</a>(<a href=\"${pathtoroot}\">back</a>)");
        Utils.createFile(src, "2-dir1.nav/.xdocc", "promote=true");
        Utils.createFile(src, "2-dir1.nav/2-subdir1.nav/1-test.txt", "this is a 3rd text file <a href=\"${path}\">current</a>(<a href=\"${pathtoroot}\">back</a>)");
        Utils.createFile(src, "2-dir1.nav/2-subdir1.nav/.xdocc", "promote=true");
        
        Utils.createFile(src, ".templates/text.ftl", "${content}");
        Utils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("[this is a text file <a href=\".\">current</a>(<a href=\"\">back</a>)][[this is a 2nd text file <a href=\"dir1\">current</a>(<a href=\"\">back</a>)][[this is a 3rd text file <a href=\"dir1/subdir1\">current</a>(<a href=\"\">back</a>)]]]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("[this is a 2nd text file <a href=\".\">current</a>(<a href=\"../\">back</a>)][[this is a 3rd text file <a href=\"subdir1\">current</a>(<a href=\"../\">back</a>)]]", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
        Assert.assertEquals("[this is a 3rd text file <a href=\".\">current</a>(<a href=\"../../\">back</a>)]", FileUtils.readFileToString(gen.resolve("dir1/subdir1/index.html").toFile()));
    }
    
    @Test
    public void testDepth1() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-test.txt", "this is a text file");
        Utils.createFile(src, "2-dir1.nav/1-test.txt", "this is a 2nd text file");
        Utils.createFile(src, "2-dir1.nav/.xdocc", "promote=true");
        Utils.createFile(src, "2-dir1.nav/2-subdir1.nav/1-test.txt", "this is a 3rd text file");
        Utils.createFile(src, "2-dir1.nav/2-subdir1.nav/.xdocc", "promote=true");
        
        Utils.createFile(src, ".templates/text.ftl", "${content}");
        Utils.createFile(src, ".templates/list.ftl", "${depth}/${promotedepth}<#list items as item>[${item.content}]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("0/0[this is a text file][1/1[this is a 2nd text file][2/2[this is a 3rd text file]]]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("1/0[this is a 2nd text file][2/1[this is a 3rd text file]]", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
        Assert.assertEquals("2/0[this is a 3rd text file]", FileUtils.readFileToString(gen.resolve("dir1/subdir1/index.html").toFile()));
    }
    
    @Test
    public void testDepth2() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-test.txt", "this is a text file");
        Utils.createFile(src, "2-dir1.nav/1-test.txt", "this is a 2nd text file");
        Utils.createFile(src, "2-dir1.nav/2-subdir1.nav/1-test.txt", "this is a 3rd text file");
        Utils.createFile(src, "2-dir1.nav/2-subdir1.nav/.xdocc", "promote=true");
        
        Utils.createFile(src, ".templates/text.ftl", "${content}");
        Utils.createFile(src, ".templates/list.ftl", "${depth}/${promotedepth}<#list items as item>[${item.content}]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("0/0[this is a text file]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("1/0[this is a 2nd text file][2/1[this is a 3rd text file]]", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
        Assert.assertEquals("2/0[this is a 3rd text file]", FileUtils.readFileToString(gen.resolve("dir1/subdir1/index.html").toFile()));
    }
    
    @Test
    public void testLayout1() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-test.txt", "this is a text file");
        Utils.createFile(src, "2-dir1|l=m.nav/1-test.txt", "this is a 2nd text file");
        Utils.createFile(src, "2-dir1|l=m.nav/.xdocc", "promote=true");
        Utils.createFile(src, "2-dir1|l=m.nav/2-subdir1.nav/1-test.txt", "this is a 3rd text file");
        Utils.createFile(src, "2-dir1|l=m.nav/2-subdir1.nav/.xdocc", "promote=true");
        
        Utils.createFile(src, ".templates/text.ftl", "${content}");
        Utils.createFile(src, ".templates/list.ftl", "${depth}/${promotedepth}<#list items as item>[${item.content}]</#list>");
        Utils.createFile(src, ".templates/list_m.ftl", "MM ${depth}/${promotedepth}<#list items as item>[${item.content}]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("0/0[this is a text file][MM 1/1[this is a 2nd text file][2/2[this is a 3rd text file]]]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("MM 1/0[this is a 2nd text file][2/1[this is a 3rd text file]]", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
        Assert.assertEquals("2/0[this is a 3rd text file]", FileUtils.readFileToString(gen.resolve("dir1/subdir1/index.html").toFile()));
    }
    
    @Test
    public void testLayout2() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-test.txt", "this is a text file");
        Utils.createFile(src, "2-dir1|l1=m.nav/1-test.txt", "this is a 2nd text file");
        Utils.createFile(src, "2-dir1|l1=m.nav/.xdocc", "promote=true");
        Utils.createFile(src, "2-dir1|l1=m.nav/2-subdir1.nav/1-test.txt", "this is a 3rd text file");
        Utils.createFile(src, "2-dir1|l1=m.nav/2-subdir1.nav/.xdocc", "promote=true");
        
        Utils.createFile(src, ".templates/text.ftl", "${content}");
        Utils.createFile(src, ".templates/text_m.ftl", "MX ${content}");
        Utils.createFile(src, ".templates/list.ftl", "${depth}/${promotedepth}<#list items as item>[${item.content}]</#list>");
        Utils.createFile(src, ".templates/list_m.ftl", "MM ${depth}/${promotedepth}<#list items as item>[${item.content}]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("0/0[this is a text file][MM 1/1[MX this is a 2nd text file][MM 2/2[this is a 3rd text file]]]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("MM 1/0[MX this is a 2nd text file][MM 2/1[this is a 3rd text file]]", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
        Assert.assertEquals("MM 2/0[this is a 3rd text file]", FileUtils.readFileToString(gen.resolve("dir1/subdir1/index.html").toFile()));
    }
    
    @Test
    public void testLink1() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-test.txt", "this is a text file");
        Utils.createFile(src, "2-link.link", "url=dir1");
        Utils.createFile(src, "1-dir1/1-test.txt", "<a href=\"${path}/hallo.html\">hallo</a>");
        Utils.createFile(src, "1-dir1/2-hallo.txt", "<a href=\"${path}/test.html\">test</a>");
        Utils.createFile(src, ".templates/text.ftl", "${content}");
        //Utils.createFile(src, ".templates/list.ftl", "${depth}/${promotedepth}<#list items as item>[${item.content}]</#list>");
        Utils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        Utils.createFile(src, ".templates/link.ftl", "<#list items as item>(${item.content})</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("[this is a text file][([<a href=\"dir1/hallo.html\">hallo</a>][<a href=\"dir1/test.html\">test</a>])]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("[<a href=\"./hallo.html\">hallo</a>][<a href=\"./test.html\">test</a>]", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
    }
    
    @Test
    public void testLink2() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-dir1/1-subdir/1-test.txt", "<a href=\"${path}/hallo.html\">hallo</a>");
        Utils.createFile(src, "1-dir1/1-subdir/2-hallo.txt", "<a href=\"${path}/test.html\">test</a>");
        Utils.createFile(src, "1-dir1/1-subdir/.xdocc" ,"promote=true");
        
        Utils.createFile(src, "3-dir3/1-subdir/1-linkabs.link", "url=/dir1");
        Utils.createFile(src, "3-dir3/1-subdir/2-linkrel.link", "url=../../dir1");
        
        Utils.createFile(src, ".templates/text.ftl", "${content}");
        Utils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        Utils.createFile(src, ".templates/link.ftl", "<#list items as item>(${item.content})</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("[([[<a href=\"../../dir1/subdir/hallo.html\">hallo</a>][<a href=\"../../dir1/subdir/test.html\">test</a>]])][([[<a href=\"../../dir1/subdir/hallo.html\">hallo</a>][<a href=\"../../dir1/subdir/test.html\">test</a>]])]", FileUtils.readFileToString(gen.resolve("dir3/subdir/index.html").toFile()));
        Assert.assertEquals("([[<a href=\"../../dir1/subdir/hallo.html\">hallo</a>][<a href=\"../../dir1/subdir/test.html\">test</a>]])", FileUtils.readFileToString(gen.resolve("dir3/subdir/linkabs.html").toFile()));
        Assert.assertEquals("([[<a href=\"../../dir1/subdir/hallo.html\">hallo</a>][<a href=\"../../dir1/subdir/test.html\">test</a>]])", FileUtils.readFileToString(gen.resolve("dir3/subdir/linkrel.html").toFile()));
        Assert.assertEquals("[[<a href=\"subdir/hallo.html\">hallo</a>][<a href=\"subdir/test.html\">test</a>]]", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
        Assert.assertEquals("[<a href=\"./hallo.html\">hallo</a>][<a href=\"./test.html\">test</a>]", FileUtils.readFileToString(gen.resolve("dir1/subdir/index.html").toFile()));
    }
    
    @Test
    public void testCopy() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-dir1/read.me", "copy this data 1:1");
        Utils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("copy this data 1:1", FileUtils.readFileToString(gen.resolve("dir1/read.me").toFile()));
    }
    
    @Test
    public void testTextile() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-dir1/1-read.textile", "h1. A headline");
        Utils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        Utils.createFile(src, ".templates/wikitext.ftl", "${content}");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("<h1 id=\"Aheadline\">A headline</h1>", FileUtils.readFileToString(gen.resolve("dir1/read.html").toFile()));
    }
    
    @Test
    public void testTextileLink() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-dir1/1-read.textile", "h1. A headline\n\n \"Link1\":${path}/../dir2 to 2");
        Utils.createFile(src, "1-dir2/1-me.textile", "h1. Title\n\n \"Link2\":${path}/../dir1 to 1 ");
        
        Utils.createFile(src, "3-dir3.prm/4-dir4.prm/1-link.link", "url=../../dir1 \nurl=../../dir2");
        
        Utils.createFile(src, ".templates/link.ftl", "<#list items as item>(${item.content})</#list>");
        Utils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        Utils.createFile(src, ".templates/wikitext.ftl", "${content}");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("<h1 id=\"Aheadline\">A headline</h1><a href=\"./../dir2\">Link1</a> to 2", FileUtils.readFileToString(gen.resolve("dir1/read.html").toFile()));
        Assert.assertEquals("[([<h1 id=\"Aheadline\">A headline</h1><a href=\"../../dir1/../dir2\">Link1</a> to 2])([<h1 id=\"Title\">Title</h1><a href=\"../../dir2/../dir1\">Link2</a> to 1 ])]", FileUtils.readFileToString(gen.resolve("dir3/dir4/index.html").toFile()));
    }
    
    @Test
    public void testImage() throws IOException, InterruptedException, ExecutionException {
        Utils.copyFile("imgs/label-1.jpg", src, "1-dir1.vis.prm/label-1.jpg");
        Utils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        Utils.createFile(src, ".templates/image_thumb.ftl", "thumb:<img src=${path}>");
        Utils.createFile(src, ".templates/image_norm.ftl", "norm:<img src=${path}>");
        Utils.createFile(src, ".templates/image_orig.ftl", "orig:<img src=${path}>");
        Utils.createFile(src, ".templates/image.ftl", "<#if items[0].link??>${items[0].content}</#if>|<#if items[1].link??>${items[1].content}</#if>|<#if items[2].link??>${items[2].content}</#if>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("[[|thumb:<img src=dir1/label-1_t.jpg>|norm:<img src=dir1/label-1_n.jpg>]]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("[|thumb:<img src=label-1_t.jpg>|norm:<img src=label-1_n.jpg>]", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
        
        Assert.assertTrue(Files.exists(gen.resolve("dir1/label-1_n.jpg")));
        Assert.assertTrue(Files.exists(gen.resolve("dir1/label-1_n.html")));
        Assert.assertTrue(Files.exists(gen.resolve("dir1/label-1_t.jpg")));
        Assert.assertTrue(Files.exists(gen.resolve("dir1/label-1_t.html")));
        
    }
            
}
