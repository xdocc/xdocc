package net.xdocc;

/**
 * Test needs to be run 2x to pass cache
 * 
 */
public class TestCompiler {

	/*private static final String genString = "/tmp/gen/example";
	private static final String sourceString = "/example|si=50x50|sn=500x500";

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
	public void testCompilerSingle() throws Exception {
		Path index = site.getGenerated().resolve(
				"folder0/folder01/folder011/index.html");
		Assert.assertEquals(true, Files.exists(index));
	}

	@Test
	public void testCompilerCollection() throws Exception {
		Path index = site.getGenerated().resolve("index.html");
		Assert.assertEquals(true, Files.exists(index));
	}

	@Test
	public void testCompilerNavigation() throws Exception {
		Path index = site.getGenerated().resolve("index.html");
		Assert.assertEquals(true, Files.exists(index));
		index = site.getGenerated().resolve("folder0/index.html");
		Assert.assertEquals(true, Files.exists(index));
		System.out.println(site.getNavigation());
	}*/

}
