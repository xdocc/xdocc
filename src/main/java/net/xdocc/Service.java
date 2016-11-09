package net.xdocc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogManager;

import net.xdocc.CompileResult.Key;
import net.xdocc.Site.TemplateBean;
import net.xdocc.filenotify.FileListener;
import net.xdocc.filenotify.RecursiveWatcherService;
import net.xdocc.handlers.Handler;
import net.xdocc.handlers.HandlerCopy;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Service {
	private static final String CONFIG_FOLDER = "/etc/xdocc";

	private static final String PROPERTY_SOURCE = "source";

	private static final String PROPERTY_GENERATED = "generated";

	private static final Options options = new Options();

	private static final Logger LOG = LoggerFactory.getLogger(Service.class);

	private final ExecutorService executorServiceCompiler = Executors
			.newCachedThreadPool();

	private final Set<Path> dirtySet = Collections
			.synchronizedSet(new HashSet<Path>());

	private final Map<Key<Path>, CompileResult> compileResult = Collections
			.synchronizedMap(new HashMap<Key<Path>, CompileResult>());
	

	private static Map<String, Set<FileInfos>> cache;

	private static Map<Site, Map<String, TemplateBean>> cacheTemplates;

	private static DB db;

	private static File configDir;

	private static File cacheDir;

	private static boolean fileChangeListener = true;

	private static boolean clearCache = false;

	private static int compilerCounter = 0;
        
        private final List<RecursiveWatcherService> watchServices = new ArrayList<>();

	static {
		options.addOption("c", "config", true,
				"the folder that contains the configuration for the sites");
		options.addOption("r", "run-once", false,
				"runs only once and exits. If omited, application runs as a service");
		options.addOption("f", "file-change-listener", false,
				"run the file-change-listener to recompile if files changed");
		options.addOption("s", "cache", true,
				"set the path to persist the cached data");
		options.addOption("x", "clear-cache", false, "clear cache at startup");
		// read config file from project if run in eclipse, otherwise use
		// directory
		try {
			InputStream is = Service.class.getResourceAsStream("/logback.xml");
			if (is == null)
				is = new FileInputStream(CONFIG_FOLDER + "/logback.xml");
			LogManager.getLogManager().readConfiguration(is);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) {
		final Service service = new Service();
		service.addShutdownHook();
		try {
			List<Site> sites = service.init(args);
			if (sites != null && fileChangeListener) {
                            FileListener fileListener = (Site site) -> {
                                try {
                                    service.compile(site);
                                } catch (IOException ex) {
                                   LOG.error("cannot compile sites ", ex);
                                }
                            };
                            for(Site site:sites) {
				service.startWatch(site, fileListener);
                            }
			}
			LOG.info("service ready!");
		} catch (IOException e) {
			LOG.error("cannot compile sites ", e);
			e.printStackTrace();
		}
	}
        
        private void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				shutdown();
			}
		});
	}

	public void startWatch(Site site, FileListener listener) throws IOException {
	    RecursiveWatcherService recursiveWatcherService = new RecursiveWatcherService(site, listener);
            watchServices.add(recursiveWatcherService);
        }
	


	

	

	private List<Site> init(String[] args) throws FileNotFoundException,
			IOException {
		LOG.info("Starting XDocC");

		try {
			initConfig(args);
			setupCache(cacheDir);
			LOG.info("configuration directory set to " + configDir);
			return initCompile();
		} catch (ParseException | IOException | InterruptedException e) {
			LOG.error("cannot start xdocc ", e);
			if (LOG.isDebugEnabled()) {
				e.printStackTrace();
			}
			shutdown();
			return null;
		}
	}

	void setupCache(File cacheDir) {
		db = DBMaker.newFileDB(cacheDir).closeOnJvmShutdown().make();
		cache = db.getHashMap("results");
		cacheTemplates = Collections
				.synchronizedMap(new HashMap<Site, Map<String, TemplateBean>>());
		if (clearCache) {
			cache.clear();
		}
	}

	private List<Site> initCompile() throws FileNotFoundException, IOException,
			InterruptedException {
		// now we read the config files for each site
		List<Handler> handlers = Utils.findHandlers();
		List<Site> sites = createSites(handlers);

		if (LOG.isDebugEnabled()) {
			LOG.debug("found " + handlers.size() + " handlers and "
					+ sites.size() + " sites. Going to compile the sites now");
		}

		for (Site site : sites) {
			Utils.createDirectory(site);
			compile(site);
			Key<Path> crk = new Key<Path>(site.source(), site.source());
			waitFor(crk);
			LOG.info("compiling done: " + site);
			db.commit();
		}

		return sites;
	}

	private void initConfig(String[] args) throws ParseException, IOException {
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse(options, args);
		String file = cmd.getOptionValue("c");
		if (file == null) {
			throw new IOException("Could not find config file");
		}
		fileChangeListener = cmd.hasOption("f");
		clearCache = cmd.hasOption("x");
		configDir = new File(file);
		String tmpCacheDir = cmd.getOptionValue("s");
		if (tmpCacheDir == null) {
			throw new IOException("No cache dir specified with -s");
		}
		cacheDir = new File(tmpCacheDir);
	}

	private List<Site> createSites(List<Handler> handlers)
			throws FileNotFoundException, IOException {
		File[] configFiles = configDir.listFiles();
		List<Site> sites = new ArrayList<Site>();
		for (File configFile : configFiles) {
			try {
				Site site = readSite(configFile, handlers);
				if (!"true".equalsIgnoreCase(StringUtils.trim(site
						.getProperty("enabled")))) {
					continue;
				}
				sites.add(site);
				Link link = readNavigation(site);
				site.navigation(link);
			} catch (Throwable t) {
				LOG.error(t.getMessage());
			}
		}
		return sites;
	}

	/**
	 * Initial call
	 * 
	 * @param site
	 * @throws IOException
	 */
	public void compile(Site site) throws IOException {
		invalidateCache(site);
		compile(site, site.source(), new HashMap<String, Object>());
		compilerCounter++;
	}

	/**
	 * Call recursively
	 * 
	 * @param site
	 * @param path
	 * @throws IOException
	 */
	public void compile(Site site, Path path, Map<String, Object> model)
			throws IOException {
		LOG.debug("compiling: " + site + "/" + path);
		// Link link = readNavigation(site);
		// site.setNavigation(link);
		executorServiceCompiler.execute(new Compiler(site, path, dirtySet,
				model));
	}

	public void compileDone(Site site) throws IOException {
		// compileResult.clear();
		List<Path> children = Utils.getChildren(site, site.generated());
		for (Iterator<Path> iterator = children.iterator(); iterator.hasNext();) {
			Path foundPath = iterator.next();
			if (dirtySet.contains(foundPath)) {
				iterator.remove();
			}
		}
		dirtySet.clear();
		// we can remove now all the ones in children
		// make sure we are in the right directory
		for (Path pathToDelete : children) {
			if (!Files.isDirectory(pathToDelete)) {
				if (Utils.isChild(pathToDelete, site.generated())) {
					Files.delete(pathToDelete);
					LOG.debug("delete " + pathToDelete);
				}
			}
		}
		if (compilerCounter == 1) {
			// some templates may be used within a template -> find those and
			// get the file size and date
			for (Map.Entry<String, TemplateBean> entry : cacheTemplates.get(
					site).entrySet()) {
				if (entry.getValue().isDirty()) {
					String baseName = FilenameUtils.getBaseName(entry.getKey());
					site.getTemplate(baseName, "");
				}
			}
		}
		if (!fileChangeListener) {
			// wait until everything is compiled and exit;
			shutdown();
		}
	}

	Link readNavigation(Site site) throws IOException {
		return readNavigation(site, new XPath(site, site.source()));
	}

	public Link readNavigation(Site site, XPath source) throws IOException {
		Link root = new Link(source, null);
		List<XPath> children = Utils.getNonHiddenChildren(site,
				source.getPath());
		final boolean ascending;
		if (source.isAutoSort()) {
			ascending = Utils.guessAutoSort(children);
		} else {
			ascending = source.isAscending();
		}
		Utils.sort2(children, ascending);
		for (XPath xPath : children) {
			if (xPath.isNavigation()) {
				Link link = new Link(xPath, root);
				root.addChildren(link);
				readNavigationRec(site, link, xPath.getPath());
			}
		}
		return root;
	}

	void readNavigationRec(Site site, Link parent, Path parentPath)
			throws IOException {
		List<XPath> children = Utils.getNonHiddenChildren(site, parentPath);

		final boolean ascending;
		if (parent.getTarget().isAutoSort()) {
			ascending = Utils.guessAutoSort(children);
		} else {
			ascending = parent.getTarget().isAscending();
		}
		Utils.sort2(children, ascending);
		for (XPath xPath : children) {
			if (xPath.isNavigation()) {
				Link link = new Link(xPath, parent);
				parent.addChildren(link);
				readNavigationRec(site, link, xPath.getPath());
			}
		}
	}

	

	private Site readSite(File configFile, List<Handler> handlers)
			throws Exception {
		Properties properties = new Properties();
		try (FileReader fr = new FileReader(configFile)) {
			properties.load(fr);
			if (properties.getProperty(PROPERTY_SOURCE) == null) {
                            throw new Exception("bad config file: " + PROPERTY_SOURCE 
                                    + "need to be specified in: " + configFile.getPath().toString());
			}
                        if (properties.getProperty(PROPERTY_GENERATED) == null) {
                            throw new Exception("bad config file: " + PROPERTY_GENERATED 
                                    + "need to be specified in: " + configFile.getPath().toString());
			}
                        Path source = Paths.get(properties.getProperty(PROPERTY_SOURCE));
                        Path generated = Paths.get(properties.getProperty(PROPERTY_GENERATED));
                        if(!Files.isRegularFile(source)) {
                            throw new Exception("bad config file: " 
                                    + properties.getProperty(PROPERTY_SOURCE) + " is not a regular file");
                        }
                        if(!Files.isRegularFile(generated)) {
                            throw new Exception("bad config file: " 
                                    + properties.getProperty(PROPERTY_GENERATED) + " is not a regular file");
                        }
			return new Site(this, source, generated, handlers, properties);
		}
	}

	public boolean isCached(Site site, Key<Path> key) {
		if (cache == null) {
			return false;
		}
		Set<FileInfos> infos = getFromCache(key);
		if (infos == null) {
			return false;
		}
		for (FileInfos info : infos) {
			try {
				if (info.isFiles(key.getSource())) {
					// now we have all the files found
					long sourceSize = Files.size(key.getSource());
					long targetSize = Files.size(info.getTarget().toPath());
					long targetTimestamp = Files.getLastModifiedTime(
							info.getTarget().toPath()).toMillis();
					long sourceTimestamp = Files.getLastModifiedTime(
							key.getSource()).toMillis();
					boolean isSourceDirty = info.isSourceDirty(sourceTimestamp,
							sourceSize);
					boolean isTargetDirty = info.isTargetDirty(info.getTarget()
							.toPath(), targetTimestamp, targetSize);
					return !isSourceDirty && !isTargetDirty;
				} else if (info.isDirectories(key.getSource())) {

					// no need to check target, since it will be modified
					// when a file changes inside

					long sourceTimestamp = Files.getLastModifiedTime(
							key.getSource()).toMillis();
					boolean isSourceDirty = info.isSourceDirty(sourceTimestamp);
					return !isSourceDirty;
				} else {
					return false;
				}
			} catch (IOException e) {
				LOG.info("exception in isCached - probably due to file removed event: "
						+ key);
				return false;
			}
		}
		if (!key.getSource().toString().equals(key.getTarget())) {
			return true;
		}
		return false;
	}

	public void notifyFor() {
		synchronized (compileResult) {
			compileResult.notifyAll();
		}
	}

	public void addCompileResult(Key<Path> key, CompileResult result) {
		LOG.info("");
		synchronized (compileResult) {
			LOG.info("adding CR " + key);
			compileResult.put(key, result);
		}
		if (result.getFileInfos() != null) {
			addToCache(key, result.getFileInfos());
		} else {
			LOG.info("no file infos: " + key + " added to CR but NOT IN CACHE!");
		}
	}

	public CompileResult getCompileResult(Key<Path> key) {
		synchronized (compileResult) {
			return compileResult.get(key);
		}
	}

	public void removeCompileResult(Key<Path> key) {
		synchronized (compileResult) {
			compileResult.remove(key);
			LOG.info("remove CR: " + key.getSource() + " " + key);
		}
	}

	public void addToCache(Key<Path> key, Set<FileInfos> infos) {
		synchronized (cache) {
			LOG.info("adding CACHE: " + key + " size FileInfos: "
					+ infos.size());
			cache.put(key.toString(), infos);
		}
	}

	public Set<FileInfos> getFromCache(Key<Path> key) {
		synchronized (cache) {
			return cache.get(key.toString());
		}
	}

	public void removeFromCache(Key<Path> key) {
		synchronized (cache) {
			cache.remove(key.toString());
			LOG.info("remove CACHE: " + key);
		}
	}

	public synchronized Map<String, TemplateBean> getTemplateBeans(Site site) {
		if (cacheTemplates == null) {
			return null;
		}
		return cacheTemplates.get(site);
	}

	public synchronized void addTemplateBeans(Site site,
			Map<String, TemplateBean> templates) {
		if (cacheTemplates != null) {
			cacheTemplates.put(site, templates);
		}
	}

	public void waitFor(Key<Path> key) throws InterruptedException {
		while (getCompileResult(key) == null) {
			synchronized (compileResult) {
				compileResult.wait();
			}
		}
	}

	private void invalidateCache(Site site) throws IOException {
		Set<Key<Path>> dependencies = new HashSet<Key<Path>>();

		// template cache
		for (Map.Entry<String, TemplateBean> entry : getTemplateBeans(site)
				.entrySet()) {
			if (entry.getValue().isDirty()) {
				LOG.debug("yes, we are dirty: " + entry.getKey());
				//dependencies.addAll(entry.getValue().getFlatDependencies());
			}
		}

		// content cache
		synchronized (compileResult) {
			for (Key<Path> key : compileResult.keySet()) {
				CompileResult cr = compileResult.get(key);
				if (!Utils.isChild(key.getSource(), site.source())) {
					continue;
				}
				Set<FileInfos> list = cr.getFileInfos();
				if (list == null) {
					continue;
				}

				// if the cache is not valid OR if the template changed
				if (!isCached(site, key) || dependencies.contains(key)) {
					dependencies.add(key);
					Set<Key<Path>> deps = cr.findDependencies(key);
					if (deps.size() > 0)
						LOG.info("dependencies for " + key + " :");
					for (Key<Path> kk : deps) {
						LOG.info(kk.toString());
					}
					dependencies.addAll(deps);
				}
			}
		}
		for (Key<Path> dependency : dependencies) {
			LOG.debug("removing dependency from cache " + dependency);
			removeFromCache(dependency);
			LOG.debug("removing key from CR " + dependency);
			removeCompileResult(dependency);
		}
                //empty navigation
		site.navigation(null);
	}

	public void shutdown() {
		for(RecursiveWatcherService recursiveWatcherService:watchServices) {
                    recursiveWatcherService.shutdown();
                }
		executorServiceCompiler.shutdown();
	}

	public static void setFileListener(boolean set) {
		fileChangeListener = set;
	}

	public synchronized void printAll() {

		synchronized (compileResult) {
			// CompileResult
			LOG.info("------CR---------");
			for (Key<Path> key : compileResult.keySet()) {
				CompileResult cr = getCompileResult(key);
				LOG.info(key.toString());
				for (FileInfos inf : cr.getFileInfos()) {
					LOG.info("- fileInfos " + inf.getTarget().toString());
					LOG.info(" -- sSize = " + inf.getSourceSize() + " sTime = "
							+ inf.getSourceTimestamp());
					LOG.info(" -- tSize = " + inf.getTargetSize() + " tTime = "
							+ inf.getTargetTimestamp());
				}
				LOG.info("");
			}
		}

		synchronized (cache) {
			// CompileResult
			LOG.info("------CACHE---------");
			for (String key : cache.keySet()) {
				LOG.info(key);
				Set<FileInfos> infos = cache.get(key);
				for (FileInfos inf : infos) {
					LOG.info("- fileInfo: " + inf.getTarget().toString());
					LOG.info(" -- sSize = " + inf.getSourceSize() + " sTime = "
							+ inf.getSourceTimestamp());
					LOG.info(" -- tSize = " + inf.getTargetSize() + " tTime = "
							+ inf.getTargetTimestamp());
				}
				LOG.info("");
			}
		}
	}

	
}
