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
        TestUtils.deleteDirectories(gen, src);
    }
    
    @Test
    public void testHeader() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-test.txt", "this is a text file <a href=\"${path}\">current</a>(<a href=\"${root}\">back</a>)");
        TestUtils.createFile(src, ".xdocc", "page=true");
        TestUtils.createFile(src, "2-dir1.nav/1-test.txt", "this is a 3rd text file <a href=\"${path}\">current</a>(<a href=\"${root}\">back</a>)");
        TestUtils.createFile(src, "2-dir1.nav/.xdocc", "page=true");
        
        TestUtils.createFile(src, ".templates/header.ftl", "[header in path: ${root}]");
        TestUtils.createFile(src, ".templates/text.ftl", "${content}");
        TestUtils.createFile(src, ".templates/page.ftl", "<#include \"header.ftl\">${content}");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as item>${item.content}</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("[header in path: ../]this is a 3rd text file <a href=\".\">current</a>(<a href=\"../\">back</a>)", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
    }
    
    @Test
    public void testCopyRoot() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-test.txt", "this is a text file");
        TestUtils.createFile(src, "h2h.png", "this is an image");
        TestUtils.createFile(src, ".xdocc", "page=true");
        
        TestUtils.createFile(src, ".templates/header.ftl", "[header in path: ${root}]");
        TestUtils.createFile(src, ".templates/text.ftl", "${content}");
        TestUtils.createFile(src, ".templates/page.ftl", "<#include \"header.ftl\">${content}");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as item>${item.content}</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertTrue(Files.exists(gen.resolve("h2h.png")));
    }
    
    
    @Test
    public void testPath() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-test.txt", "this is a text file <a href=\"${path}\">current</a>(<a href=\"${root}\">back</a>)");
        TestUtils.createFile(src, "2-dir1.nav/1-test.txt", "this is a 2nd text file <a href=\"${path}\">current</a>(<a href=\"${root}\">back</a>)");
        TestUtils.createFile(src, "2-dir1.nav/.xdocc", "promote=true");
        TestUtils.createFile(src, "2-dir1.nav/2-subdir1.nav/1-test.txt", "this is a 3rd text file <a href=\"${path}\">current</a>(<a href=\"${root}\">back</a>)");
        TestUtils.createFile(src, "2-dir1.nav/2-subdir1.nav/.xdocc", "promote=true");
        
        TestUtils.createFile(src, ".templates/text.ftl", "${content}");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("[this is a text file <a href=\".\">current</a>(<a href=\".\">back</a>)][[this is a 2nd text file <a href=\"dir1\">current</a>(<a href=\".\">back</a>)][[this is a 3rd text file <a href=\"dir1/subdir1\">current</a>(<a href=\".\">back</a>)]]]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("[this is a 2nd text file <a href=\".\">current</a>(<a href=\"../\">back</a>)][[this is a 3rd text file <a href=\"subdir1\">current</a>(<a href=\"../\">back</a>)]]", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
        Assert.assertEquals("[this is a 3rd text file <a href=\".\">current</a>(<a href=\"../../\">back</a>)]", FileUtils.readFileToString(gen.resolve("dir1/subdir1/index.html").toFile()));
    }
    
    @Test
    public void testDepth1() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-test.txt", "this is a text file");
        TestUtils.createFile(src, "2-dir1.nav/1-test.txt", "this is a 2nd text file");
        TestUtils.createFile(src, "2-dir1.nav/.xdocc", "promote=true");
        TestUtils.createFile(src, "2-dir1.nav/2-subdir1.nav/1-test.txt", "this is a 3rd text file");
        TestUtils.createFile(src, "2-dir1.nav/2-subdir1.nav/.xdocc", "promote=true");
        
        TestUtils.createFile(src, ".templates/text.ftl", "${content}");
        TestUtils.createFile(src, ".templates/list.ftl", "${depth}/${promotedepth}<#list items as item>[${item.content}]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("0/0[this is a text file][1/1[this is a 2nd text file][2/2[this is a 3rd text file]]]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("1/0[this is a 2nd text file][2/1[this is a 3rd text file]]", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
        Assert.assertEquals("2/0[this is a 3rd text file]", FileUtils.readFileToString(gen.resolve("dir1/subdir1/index.html").toFile()));
    }
    
    @Test
    public void testDepth2() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-test.txt", "this is a text file");
        TestUtils.createFile(src, "2-dir1.nav/1-test.txt", "this is a 2nd text file");
        TestUtils.createFile(src, "2-dir1.nav/2-subdir1.nav/1-test.txt", "this is a 3rd text file");
        TestUtils.createFile(src, "2-dir1.nav/2-subdir1.nav/.xdocc", "promote=true");
        
        TestUtils.createFile(src, ".templates/text.ftl", "${content}");
        TestUtils.createFile(src, ".templates/list.ftl", "${depth}/${promotedepth}<#list items as item>[${item.content}]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("0/0[this is a text file]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("1/0[this is a 2nd text file][2/1[this is a 3rd text file]]", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
        Assert.assertEquals("2/0[this is a 3rd text file]", FileUtils.readFileToString(gen.resolve("dir1/subdir1/index.html").toFile()));
    }
    
    @Test
    public void testLayout1() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-test.txt", "this is a text file");
        TestUtils.createFile(src, "2-dir1|l=m.nav/1-test.txt", "this is a 2nd text file");
        TestUtils.createFile(src, "2-dir1|l=m.nav/.xdocc", "promote=true");
        TestUtils.createFile(src, "2-dir1|l=m.nav/2-subdir1.nav/1-test.txt", "this is a 3rd text file");
        TestUtils.createFile(src, "2-dir1|l=m.nav/2-subdir1.nav/.xdocc", "promote=true");
        
        TestUtils.createFile(src, ".templates/text.ftl", "${content}");
        TestUtils.createFile(src, ".templates/list.ftl", "${depth}/${promotedepth}<#list items as item>[${item.content}]</#list>");
        TestUtils.createFile(src, ".templates/list_m.ftl", "MM ${depth}/${promotedepth}<#list items as item>[${item.content}]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("0/0[this is a text file][MM 1/1[this is a 2nd text file][2/2[this is a 3rd text file]]]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("MM 1/0[this is a 2nd text file][2/1[this is a 3rd text file]]", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
        Assert.assertEquals("2/0[this is a 3rd text file]", FileUtils.readFileToString(gen.resolve("dir1/subdir1/index.html").toFile()));
    }
    
    @Test
    public void testLayout2() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-test.txt", "this is a text file");
        TestUtils.createFile(src, "2-dir1|l1=m.nav/1-test.txt", "this is a 2nd text file");
        TestUtils.createFile(src, "2-dir1|l1=m.nav/.xdocc", "promote=true");
        TestUtils.createFile(src, "2-dir1|l1=m.nav/2-subdir1.nav/1-test.txt", "this is a 3rd text file");
        TestUtils.createFile(src, "2-dir1|l1=m.nav/2-subdir1.nav/.xdocc", "promote=true");
        
        TestUtils.createFile(src, ".templates/text.ftl", "${content}");
        TestUtils.createFile(src, ".templates/text_m.ftl", "MX ${content}");
        TestUtils.createFile(src, ".templates/list.ftl", "${depth}/${promotedepth}<#list items as item>[${item.content}]</#list>");
        TestUtils.createFile(src, ".templates/list_m.ftl", "MM ${depth}/${promotedepth}<#list items as item>[${item.content}]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("0/0[this is a text file][MM 1/1[MX this is a 2nd text file][MM 2/2[this is a 3rd text file]]]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("MM 1/0[MX this is a 2nd text file][MM 2/1[this is a 3rd text file]]", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
        Assert.assertEquals("MM 2/0[this is a 3rd text file]", FileUtils.readFileToString(gen.resolve("dir1/subdir1/index.html").toFile()));
    }
    
    @Test
    public void testLink1() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-test.txt", "this is a text file");
        TestUtils.createFile(src, "2-link.link", "url=dir1");
        TestUtils.createFile(src, "1-dir1/1-test.txt", "<a href=\"${path}/hallo.html\">hallo</a>");
        TestUtils.createFile(src, "1-dir1/2-hallo.txt", "<a href=\"${path}/test.html\">test</a>");
        TestUtils.createFile(src, ".templates/text.ftl", "${content}");
        //Utils.createFile(src, ".templates/list.ftl", "${depth}/${promotedepth}<#list items as item>[${item.content}]</#list>");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        TestUtils.createFile(src, ".templates/link.ftl", "<#list items as item>(${item.content})</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("[this is a text file][([<a href=\"dir1/hallo.html\">hallo</a>][<a href=\"dir1/test.html\">test</a>])]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("[<a href=\"./hallo.html\">hallo</a>][<a href=\"./test.html\">test</a>]", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
    }
    
    @Test
    public void testLink2() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-dir1/1-subdir/1-test.txt", "<a href=\"${path}/hallo.html\">hallo</a>");
        TestUtils.createFile(src, "1-dir1/1-subdir/2-hallo.txt", "<a href=\"${path}/test.html\">test</a>");
        TestUtils.createFile(src, "1-dir1/1-subdir/.xdocc" ,"promote=true");
        
        TestUtils.createFile(src, "3-dir3/1-subdir/1-linkabs.link", "url=/dir1");
        TestUtils.createFile(src, "3-dir3/1-subdir/2-linkrel.link", "url=../../dir1");
        
        TestUtils.createFile(src, ".templates/text.ftl", "${content}");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        TestUtils.createFile(src, ".templates/link.ftl", "<#list items as item>(${item.content})</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("[([[<a href=\"../../dir1/subdir/hallo.html\">hallo</a>][<a href=\"../../dir1/subdir/test.html\">test</a>]])][([[<a href=\"../../dir1/subdir/hallo.html\">hallo</a>][<a href=\"../../dir1/subdir/test.html\">test</a>]])]", FileUtils.readFileToString(gen.resolve("dir3/subdir/index.html").toFile()));
        Assert.assertEquals("([[<a href=\"../../dir1/subdir/hallo.html\">hallo</a>][<a href=\"../../dir1/subdir/test.html\">test</a>]])", FileUtils.readFileToString(gen.resolve("dir3/subdir/linkabs.html").toFile()));
        Assert.assertEquals("([[<a href=\"../../dir1/subdir/hallo.html\">hallo</a>][<a href=\"../../dir1/subdir/test.html\">test</a>]])", FileUtils.readFileToString(gen.resolve("dir3/subdir/linkrel.html").toFile()));
        Assert.assertEquals("[[<a href=\"subdir/hallo.html\">hallo</a>][<a href=\"subdir/test.html\">test</a>]]", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
        Assert.assertEquals("[<a href=\"./hallo.html\">hallo</a>][<a href=\"./test.html\">test</a>]", FileUtils.readFileToString(gen.resolve("dir1/subdir/index.html").toFile()));
    }
    
    @Test
    public void testLink3() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-test.txt", "first item");
        TestUtils.createFile(src, "2-link.link", "url=dir3/*\nlimit=1");
        TestUtils.createFile(src, "3-hallo.txt", "third item");
        TestUtils.createFile(src, ".xdocc" ,"page=true");
        
        TestUtils.createFile(src, "3-dir3/4-test4.txt", "test4");
        TestUtils.createFile(src, "3-dir3/5-test5.txt", "test5");
        TestUtils.createFile(src, "3-dir3/6-test6.txt", "test6");
        
        TestUtils.createFile(src, ".templates/text.ftl", "${content}");
        TestUtils.createFile(src, ".templates/page.ftl", "${content}");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        TestUtils.createFile(src, ".templates/link.ftl", "<#list items as item>(${item.content})</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        
        Assert.assertEquals("[first item][(test4)][third item]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
    }
    
    @Test
    public void testCopy() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-dir1/read.me", "copy this data 1:1");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("copy this data 1:1", FileUtils.readFileToString(gen.resolve("dir1/read.me").toFile()));
    }
    
    @Test
    public void testTextile() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-dir1/1-read.textile", "h1. A headline");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        TestUtils.createFile(src, ".templates/wikitext.ftl", "${content}");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("<h1 id=\"Aheadline\">A headline</h1>", FileUtils.readFileToString(gen.resolve("dir1/read.html").toFile()));
    }
    
    @Test
    public void testTextileLink() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-dir1/1-read.textile", "h1. A headline\n\n \"Link1\":../dir2 to 2");
        TestUtils.createFile(src, "1-dir2/1-me.textile", "h1. Title\n\n \"Link2\":../dir1 to 1 ");
        
        TestUtils.createFile(src, "3-dir3.prm/4-dir4.prm/1-link.link", "url=../../dir1 \nurl=../../dir2");
        
        TestUtils.createFile(src, ".templates/link.ftl", "<#list items as item>(${item.content})</#list>");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        TestUtils.createFile(src, ".templates/wikitext.ftl", "${content}");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("<h1 id=\"Aheadline\">A headline</h1><a href=\"./../dir2\">Link1</a> to 2", FileUtils.readFileToString(gen.resolve("dir1/read.html").toFile()));
        Assert.assertEquals("[([<h1 id=\"Aheadline\">A headline</h1><a href=\"../../dir1/../dir2\">Link1</a> to 2])([<h1 id=\"Title\">Title</h1><a href=\"../../dir2/../dir1\">Link2</a> to 1 ])]", FileUtils.readFileToString(gen.resolve("dir3/dir4/index.html").toFile()));
    }
    
    @Test
    public void testTextileImage() throws IOException, InterruptedException, ExecutionException {
        TestUtils.copyFile("imgs/label-1.jpg", src, "1-dir1/label-1.jpg");
        TestUtils.copyFile("imgs/label-2.jpg", src, "1-dir2/label-2.jpg");
        TestUtils.copyFile("imgs/label-3.jpg", src, "1-dir3/label-3.jpg");
        TestUtils.createFile(src, "1-dir1/1-read.textile", "!label-1.jpg!");
        TestUtils.createFile(src, "1-dir2/1-me.textile", "!label-2.jpg!");
        TestUtils.createFile(src, "1-dir3/1-first.textile", "!label-3.jpg!:../dir1/read.html");
        
        TestUtils.createFile(src, "1-dir4.prm/4-dir5.prm/1-link.link", "url=../../dir1 \nurl=../../dir2 \nurl=../../dir3");
        
        TestUtils.createFile(src, ".templates/link.ftl", "<#list items as item>(${item.content})</#list>");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        TestUtils.createFile(src, ".templates/wikitext.ftl", "${content}");
        
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("[[[([<p><img border=\"0\" src=\"dir1/label-1.jpg\"/></p>])([<p><img border=\"0\" src=\"dir2/label-2.jpg\"/></p>])([<p><a href=\"dir3/../dir1/read.html\"><img border=\"0\" src=\"dir3/label-3.jpg\"/></a></p>])]]]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
    }
    
    @Test
    public void testMarkdown() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-dir1/1-read.md", "# A headline");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        TestUtils.createFile(src, ".templates/markdown.ftl", "${content}");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("<h1>A headline</h1>", FileUtils.readFileToString(gen.resolve("dir1/read.html").toFile()).trim());
    }
            
}
