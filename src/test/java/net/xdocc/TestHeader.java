/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.xdocc;

import org.apache.commons.io.FileUtils;
import org.junit.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

public class TestHeader {
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
    public void testHeader() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-test.txt", "this is a text file <a href=\"${path}\">current</a>(<a href=\"${root}\">back</a>)");
        TestUtils.createFile(src, ".xdocc", "page=true");
        TestUtils.createFile(src, "2-dir1.nav/1-test.txt", "this is a 3rd text file <a href=\"${path}\">current</a>(<a href=\"${root}\">back</a>)");
        TestUtils.createFile(src, "2-dir1.nav/.xdocc", "page=true");

        TestUtils.createFile(src, ".templates/header.ftl", "[header in path: ${root}]");
        TestUtils.createFile(src, ".templates/text.ftl", "${content}");
        TestUtils.createFile(src, ".templates/page.ftl", "<#include \"header.ftl\">${content}");
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as key,item>${item.content}</#list>");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
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
        TestUtils.createFile(src, ".templates/list.ftl", "<#list items as key,item>${item.content}</#list>");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertTrue(Files.exists(gen.resolve("h2h.png")));
    }
}
