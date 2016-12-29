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
    public void testTextPath() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-test.txt", "this is a text file <a href=\"${path}\">current</a>(<a href=\"${pathtoroot}\">back</a>)");
        Utils.createFile(src, "1-dir1.nav/1-test.txt", "this is a 2nd text file <a href=\"${path}\">current</a>(<a href=\"${pathtoroot}\">back</a>)");
        Utils.createFile(src, "1-dir1.nav/.xdocc", "promote=true");
        Utils.createFile(src, "1-dir1.nav/1-subdir1.nav/1-test.txt", "this is a 3rd text file <a href=\"${path}\">current</a>(<a href=\"${pathtoroot}\">back</a>)");
        Utils.createFile(src, "1-dir1.nav/1-subdir1.nav/.xdocc", "promote=true");
        
        
        Utils.createFile(src, ".templates/text.ftl", "${content}");
        Utils.createFile(src, ".templates/list.ftl", "<#list items as item>[${item.content}]</#list>");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertEquals("[this is a text file <a href=\"\">current</a>(<a href=\"\">back</a>)][this is a 2nd text file <a href=\"dir1\">current</a>(<a href=\"\">back</a>)][this is a 3rd text file <a href=\"dir1/subdir1\">current</a>(<a href=\"\">back</a>)]", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("[this is a 2nd text file <a href=\"\">current</a>(<a href=\"../\">back</a>)][this is a 3rd text file <a href=\"subdir1\">current</a>(<a href=\"../\">back</a>)]", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
        Assert.assertEquals("[this is a 3rd text file <a href=\"\">current</a>(<a href=\"../../\">back</a>)]", FileUtils.readFileToString(gen.resolve("dir1/subdir1/index.html").toFile()));
    }
}
