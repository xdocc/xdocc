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
