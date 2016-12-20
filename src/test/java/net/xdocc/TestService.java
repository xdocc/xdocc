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

public class TestService {
    
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
    public void testStart() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, ".templates/list.ftl", "");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertTrue(Files.size(gen.resolve("index.html")) == 0);
    }
    
    @Test
    public void testTxt() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-test.txt", "this is a text file");
        Utils.createFile(src, ".templates/text.ftl", "This is a text file \n\n -- available variables: ${debug}");
        Utils.createFile(src, ".templates/list.ftl", "This is a list file \n\n -- available variables: ${debug}");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertTrue(Files.size(gen.resolve("test.html"))>0);
        Assert.assertTrue(Files.size(gen.resolve("index.html"))>0);
    }
    
    @Test
    public void testPage() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-test.txt", "this is a text file");
        Utils.createFile(src, ".xdocc", "page=true");
        Utils.createFile(src, ".templates/text.ftl", "This is a text file \n\n -- available variables: ${debug}");
        Utils.createFile(src, ".templates/list.ftl", "This is a list file \n\n -- available variables: ${debug}");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertFalse(Files.exists(gen.resolve("test.html")));
        Assert.assertTrue(Files.size(gen.resolve("index.html"))>0);
    }
    
    @Test
    public void testDocument() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-test.txt", "this is a text file");
        Utils.createFile(src, ".xdocc", "page=true");
        Utils.createFile(src, ".templates/text.ftl", "This is a text file <br><br> -- |${content}|");
        Utils.createFile(src, ".templates/list.ftl", "list file <br><br> -- <#list list as doc>[${doc.content}]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertTrue(Files.size(gen.resolve("index.html"))>0);
        Assert.assertEquals(FileUtils.readFileToString(gen.resolve("index.html").toFile()), "list file <br><br> -- [this is a text file]");
    }
    
    @Test
    public void testGenerate() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-test.txt", "this is a text file");
        Utils.createFile(src, ".xdocc", "page=true");
        Utils.createFile(src, ".templates/text.ftl", "text template <br><br> -- (${content})");
        Utils.createFile(src, ".templates/list.ftl", "list file <br><br> -- <#list list as doc>[${doc.generate}]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertTrue(Files.size(gen.resolve("index.html"))>0);
        Assert.assertEquals(FileUtils.readFileToString(gen.resolve("index.html").toFile()), "list file <br><br> -- [text template <br><br> -- (this is a text file)]");
    }
    
    @Test
    public void testGenerateList() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-test.txt", "this is a text file");
        Utils.createFile(src, ".xdocc", "page=true");
        Utils.createFile(src, "1-dir/1-test.txt", "this is a 2nd text file");
        Utils.createFile(src, "1-dir/.xdocc", "page=false\npromoted=true");
        Utils.createFile(src, ".templates/text.ftl", "text template <br><br> -- (${content})");
        Utils.createFile(src, ".templates/list.ftl", "list file <br><br> -- <#list list as doc>[${doc.generate}]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertTrue(Files.size(gen.resolve("index.html"))>0);
        Assert.assertTrue(Files.size(gen.resolve("dir/test.html"))>0);
        Assert.assertTrue(Files.size(gen.resolve("dir/index.html"))>0);
        Assert.assertEquals(FileUtils.readFileToString(gen.resolve("index.html").toFile()), "list file <br><br> -- [text template <br><br> -- (this is a text file)][text template <br><br> -- (this is a 2nd text file)]");
    }
}
