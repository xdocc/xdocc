package net.xdocc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;

import net.xdocc.Site.TemplateBean;
import net.xdocc.filenotify.FileListener;
import net.xdocc.filenotify.WatchService;
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

	private static final ExecutorService executorServiceCompiler = Executors
			.newCachedThreadPool();

	private static final Set<Path> dirtySet = Collections
			.synchronizedSet(new HashSet<Path>());

	private static final Map<Path, CompileResult> compileResult = Collections
			.synchronizedMap(new HashMap<Path, CompileResult>());
	private static Map<File, Set<FileInfos>> cache;

	private static DB db;

	private static File configDir;

	private static File cacheDir;

	private static boolean fileChangeListener = true;
	
	private static boolean clearCache = false;
	
	private static int compilerCounter = 0;

	static {
		options.addOption("c", "config", true,
				"the folder that contains the configuration for the sites");
		options.addOption("r", "run-once", false,
				"runs only once and exits. If omited, application runs as a service");
		options.addOption("f", "file-change-listener", false,
				"run the file-change-listener to recompile if files changed");
		options.addOption("s", "cache", true,
				"set the path to persist the cached data");
		options.addOption("x", "clear-cache", false,
				"clear cache at startup");
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
		addShutdownHook();
		try {
			List<Site> sites = init(args);
			if (sites != null && fileChangeListener) {
				WatchService.startWatch(sites);
				compileIfFileChanged();
			}
		} catch (IOException e) {
			LOG.error("cannot compile sites " + e);
			e.printStackTrace();
		}
	}

	private static void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				shutdown();
			}
		});
	}

	private static void compileIfFileChanged() {
		WatchService.getFileNotifier().addListener(new FileListener() {
			@Override
			public void filesChanged(List<XPath> changedSet,
					List<XPath> createdSet, List<XPath> deletedSet) {
				List<Site> sites = new ArrayList<>();
				collectSites(sites, changedSet);
				collectSites(sites, createdSet);
				collectSites(sites, deletedSet);
				for (Site site : sites) {
					try {
						Service.compile(site);
					} catch (IOException e) {
						LOG.error("compiler exception: " + e);
					}
				}
			}

			private void collectSites(List<Site> sites, List<XPath> changedSet) {
				for (XPath xpath : changedSet) {
					if (!sites.contains(xpath.getSite())) {
						sites.add(xpath.getSite());
					}
				}
			}
		});
	}

	private static List<Site> init(String[] args) throws FileNotFoundException,
			IOException {
		LOG.info("Starting XDocC");

		try {
			initConfig(args);
			setupCache(cacheDir);
			LOG.info("configuration directory set to " + configDir);
			return initCompile();
		} catch (ParseException | IOException e) {
			LOG.error("cannot start xdocc ", e);
			if (LOG.isDebugEnabled()) {
				e.printStackTrace();
			}
			shutdown();
			return null;
		}
	}

	static void setupCache(File cacheDir) {	
		db = DBMaker.newFileDB(cacheDir).closeOnJvmShutdown().make();
		cache = db.getHashMap("results");
		if(clearCache) {
			cache.clear();
		}
	}

	private static List<Site> initCompile() throws FileNotFoundException,
			IOException {
		// now we read the config files for each site
		List<Handler> handlers = findHandlers();
		List<Site> sites = createSites(handlers);

		if (LOG.isDebugEnabled()) {
			LOG.debug("found " + handlers.size() + " handlers and "
					+ sites.size() + " sites. Going to compile the sites now");
		}

		for (Site site : sites) {
			Utils.createDirectory(site);
			compile(site);
		}
		return sites;
	}

	private static void initConfig(String[] args) throws ParseException,
			IOException {
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

	private static List<Site> createSites(List<Handler> handlers)
			throws FileNotFoundException, IOException {
		File[] configFiles = configDir.listFiles();
		List<Site> sites = new ArrayList<Site>();
		for (File configFile : configFiles) {
			Site site = readSite(configFile, handlers);
			if (!"true".equalsIgnoreCase(StringUtils.trim(site
					.getProperty("enabled")))) {
				continue;
			}
			sites.add(site);
			Link link = readNavigation(site);
			site.setNavigation(link);
		}
		return sites;
	}

	/**
	 * Initial call
	 * 
	 * @param site
	 * @throws IOException
	 */
	public static void compile(Site site) throws IOException {
		invalidateCache(site);
		compile(site, site.getSource(), new HashMap<String, Object>());
		compilerCounter++;
	}

	/**
	 * Call recursively
	 * 
	 * @param site
	 * @param path
	 * @throws IOException
	 */
	public static void compile(Site site, Path path, Map<String, Object> model)
			throws IOException {
		LOG.info("compiling: " + site + "/" + path);
		Link link = readNavigation(site);
		site.setNavigation(link);
		executorServiceCompiler.execute(new Compiler(site, path, dirtySet,
				model));
	}

	public static void compileDone(Site site) throws IOException {
		LOG.info("compiling done: " + site);
		// compileResult.clear();
		List<Path> children = Utils.getChildren(site, site.getGenerated());
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
				if (Utils.isChild(pathToDelete, site.getGenerated())) {
					Files.delete(pathToDelete);
					System.err.println("delete " + pathToDelete);
				}
			}
		}		
		if(compilerCounter == 1) {
			//some templates may be used within a template -> find those and get the file size and date
			Map<String, TemplateBean> templates = site.getTemplates();
			for (Map.Entry<String, TemplateBean> entry : templates.entrySet()) {
				if (entry.getValue().isDirty()) {
					String baseName = FilenameUtils.getBaseName(entry.getKey());
					site.getTemplate("", baseName, null);
				}
			}
		}
		if (!fileChangeListener) {
			// wait until everything is compiled and exit;
			shutdown();
		}
	}

	static Link readNavigation(Site site) throws IOException {
		return readNavigation(site, new XPath(site, site.getSource()));
	}

	public static Link readNavigation(Site site, XPath source)
			throws IOException {
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

	static void readNavigationRec(Site site, Link parent, Path parentPath)
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

	static List<Handler> findHandlers() {
		Reflections reflections = new Reflections("net.xdocc");
		Set<Class<? extends Handler>> subTypes = reflections
				.getSubTypesOf(Handler.class);
		final List<Handler> handlers = new ArrayList<>();
		for (Class<? extends Handler> clazz : subTypes) {
			try {
				if (!clazz.equals(HandlerCopy.class)) {
					handlers.add(clazz.newInstance());
				}
			} catch (InstantiationException | IllegalAccessException e) {
				LOG.error("failed to initialize handler " + clazz.toString()
						+ " - " + e);
			}
		}
		return handlers;
	}

	private static Site readSite(File configFile, List<Handler> handlers)
			throws FileNotFoundException, IOException {
		Properties properties = new Properties();
		try (FileReader fr = new FileReader(configFile)) {
			properties.load(fr);
			return new Site(properties.getProperty(PROPERTY_SOURCE),
					properties.getProperty(PROPERTY_GENERATED), handlers,
					properties);
		}
	}

	public static void addCompileResult(Path path, CompileResult result) {
		if(compileResult.containsKey(path)) {
			System.err.println("Path "+path+ " already there. Overwriting");
		}
		compileResult.put(path, result);
		if (cache != null && result.getFileInfos() != null) {
			cache.put(path.toFile(), result.getFileInfos());
			//System.err.println("add to cache "+path.toFile()+" / "+(result.getFileInfos()==null));
			db.commit();
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("compiled and stored " + path);
		}
	}

	public static boolean isCached(Path source, Path target) {
		if(cache == null) {
			return false;
		}
		Set<FileInfos> infos = cache.get(source.toFile());
		if (infos == null) {
			//System.err.println("not found in cache "+source.toFile());
			return false;
		}
		for (FileInfos info : infos) {
			if (info.getTarget().equals(target.toFile())) {
				try {
					// now we have all the files found
					if (info.isFiles(source)) {
						long sourceSize = Files.size(source);
						long targetSize = Files.size(target);
						long targetTimestamp = Files
								.getLastModifiedTime(target).toMillis();
						long sourceTimestamp = Files
								.getLastModifiedTime(source).toMillis();
						boolean isSourceDirty = info.isSourceDirty(
								sourceTimestamp, sourceSize);
						boolean isTargetDirty = info.isTargetDirty(target,
								targetTimestamp, targetSize);
						return !isSourceDirty && !isTargetDirty;
					} else if (info.isDirectories(source)) {
						//no need to check target, since it will be modified when a file changes inside.
						long sourceTimestamp = Files
								.getLastModifiedTime(source).toMillis();
						boolean isSourceDirty = info.isSourceDirty(
								sourceTimestamp);
						return !isSourceDirty;
					} else {
						return false;
					}
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
			}
		}
		return false;
	}

	public static void notifyFor() {
		synchronized (compileResult) {
			compileResult.notifyAll();
		}
	}

	public static CompileResult getCompileResult(Path path) {
		CompileResult compileResult1 = compileResult.get(path);
		if(compileResult1 != null) {
			return compileResult1.copyDocument();
		}
		return null;
	}

	public static void waitFor(Path path) throws InterruptedException {
		while (getCompileResult(path) == null) {
			synchronized (compileResult) {
				compileResult.wait();
			}
		}
	}

	private static void invalidateCache(Site site) throws IOException {
		Set<Path> dependencies = new HashSet<>();
		// template cache
		Map<String, TemplateBean> templates = site.getTemplates();
		for (Map.Entry<String, TemplateBean> entry : templates.entrySet()) {
			if (entry.getValue().isDirty()) {
				System.err.println("yes, we are dirty: "+entry.getKey());
				dependencies.addAll(entry.getValue().getFlatDependencies());
			}
		}

		// content cache
		synchronized (compileResult) {

			for (Iterator<Entry<Path, CompileResult>> iterater = compileResult
					.entrySet().iterator(); iterater.hasNext();) {
				Entry<Path, CompileResult> compileResult2 = iterater.next();
				Collection<FileInfos> list = compileResult2.getValue()
						.getFileInfos();
				if (list == null) {
					continue;
				}
				for (FileInfos fileInfos : list) {
					Path source = compileResult2.getKey();
					Path target = fileInfos.getTarget().toPath();

					// if the cache is not valid OR if the template changed
					if (!isCached(source, target)
							|| dependencies.contains(source)) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("cache: invalidate " + source
									+ " with target " + target);
						}
						/*if(target.toString().contains("/ana/icons/")) {
							System.err.println("cache: invalidate " + source
								+ " with target " + target);
							isCached(source, target);
						}*/
						dependencies.addAll(compileResult2.getValue()
								.findDependencies(source));
						iterater.remove();
						if(cache!=null) {
							cache.remove(source.toFile());
						}
						break;
					}
				}
			}
		}
		for (Path dependency : dependencies) {
			//System.err.println("removing dependency " + dependency);
			if(cache!=null) {
				cache.remove(dependency.toFile());
			}
			compileResult.remove(dependency);
		}
	}

	static void shutdown() {
		WatchService.shutdown();
		executorServiceCompiler.shutdown();
	}

}
