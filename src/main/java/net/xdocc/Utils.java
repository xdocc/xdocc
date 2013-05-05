package net.xdocc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import net.xdocc.Site.TemplateBean;
import net.xdocc.handlers.HandlerUtils;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.NullCacheStorage;
import freemarker.template.TemplateException;

public class Utils {
	public static XPath find(Path resolved, List<Site> sites) {
		for (Site site : sites) {
			if (isChild(resolved, site.getSource())) {
				return new XPath(site, resolved);
			}
		}
		return null;
	}

	public static boolean isChild(Path maybeChild, Path possibleParent) {
		URI parentURI = possibleParent.toUri(), childURI = maybeChild.toUri();
		return !parentURI.relativize(childURI).isAbsolute();
	}

	/**
	 * Creates a relative path to the source. If the path is not a chiled of the
	 * source, then null is returned. If source is /tmp/ and path is /tmp/test,
	 * then the relative path to root is ../
	 * 
	 * @param root
	 *            The root
	 * @param path
	 *            The path
	 * @return The relative path to root
	 */

	public static String relativePathToRoot(Path root, Path path) {
		return relativePathToRoot(root, path, false);
	}

	public static String relativePathToRoot(Path root, Path path,
			boolean includeSelf) {
		if (!isChild(path, root)) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		Path parent;
		if (Files.isDirectory(path) && !includeSelf) {
			parent = path;
		} else {
			parent = path.getParent();
		}
		while (parent != null && !parent.equals(root)) {
			sb.append("../");
			parent = parent.getParent();
		}
		return sb.toString();
	}

	public static void deleteDirectory(Path dir) throws IOException {
		Files.walkFileTree(dir, new FileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
					throws IOException {
				if (exc == null) {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				} else {
					throw exc;
				}
			}

			@Override
			public FileVisitResult preVisitDirectory(Path dir,
					BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc)
					throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public static String[] createURLSplit(Path source, XPath xPath) {
		List<String> paths = new ArrayList<>();
		while (!source.equals(xPath.getPath())) {
			paths.add(0, xPath.getUrl());
			xPath = new XPath(xPath.getSite(), xPath.getPath().getParent());
		}
		return paths.toArray(new String[0]);
	}

	public static String createURL(String[] paths) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < paths.length; i++) {
			if (i > 0) {
				sb.append("/");
			}
			sb.append(paths[i]);
		}
		return sb.toString();
	}

	public static List<XPath> getNonHiddenChildren(final Site site,
			final Path siteToCompile) throws IOException {
		final List<XPath> result = new ArrayList<>();
		Files.walkFileTree(siteToCompile,
				EnumSet.noneOf(FileVisitOption.class), 1,
				new FileVisitor<Path>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir,
							BasicFileAttributes attrs) throws IOException {
						// do not include ourself
						if (!siteToCompile.equals(dir)) {
							XPath xPath = new XPath(site, dir);
							if (!xPath.isHidden()) {
								result.add(xPath);
							}
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file,
							BasicFileAttributes attrs) throws IOException {
						// do not include ourself
						if (!siteToCompile.equals(file)) {
							XPath xPath = new XPath(site, file);
							if (!xPath.isHidden()) {
								result.add(xPath);
							}
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file,
							IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir,
							IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}
				});
		return result;
	}

	public static List<Path> getChildren(final Site site,
			final Path siteToCompile) throws IOException {
		final List<Path> result = new ArrayList<>();
		Files.walkFileTree(siteToCompile,
				EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE,
				new FileVisitor<Path>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir,
							BasicFileAttributes attrs) throws IOException {
						// do not include ourself
						if (!siteToCompile.equals(dir)) {
							result.add(dir);
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file,
							BasicFileAttributes attrs) throws IOException {
						// do not include ourself
						if (!siteToCompile.equals(file)) {
							result.add(file);
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file,
							IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir,
							IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}
				});
		return result;
	}

	public static List<String> splitExtensions(String extensions) {
		List<String> retVal = new ArrayList<>();
		StringTokenizer st = new StringTokenizer(extensions, ".");
		while (st.hasMoreTokens()) {
			String extension = st.nextToken();
			if (!extension.equals("")) {
				retVal.add(extension);
			}
		}
		return retVal;
	}

	/**
	 * Search for the highlighted document. May return null if no document was
	 * found. If non document is tagged, the first one will be used
	 * 
	 * @param documents
	 *            The list of document found in the folder
	 * @return The highlighted document or null.
	 */
	public static Document searchHighlight(List<Document> documents) {
		for (Document document : documents) {
			if (document.isHighlight()) {
				return document;
			}
		}
		if (documents.size() > 0) {
			return documents.get(0);
		}
		return null;
	}

	/**
	 * Sort the documents according to its number. If a date was provided, the
	 * date will be converted to a long. If no number is provided the sort will
	 * be by name.
	 * 
	 * @param documents
	 *            The documents to sort
	 * @param inverted
	 *            A flag to invert the sort order
	 */
	public static void sort2(List<XPath> children, final boolean inverted) {
		Collections.sort(children, new Comparator<XPath>() {
			@Override
			public int compare(XPath o1, XPath o2) {
				int compare = o1.compareTo(o2);
				return inverted ? compare : compare * -1;
			}
		});

	}

	public static void write(String html, XPath xPath, Path generatedFile)
			throws TemplateException, IOException {
		// not in use yet
		Map<Path, Path> created = new HashMap<Path, Path>();

		Path alreadyGeneratedSource = created.get(generatedFile);
		if (alreadyGeneratedSource == null) {
			created.put(generatedFile, xPath.getPath());
		} else {
			if (alreadyGeneratedSource.equals(xPath.getPath())) {
				throw new IOException("create " + generatedFile
						+ ", but it was already created by "
						+ alreadyGeneratedSource + ". Anyway we will overwrite");
			}
		}

		try (FileWriter fw = new FileWriter(generatedFile.toFile())) {
			fw.write(html);
		}
	}

	public static Object lock = new Object();

	public static String applyTemplate(Site site, TemplateBean templateText,
			Map<String, Object> model) throws TemplateException, IOException {
		StringWriter sw = new StringWriter();
		synchronized (lock) {

			templateText
					.getTemplate()
					.getConfiguration()
					.setDirectoryForTemplateLoading(
							site.getTemplatePath().toFile());
			templateText.getTemplate().getConfiguration()
					.setCacheStorage(new NullCacheStorage());
			templateText
					.getTemplate()
					.getConfiguration()
					.setTemplateLoader(
							new Custom2FileTemplateLoader(site
									.getTemplatePath().toFile(), site,
									templateText));
			templateText.getTemplate().process(model, sw);

			sw.flush();
			return sw.getBuffer().toString();
		}

	}

	final static class Custom2FileTemplateLoader extends FileTemplateLoader {

		final private TemplateBean parentTemplateBean;
		final private Site site;

		public Custom2FileTemplateLoader(File baseDir, Site site,
				TemplateBean templateBean) throws IOException {
			super(baseDir);
			this.site = site;
			this.parentTemplateBean = templateBean;
		}

		@Override
		public Object findTemplateSource(String name) throws IOException {

			File source = (File) super.findTemplateSource(name);
			if (source == null) {
				return null;
			}

			TemplateBean templateBean = site.getTemplates().get(name);
			//FileTime fileTime = Files.getLastModifiedTime(templateBean
			//		.getFile());
			//long filesize = Files.size(templateBean.getFile());
			//templateBean.setTimestamp(fileTime.toMillis());
			//templateBean.setFilesize(filesize);

			//
			templateBean.addDependencies(parentTemplateBean);

			return source;
		}

	}

	public static List<Document> filter(List<Document> documents, int level) {
		List<Document> retVal = new ArrayList<>();
		List<Document> toPreview = new ArrayList<>();
		for (Document document : documents) {
			if (document.getLevel() < level) {
				retVal.add(document);
			} else if (document.getLevel() == level) {
				toPreview.add(document);
			}
		}
		if (toPreview.size() > 0) {
			Document document = searchHighlight(toPreview);
			retVal.add(document);
		}
		return retVal;
	}

	public static void createFile(Path source, String path, String content)
			throws IOException {
		Path file = source.resolve(path);
		Files.createDirectories(file.getParent());
		Files.createFile(file);
		Files.write(file, content.getBytes());
	}

	public static Link find(XPath xPath, Link navigation) {
		if (navigation.getTarget().getPath().equals(xPath.getPath())) {
			return navigation;
		}
		for (Link link : navigation.getChildren()) {
			Link found = findRec(xPath, link);
			if (found != null) {
				return found;
			}
		}
		return navigation;
	}

	public static Link findRec(XPath xPath, Link navigation) {
		if (navigation.getTarget().getPath().equals(xPath.getPath())) {
			return navigation;
		}
		for (Link link : navigation.getChildren()) {
			Link found = find(xPath, link);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	public static void createDirectory(Site site) throws IOException {
		Files.createDirectories(site.getGenerated());
	}

	public static String searchXPath(Path parent, String xPath)
			throws IOException {
		if(!Files.exists(parent)) {
			return null;
		}
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(parent)) {
			for (Path p : ds) {
				if (Files.isDirectory(p)) {
					if (p.getFileName().toString().indexOf("|" + xPath) > 0) {
						return p.toString();
					} else if (p.getFileName().toString().indexOf(xPath) == 0) {
						return p.toString();
					}
				}
			}

		}
		return null;
	}

	public static String searchPropertySizeIcon(XPath xPath, Site site) {
		XPath current = xPath;
		do {
			String size = current.getSizeIcon();
			if (size != null) {
				return size;
			}
		} while ((current = current.getParent()) != null);

		String size = site.getProperty("size_icon");
		if (size != null) {
			return size;
		}
		size = site.getProperty("si");
		if (size != null) {
			return size;
		}
		return "250x250^c";
	}

	public static String searchPropertySizeNormal(XPath xPath, Site site) {
		XPath current = xPath;
		do {
			String size = current.getSizeNormal();
			if (size != null) {
				return size;
			}
		} while ((current = current.getParent()) != null);

		String size = site.getProperty("size_normal");
		if (size != null) {
			return size;
		}
		size = site.getProperty("sn");
		if (size != null) {
			return size;
		}
		return "800x600^";
	}

	/*
	 * public static XPath findXPathFromURL(String url, Site site) throws
	 * IOException { XPath current = new XPath(site, site.getSource()); return
	 * findXPathFromURL( url, site, current ); } public static XPath
	 * findXPathFromURL(String url, Site site, XPath current) throws IOException
	 * { RelativeURL rUrl = RelativeURL.create(url);
	 * 
	 * current = Utils.findChildURL(site, current, rUrl.getCurrent()); for(;;) {
	 * if(current.getUrl().equals(rUrl.getCurrent())) { if(rUrl.getChild() ==
	 * null && current.getExtensions().equals(rUrl.getExtensions())) { return
	 * current; } rUrl = rUrl.getChild(); current = Utils.findChildURL(site,
	 * current, rUrl.getCurrent()); if(current == null) { return null; } } else
	 * { return null; } } }
	 */

	public static List<XPath> findChildURL(Site site, XPath current,
			String url, String extension) throws IOException {
		List<XPath> children = Utils.getNonHiddenChildren(site,
				current.getPath());
		List<XPath> result = new ArrayList<>();
		for (XPath child : children) {
			if (child.getUrl().equals(url)) {
				if (extension == null
						|| extension.equals(child.getExtensions())) {
					result.add(child);
				}
			}
		}
		return result;
	}

	public static List<XPath> findURL(Site site, XPath current, String url)
			throws IOException {
		String[] parsedURL = url.split("/");
		List<XPath> founds = new ArrayList<>();
		founds.add(current.getParent());
		for (int i = 0; i < parsedURL.length; i++) {
			String pURL = parsedURL[i];
			List<XPath> result = new ArrayList<>();
			for (Iterator<XPath> iterator = founds.iterator(); iterator
					.hasNext();) {
				XPath found = iterator.next();
				result.addAll(Utils.findChildURL(site, found, pURL, null));
			}
			founds = result;
		}
		return founds;
	}

	public static class RelativeURL {
		final private String current;
		final private RelativeURL parent;
		final private String extension;
		private RelativeURL child;

		private RelativeURL(String current, RelativeURL parent, String extension) {
			this.current = current;
			this.parent = parent;
			this.extension = extension;
		}

		public String getExtensions() {
			return extension;
		}

		public static RelativeURL create(String url) {
			StringTokenizer st = new StringTokenizer(url, "/");
			RelativeURL result = null;
			String extension = null;
			while (st.hasMoreTokens()) {
				String next = st.nextToken().trim();
				if (!st.hasMoreTokens()) {
					// is last?
					int index = next.indexOf(".");
					if (index >= 0) {
						extension = next.substring(index);
						next = next.substring(0, index);
					}
				}
				if (result == null) {
					result = new RelativeURL(next, null, extension);
				} else {
					RelativeURL result2 = new RelativeURL(next, result,
							extension);
					result.setChild(result2);
					result = result2;
				}
			}

			// get root
			while (result != null && result.getParent() != null) {
				result = result.getParent();
			}
			return result;
		}

		public String getCurrent() {
			return current;
		}

		public RelativeURL getParent() {
			return parent;
		}

		public RelativeURL getChild() {
			return child;
		}

		public void setChild(RelativeURL child) {
			this.child = child;
		}
	}

	public static CompileResult subList(CompileResult compileResult, int limit) {
		Document document = compileResult.getDocument();
		if (document == null) {
			return compileResult;
		}
		@SuppressWarnings("unchecked")
		List<Document> documents = (List<Document>) document
				.getDocumentGenerator().getModel().get("documents");
		if (documents == null) {
			return compileResult;
		}
		documents = documents.subList(0,
				limit > documents.size() ? documents.size() : limit);
		Document copy = document.copy();
		copy.getDocumentGenerator().getModel().put("documents", documents);
		Set<FileInfos> fileInfos = compileResult.getFileInfos();

		return new CompileResult(copy, fileInfos);
	}

	/**
	 * Returns a list of links that goes to the root. This is typically used for
	 * breadcrumbs.
	 * 
	 * @param current
	 *            The current location
	 * @return the list of links
	 */
	public static List<Link> linkToRoot(Path root, XPath xPath) {

		List<XPath> xPaths = new ArrayList<>();
		while (!root.equals(xPath.getPath())) {
			xPaths.add(0, xPath);
			xPath = new XPath(xPath.getSite(), xPath.getPath().getParent());
		}

		List<Link> retVal = new ArrayList<>();
		Link old = null;
		for (XPath xPath2 : xPaths) {
			Link link = new Link(xPath2, old);
			retVal.add(link);
			old = link;
		}
		return retVal;
	}

	public static Link setSelected(List<Link> pathToRoot, Link root) {
		Link copy = root.copy();
		List<Link> pathToRootCopy = new ArrayList<>();
		pathToRootCopy.addAll(pathToRoot);
		setSelectedRec(pathToRootCopy, copy);
		return copy;
	}

	public static void setSelectedRec(List<Link> pathToRoot, Link root) {
		if (pathToRoot.size() == 0) {
			return;
		}

		Link current = pathToRoot.remove(0);
		for (Link children : root.getChildren()) {
			if (children.equals(current)) {
				children.setSelected(true);
			}
			setSelectedRec(pathToRoot, children);
		}
	}

	public static String postApplyTemplate(String html,
			Map<String, Object> model, String... string) {
		for (String key : string) {
			if (!model.containsKey(key)) {
				continue;
			}
			// TODO: regexp would be better
			html = html.replace("${" + key + "}", model.get(key).toString());
		}
		return html;
	}

	/**
	 * The automatic sort detection does detect if its sorting with number up to
	 * 1000 (e.g. 1,2,3), which is sorted ascending. If its a date, the number
	 * is much higher and the sorting is descending (newest first). If there is
	 * a mix of number (e.g., 1,3,234534547545) it will be sorted descending.
	 * 
	 * @param children
	 *            The files in that directory. Only visible files are
	 *            considered.
	 * @return True if all numbers of visible files are smaller or equal than
	 *         1000, false otherwise.
	 */
	public static boolean guessAutoSort(List<XPath> children) {
		for (XPath xpath : children) {
			if (xpath.getNr() > 1000) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Counts all visible documents in a list. The visible documents start with
	 * either a number or a date -> 1|bla... or 2013-01-01|bla...
	 * 
	 * @param children
	 *            The list of all files to consider
	 * @return The number of visible elements.
	 */
	public static int countVisibleDocmuntes(List<XPath> children) {
		int counter = 0;
		for (XPath xpath : children) {
			if (xpath.isVisible()) {
				counter++;
			}
		}
		return counter;
	}

	public static void copyModelValues(Map<String, Object> modelDestination,
			Map<String, Object> modelSource, String... keys) {
		for (String key : keys) {
			modelDestination.put(key, modelSource.get(key));
		}
	}

	public static Document createDocument(Site site, XPath xPath,
			String relativePathToRoot, String htmlContent, String template)
			throws IOException {
		Map<String, Object> model;
		String documentName = xPath.getName();
		String documentURL = xPath.getTargetURL() + ".html";
		Date documentDate = xPath.getDate();
		long documentNr = xPath.getNr();
		String documentFilename = xPath.getFileName();
		// apply text ftl
		model = new HashMap<>();
		HandlerUtils.fillModel(documentName, documentURL, documentDate,
				documentNr, documentFilename, htmlContent, model);
		TemplateBean templateText = site.getTemplate(xPath.getLayoutSuffix(),
				template, xPath.getPath());
		// create the document
		Document doc = new Document(xPath, documentName, documentURL,
				documentDate, documentNr, documentFilename,
				xPath.isHighlight(), relativePathToRoot, new DocumentGenerator(
						site, templateText, model));
		return doc;
	}

	public static void writeHTML(Site site, XPath xPath, Set<Path> dirtyset,
			String relativePathToRoot, Document doc, Path generatedFile)
			throws IOException, TemplateException {
		TemplateBean templateSite = site.getTemplate(xPath.getLayoutSuffix(),
				"document", xPath.getPath());
		// create the site
		Map<String, Object> modelSite = new HashMap<>();
		// root.put("navigation", site.getNavigation());
		modelSite.put("path", relativePathToRoot);
		modelSite.put("document", doc);
		Link current = Utils.find(xPath.getParent(), site.getNavigation());
		List<Link> pathToRoot = Utils.linkToRoot(site.getSource(),
				xPath.getParent());
		modelSite.put("current", current);
		modelSite.put("breadcrumb", pathToRoot);
		modelSite.put("navigation", site.getNavigation());
		String htmlSite = Utils.applyTemplate(site, templateSite, modelSite);

		dirtyset.add(generatedFile);
		Path generatedDir = Files.createDirectories(generatedFile.getParent());
		dirtyset.add(generatedDir);
		Utils.write(htmlSite, xPath, generatedFile);
	}

}
