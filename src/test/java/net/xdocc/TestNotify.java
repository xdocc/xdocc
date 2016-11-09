package net.xdocc;

import net.xdocc.FileListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import net.xdocc.Service;
import net.xdocc.Site;
import net.xdocc.Utils;
import net.xdocc.XPath;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestNotify {

    private static CountDownLatch latch = new CountDownLatch(1);
    private static AtomicInteger counter = new AtomicInteger(0);
    private static Site site;

    private static Service service;
    private static Path dir;

    @BeforeClass
    public static void setup() throws Exception {
        dir = Files.createTempDirectory("xdocc");
        Path source = Files.createTempDirectory(dir, "source");
        Files.createDirectories(source.resolve(".templates"));
        Path generated = Files.createTempDirectory(dir, "generated");
        service = new Service();
        site = new Site(service, source, generated);
    }

    @AfterClass
    public static void tearDown() throws IOException {
        service.shutdown();
        Utils.deleteDirectory(dir);
    }

    private void init(int nr) throws IOException {
        counter = new AtomicInteger(0);
        latch = new CountDownLatch(nr);
        service.startWatch(site, new FileListener() {
            @Override
            public void filesChanged(Site site) {
                counter.incrementAndGet();
                latch.countDown();
            }
        });
    }

    @Test
    public void testChange() throws IOException, InterruptedException {
        init(1);
        Path source = site.source();
        // create two dirs
        Files.createTempDirectory(source, "dir1");
        latch.await(5, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch.getCount());
        Assert.assertEquals(1, counter.get());
        service.shutdown();
    }

    @Test
    public void testChangeOnce() throws IOException, InterruptedException {
        init(1);
        Path source = site.source();
        // create two dirs
        Files.createTempDirectory(source, "dir1");
        Thread.sleep(100);
        Files.createTempDirectory(source, "dir2");
        latch.await(5, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch.getCount());
        Assert.assertEquals(1, counter.get());
        service.shutdown();
    }

    @Test
    public void testChangeTwice() throws IOException, InterruptedException {
        init(2);
        Path source = site.source();
        // create two dirs
        Files.createTempDirectory(source, "dir1");
        Thread.sleep(2000);
        Files.createTempDirectory(source, "dir2");
        latch.await(5, TimeUnit.SECONDS);
        Assert.assertEquals(0, latch.getCount());
        Assert.assertEquals(2, counter.get());
        service.shutdown();
    }
}
