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

public class TestPromote {
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
    public void testPromote1() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "2-item2.txt", "Item2");
        TestUtils.createFile(src, "3-item3.txt", "Item3");
        TestUtils.createFile(src, "4-item4.txt", "Item4");
        TestUtils.createFile(src, "1-dir1[Dir]prm/1-item1.txt", "Item1");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertEquals("Item1Item2Item3Item4", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
    }

    @Test
    public void testPromote2() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "2-item2.txt", "Item2");
        TestUtils.createFile(src, "3-item3.txt", "Item3");
        TestUtils.createFile(src, "4-item4.txt", "Item4");
        TestUtils.createFile(src, "1-dir1[Dir]prm/6-dir6|prm/1-item1|prm.txt", "Item1");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertEquals("Item1Item2Item3Item4", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
    }

    @Test
    public void testPromote3() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-dir1/2-item2.txt", "Item2");
        TestUtils.createFile(src, "1-dir1/3-item3.txt", "Item3");
        TestUtils.createFile(src, "1-dir1/4-item4.txt", "Item4");
        TestUtils.createFile(src, "1-dir1/1-dir1[Dir]prm/6-dir6|prm/1-item1|prm.txt", "Item1");
        TestUtils.createFile(src, "1-dir1/1-dir1[Dir]prm/6-dir6|prm/2-item2|prm.txt", "Item1.2");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertEquals("Item1Item1.2Item2Item3Item4", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
    }

    @Test
    public void testPromote4() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-dir1/2-item2.txt", "Item2");
        TestUtils.createFile(src, "1-dir1/3-item3.txt", "Item3");
        TestUtils.createFile(src, "1-dir1/4-item4.txt", "Item4");
        TestUtils.createFile(src, "1-dir1/1-dir1[Dir]prm/6-dir6|prm/1-item1|prm.txt", "Item1");
        TestUtils.createFile(src, "1-dir1/1-dir1[Dir]prm/6-dir6/2-item2.txt", "Item1.2");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertEquals("Item1Item2Item3Item4", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
    }

    @Test
    public void testPromote5() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-dir1/2-item2.txt", "Item2");
        TestUtils.createFile(src, "1-dir1/3-item3.txt", "Item3");
        TestUtils.createFile(src, "1-dir1/4-item4.txt", "Item4");
        TestUtils.createFile(src, "1-dir1/1-dir1[Dir]prm/6-dir6|prm/1-item1|prm.txt", "Item1");
        TestUtils.createFile(src, "1-dir1/1-dir1[Dir]prm/6-dir6|prm/2-item2.txt", "Item1.2");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertEquals("Item1Item2Item3Item4", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
    }

    @Test
    public void testPromoteNav() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "1-dir1|nav/2-item2.txt", "Item2");
        TestUtils.createFile(src, "1-dir1|nav/3-item3.txt", "Item3");
        TestUtils.createFile(src, "1-dir1|nav/4-item4.txt", "Item4");
        TestUtils.createFile(src, "1-dir1|nav/1-dir1[Dir]prm/6-dir6|prm/1-item1|prm.txt", "Item1");
        TestUtils.createFile(src, "1-dir1|nav/1-dir1[Dir]prm/6-dir6|prm/2-item2|prm.txt", "Item1.2");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertEquals("Item1Item1.2Item2Item3Item4", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
    }

    @Test
    public void testPromoteOne1() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "2-item2.txt", "Item2");
        TestUtils.createFile(src, "3-item3.txt", "Item3");
        TestUtils.createFile(src, "4-item4.txt", "Item4");
        TestUtils.createFile(src, "1-dir1[Dir]prm/1-item1.txt", "Item1a");
        TestUtils.createFile(src, "1-dir1[Dir]prm/2-item1.txt", "Item1b");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertEquals("Item1aItem1bItem2Item3Item4", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("Item1aItem1b", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
    }

    @Test
    public void testPromoteOne2() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "2-item2.txt", "Item2");
        TestUtils.createFile(src, "3-item3.txt", "Item3");
        TestUtils.createFile(src, "4-item4.txt", "Item4");
        TestUtils.createFile(src, "1-dir1[Dir]prm1/1-item1.txt", "Item1a");
        TestUtils.createFile(src, "1-dir1[Dir]prm1/2-item1.txt", "Item1b");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertEquals("Item1aItem2Item3Item4", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("Item1aItem1b", FileUtils.readFileToString(gen.resolve("dir1/index.html").toFile()));
    }

    @Test
    public void testPromoteMix() throws IOException, InterruptedException, ExecutionException {
        TestUtils.createFile(src, "2-item2.txt", "Item2");
        TestUtils.createFile(src, "3-item3.txt", "Item3");
        TestUtils.createFile(src, "4-item4.txt", "Item4");
        TestUtils.createFile(src, "1-dir1[Dir]prm/2-dir2|prm/1-item1|prm.txt", "Item1a");
        TestUtils.createFile(src, "1-dir1[Dir]prm/2-dir2|prm/2-item2.txt", "Item1aa");
        TestUtils.createFile(src, "1-dir1[Dir]/3-dir3/3-item3|prm.txt", "Item1b");
        TestUtils.createFile(src, "1-dir1[Dir]/3-dir3/4-item4.txt", "Item1bb");

        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString() , "-r", "-x");
        Assert.assertEquals("Item1aItem2Item3Item4", FileUtils.readFileToString(gen.resolve("index.html").toFile()));
        Assert.assertEquals("Item1aItem1aa", FileUtils.readFileToString(gen.resolve("dir1/dir2/index.html").toFile()));
    }
}
