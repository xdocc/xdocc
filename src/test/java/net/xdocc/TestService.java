package net.xdocc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestService {
    
    private static Path gen;
    private static Path src;

    @BeforeClass
    public static void setup() throws IOException {
        src = Files.createTempDirectory("src");
        gen = Files.createTempDirectory("gen");
        Files.createDirectories(src.resolve(".templates"));
    }

    @AfterClass
    public static void tearDown() throws IOException {
        //Utils.deleteDirectories(gen, src);
    }
    
    @Test
    public void testStart() throws IOException, InterruptedException, ExecutionException {
        
        Path log = Files.createTempFile("xdocc",".log");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x", "-l", log.toString());
    }
    
    @Test
    public void testTxt() throws IOException, InterruptedException, ExecutionException {
        Utils.createFile(src, "1-test.txt", "this is a text file");
        Utils.createFile(src, ".templates/text.ftl", "This is a text file \n\n -- available files: ${all}");
        Path log = Files.createTempFile("xdocc",".log");
        Service.main("-w", src.toString(), "-o", gen.toString(), "-r", "-x", "-l", log.toString());
    }

	/*private static final Logger LOG = LoggerFactory
			.getLogger(TestService.class);

	private static final String configDir = "/example-main/config";
	private static final String configFile = "/example-main/config/testsite.properties";
	private static final String copyPath = "/example-main/testsite";

	private static String[] args;

	private static Properties props;

	@BeforeClass
	public static void setup() throws IOException, InterruptedException,
			URISyntaxException {
		// setup source & generated folder
		URL resConfDir = TestService.class.getResource(configDir);
		URL resConfFile = TestService.class.getResource(configFile);
		URL resCopyPath = TestService.class.getResource(copyPath);

		props = new Properties();
		props.load(new BufferedReader(new InputStreamReader(resConfFile
				.openStream())));

		Path s = Paths.get(resCopyPath.toURI());
		Path t = Paths.get(props.getProperty("source"));
		
		if(Files.exists(t)) {
			FileUtils.deleteDirectory(t.toFile());
		}	
		FileUtils.copyDirectory(s.toFile(), t.toFile());
		
		args = new String[4];
		args[0] = "-c";
		args[1] = Paths.get(resConfDir.toURI()).toString();
		args[2] = "-s";
		args[3] = "/tmp/testcache";
	}
	
	@Test
	public void test() throws InterruptedException, IOException, CmdLineException {
		Service.main(args);
		Thread.sleep(5000);
		Assert.assertTrue(Files.exists(Paths.get(props.getProperty("generated")+"/index.html")));
	}
	
	@AfterClass
	public static void cleanup() throws IOException {
		if (Files.exists(Paths.get("/tmp/testcache.p"))) {
			FileUtils.deleteQuietly(new File("/tmp/testcache.p"));
		}
		if (Files.exists(Paths.get("/tmp/testcache.t"))) {
			FileUtils.deleteQuietly(new File("/tmp/testcache.t"));
		}
		if (Files.exists(Paths.get("/tmp/testcache"))) {
			FileUtils.deleteQuietly(new File("/tmp/testcache"));
		}
		Path t = Paths.get(props.getProperty("source"));
		Path g = Paths.get(props.getProperty("generated"));
		FileUtils.deleteDirectory(t.toFile());
		FileUtils.deleteDirectory(g.toFile());
	}*/

}
