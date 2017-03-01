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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author draft
 */
public class TestTomP2P {
    private static Path gen;
    private static Path src;

    @Before
    public void setup() throws IOException {
        src = Paths.get("/home/draft/Downloads/tomp2p-min");
        gen = Files.createTempDirectory("gen");
        Files.createDirectories(src.resolve(".templates"));
    }

    @After
    public void tearDown() throws IOException {
        //TestUtils.deleteDirectories(gen);
    }
    
    @Test
        public void testCache() throws IOException, InterruptedException, ExecutionException {
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x");
    }
}
