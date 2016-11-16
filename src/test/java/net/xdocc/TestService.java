package net.xdocc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import junit.framework.Assert;

import org.junit.After;
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
        //Utils.deleteDirectories(gen, src, log);
    }
    
    @Test
    public void testStart() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, ".templates/list.ftl", "");
        Utils.createFile(src, ".templates/page.ftl", "");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
    }
    
    @Test
    public void testTxt() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-test.txt", "this is a text file");
        Utils.createFile(src, ".templates/text.ftl", "This is a text file \n\n -- available variables: ${debug}");
        Utils.createFile(src, ".templates/page.ftl", "<html><body>This is a page template <br><br> -- available variables: ${debug}</body></html>");
        Utils.createFile(src, ".templates/list.ftl", "This is a list file \n\n -- available variables: ${debug}");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
        Assert.assertTrue(Files.size(gen.resolve("test.html"))>0);
    }
}
