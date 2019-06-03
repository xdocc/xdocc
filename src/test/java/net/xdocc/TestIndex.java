package net.xdocc;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

public class TestIndex {
    private static Path gen;
    private static Path src;
    private static Path cache;

    @Before
    public void setup() throws IOException {
        src = Files.createTempDirectory("src");
        gen = Files.createTempDirectory("gen");
        cache = Files.createTempDirectory("cache").resolve("cache");
        Files.createDirectories(src.resolve(".templates"));
        TestUtils.createFile(src, ".templates/page.ftl", "${content}");
    }

    @After
    public void tearDown() throws IOException {
        TestUtils.deleteDirectories(gen, src, cache);
    }

    @Test
    public void testIndex() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "2-item2.txt", "Item2");
        TestUtils.createFile(src, "3-item3.txt", "Item3");
        TestUtils.createFile(src, "4-item4|nidx.txt", "Item4");
        TestUtils.createFile(src, "1-dir1[Dir]prm/1-item1|prm.txt", "Item1");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertEquals("Item1Item2Item3", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
    }


}
