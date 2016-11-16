package net.xdocc;

public class TestCache {
	
	/*private static final Logger log = LoggerFactory.getLogger(TestCache.class);
	
	private static final String genString = "/tmp/gen/example-cache";
	private static final String sourceString = "/example-cache";

	private static Site site;
	private static File mapCache;
	private static Service service;

	@BeforeClass
	public static void setup() throws IOException, InterruptedException,
			URISyntaxException {
		// setup source & generated folder
		URL resourceUrl = TestTags.class.getResource(sourceString);
		Path source = Paths.get(resourceUrl.toURI());
		Path generated = Paths.get(genString);
		Files.createDirectories(generated);

		// setup cache
		mapCache = new File("/tmp/testcache.mapdb");
		service = new Service();
		service.setupCache(mapCache);
		site = new Site(service, source, generated, service.findHandlers(),
				null);
		service.compile(site);
		Key<Path> crk = new Key<Path>(site.getSource(), site.getSource());
		service.waitFor(crk);
		log.info("testcompile done 1");
	}

	@AfterClass
	public static void cleanup() throws IOException {
		service.shutdown();
		mapCache.delete();
		if (Files.exists(Paths.get("/tmp/testcache.mapdb.p"))) {
			new File("/tmp/testcache.mapdb.p").delete();
		}
		if (Files.exists(Paths.get("/tmp/testcache.mapdb.t"))) {
			new File("/tmp/testcache.mapdb.t").delete();
		}
		FileUtils.deleteDirectory(new File(genString));
	}
	
	@Test
	public void testCache() throws Exception {
		Path index = site.getGenerated().resolve("index.html");
		long timestap = Files.getLastModifiedTime(index).toMillis();
		Thread.sleep(1000);
		service.compile(site);
		Key<Path> crk = new Key<Path>(site.getSource(), site.getSource());
		service.waitFor(crk);
		Thread.sleep(1000);
		log.info("testcompile done 2");
		long timestap2 = Files.getLastModifiedTime(index).toMillis();
		Assert.assertEquals(timestap, timestap2);
		Files.write(index, "3333".getBytes());
		Thread.sleep(1000);
		service.compile(site);
		service.waitFor(crk);
		Thread.sleep(1000);
		log.info("testcompile done 3");
		timestap2 = Files.getLastModifiedTime(index).toMillis();
		Assert.assertNotSame(timestap, timestap2);
	}*/
	
}
