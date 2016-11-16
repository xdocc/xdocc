package net.xdocc;


public class TestLinkUrl {

	/*private static final Logger log = LoggerFactory.getLogger(TestLinkUrl.class);

	private static final String genString = "/tmp/gen/example-linkurl";
	private static final String sourceString = "/example-linkurl";
	
	private static Site site;
	private static File mapCache;

	private static Service service;

	@BeforeClass
	public static void setup() throws IOException, InterruptedException, URISyntaxException {
		// setup source & generated folder
		URL resourceUrl = TestLinkUrl.class.getResource(sourceString);
		Path source = Paths.get(resourceUrl.toURI());
		Path generated = Paths.get(genString);
		Files.createDirectories(generated);

		// setup cache
		mapCache = new File("/tmp/testcache.mapdb");
		service = new Service();
		Service.setFileListener(true);
		service.setupCache(mapCache);
		site = new Site(service, source, generated, service.findHandlers(),
				null);
		service.compile(site);
		Key<Path> crk = new Key<Path>(site.getSource(), site.getSource());
		service.waitFor(crk);
	}
	
	@AfterClass
	public static void cleanup() throws IOException {
		service.shutdown();
		mapCache.delete();
		if(Files.exists(Paths.get("/tmp/testcache.mapdb.p"))) {
			new File("/tmp/testcache.mapdb.p").delete();
		}
		if(Files.exists(Paths.get("/tmp/testcache.mapdb.t"))) {
			new File("/tmp/testcache.mapdb.t").delete();
		}
		FileUtils.deleteDirectory(new File(genString));
	}

	@Test
	public void testLink() throws InterruptedException, IOException {
		Path p = site.getGenerated().resolve("index.html");
		String s = FileUtils.readFileToString(p.toFile());
		Assert.assertTrue(s.contains("\"download/read.me\""));
		//
		p = site.getGenerated().resolve("mylink.html");
		s = FileUtils.readFileToString(p.toFile());
		Assert.assertTrue(s.contains("\"download/read.me\""));
		//
		p = site.getGenerated().resolve("news/index.html");
		s = FileUtils.readFileToString(p.toFile());
		Assert.assertTrue(s.contains("\"../download/read.me\""));
		//
		p = site.getGenerated().resolve("news/test5.html");
		s = FileUtils.readFileToString(p.toFile());
		Assert.assertTrue(s.contains("\"../download/read.me\""));
	}*/
	
}
