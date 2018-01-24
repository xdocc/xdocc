/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.xdocc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Thomas Bocek
 */
public class TestRun {
    private static Path gen;
    private static Path src;
    private static Path cache;

    @Before
    public void setup() throws IOException {
        //src = Paths.get("/home/draft/Downloads/tomp2p-min");
        src = Paths.get("/home/draft/git/xdocc/src/site");
        gen = Files.createTempDirectory("gen");
        cache = Files.createTempDirectory("cache").resolve("cache");
        Files.createDirectories(src.resolve(".templates"));
    }

    @After
    public void tearDown() throws IOException {
        //TestUtils.deleteDirectories(gen);
    }
    
    @Test
    //@Ignore
    public void testRun() throws IOException, InterruptedException, ExecutionException {
        Service.main("-s", src.toString(), "-g", gen.toString(), "-c", cache.toString(), "-x");
        Thread.sleep(100000000);
    }
}
