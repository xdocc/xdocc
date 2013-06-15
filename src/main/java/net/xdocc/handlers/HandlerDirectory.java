package net.xdocc.handlers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.xdocc.CompileResult;
import net.xdocc.Document;
import net.xdocc.DocumentGenerator;
import net.xdocc.Link;
import net.xdocc.Service;
import net.xdocc.Site;
import net.xdocc.Site.TemplateBean;
import net.xdocc.Utils;
import net.xdocc.XPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlerDirectory implements Handler {

	private static final Logger LOG = LoggerFactory
			.getLogger(HandlerDirectory.class);

	@Override
	public boolean canHandle(Site site, XPath xPath) {
		// only deal with directories
		if (!xPath.isDirectory()) {
			return false;
		} else if (xPath.isCopyAll()) {
			return true;
		}
		// do not handle if not visible or hidden. Exception: if its the root
		// directory, do it anyway
		if (!xPath.isCompile()) {
			return false;
		}
		// in all other cases we have a content directory, either a full content
		// directory or a preview directory
		return true;
	}

	@Override
	public CompileResult compile(Site site, XPath xPath, Set<Path> dirtyset,
			Map<String, Object> previousModel, String relativePathToRoot)
			throws Exception {
		Map<String, Object> model = new HashMap<>(previousModel);
		List<Document> documents = recursiveHandler(site, xPath);
		// Utils.adjustUrls(xPath, documents, path);

		model.put("path", relativePathToRoot);
		if (!xPath.isRoot()) {
			Link current = Service.readNavigation(site, xPath);
			model.put("local_navigation", current);
		}
		List<Link> pathToRoot = Utils.linkToRoot(site.getSource(), xPath);
		model.put("breadcrumb", pathToRoot);
		model.put("navigation",
				Utils.setSelected(pathToRoot, site.getNavigation()));
		String url = xPath.getTargetURL();
		model.put("url", url);
		if (documents.size() == 0) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("The directory [" + xPath + "] has no elements");
			}

			Path p = xPath.getTargetPath(url);
			Path generatedDir = Files.createDirectories(p);
			dirtyset.add(generatedDir);
			return CompileResult.DONE;
		}

		model.put("document_size", documents.size());
		// model.put("preview", xPath.isPreview());

		String target = xPath.resolveTargetURL("index.html");

		// change the path for the documents, if we find a highlighted document,
		// we want to point to the whole folder
		applyPath(documents, relativePathToRoot);

		int pageSize = xPath.getPageSize();
		int pages = pageSize == 0 ? 0 : documents.size() / (pageSize + 1);
		List<List<Document>> tmp = Utils.split(documents, pages, pageSize);
		String[] pageURLs = Utils.paging(xPath, pages);
		int counter = 0;

		TemplateBean template = site.getTemplate(xPath.getLayoutSuffix(), "page",
					xPath.getPath());
		
		// create the site
		final Document documentFull = createDocumentCollection(site, xPath,
				xPath, relativePathToRoot, documents, model, "collection",
				pageURLs, 0);
		model.put("document", documentFull);
		model.put("type", "directory");
		String html = Utils.applyTemplate(site, template, model);
		html = Utils.postApplyTemplate(html, model, "path");
		Path generatedFile = xPath.getTargetPath(target);
		dirtyset.add(generatedFile);
		Path generatedDir = Files.createDirectories(generatedFile.getParent());
		dirtyset.add(generatedDir);
		if (!Service.isCached(xPath.getPath(), generatedFile)) {
			Utils.write(html, xPath, generatedFile);
		}

		final CompileResult compileResult;
		if (xPath.isPreview()) {
			Document documentPreview = Utils.searchHighlight(documentFull.getDocuments());
			documentPreview.setHighlightUrl(target);
			documentPreview.setHighlight(true);
			documentPreview.setPreview(true);
			documentPreview.setCompleteDocument(documentFull);
			documentPreview.setDate(xPath.getDate());
			compileResult = new CompileResult(documentPreview, xPath.getPath(),
					generatedFile);
		} else {
			compileResult = new CompileResult(documentFull, xPath.getPath(),
					generatedFile);
		}
		return compileResult;
	}

	/*
	 * private void apply(List<Document> documents, String key, Object value) {
	 * apply(documents, "parent.", key, value ); }
	 * 
	 * private void apply(List<Document> documents, String prefix, String key,
	 * Object value) { for (Document document : documents) { Map<String, Object>
	 * model = document.getDocumentGenerator().getModel(); model.put(prefix +
	 * key, value); Object obj = document.getDocumentGenerator().getModel()
	 * .get("documents"); if (obj != null && obj instanceof List) {
	 * 
	 * @SuppressWarnings("unchecked") List<Document> documents2 =
	 * (List<Document>) obj; apply(documents2, prefix + "parent." , key, value);
	 * } }
	 * 
	 * }
	 */

	private void applyPath(List<Document> documents, String path) {
		for (Document document : documents) {

			if (document.getHighlight()) {
				document.setOriginalUrl(document.getHighlightUrl());
			}
			document.applyPath(path);
			List<Document> documents2 = document.getDocuments();
			if (documents2 != null) {
				applyPath(documents2, path);
			}
		}
	}

	private List<Document> recursiveHandler(Site site, XPath xPath)
			throws IOException, InterruptedException {
		final List<XPath> children = Utils.getNonHiddenChildren(site,
				xPath.getPath());
		final List<CompileResult> aggregate = new ArrayList<>();
		final List<Document> documents = new ArrayList<>();
		final boolean ascending;
		if (xPath.isAutoSort()) {
			ascending = Utils.guessAutoSort(children);
		} else {
			ascending = xPath.isAscending();
		}
		Utils.sort2(children, ascending);
		for (XPath xPathChild : children) {

			Service.waitFor(xPathChild.getPath());
			CompileResult result = Service.getCompileResult(xPathChild
					.getPath());
			result.addDependencies(xPathChild.getPath(), xPath.getPath());
			boolean pre = xPathChild.isPreview();
			boolean full = xPathChild.isFull() && !pre;
			boolean none = xPathChild.isNavigation() && !pre && !full;

			if (!none) {
				aggregate.add(result);
			}
		}

		for (CompileResult result : aggregate) {
			final Document document;
			if ((document = result.getDocument()) != null) {
				documents.add(document);
			}
		}
		// Utils.sort1( documents, xPath.isInverted() );
		return documents;
	}

	@Override
	public List<String> knownExtensions() {
		return Arrays.asList(new String[] { "nav", "n", "preview", "pre", "p",
				"none", "full", "f" });
	}

	public static Document createDocumentCollection(Site site, XPath xPath,
			XPath original, String relativePathToRoot, List<Document> documents,
			Map<String, Object> previousModel, String templateName,
			String[] pageURLs, int current) throws IOException {
		//since we set the level, we need to work on a copy of this
		documents = HandlerUtils.copy(documents, relativePathToRoot);
		String prefix = original.getLayoutSuffix();
		if (prefix.equals("")) {
			prefix = xPath.getLayoutSuffix();
		}

		TemplateBean templateText = null;
		if (xPath.isPreview()) {
			String prefix1 = "_pre" + prefix;
			try {
				templateText = site.getTemplate(prefix1, templateName,
						xPath.getPath());
			} catch (FileNotFoundException nfe) {
				templateText = site.getTemplate(prefix, templateName,
						xPath.getPath());
			}
		} else {
			templateText = site.getTemplate(prefix, templateName,
					xPath.getPath());
		}

		DocumentGenerator gen = new DocumentGenerator(site, templateText);

		Document document = new Document(xPath, gen, xPath.getTargetURL(), relativePathToRoot, "directory");
		document.setPreview(xPath.isPreview());
		document.setPaging(Arrays.asList(pageURLs), current);
		document.setTemplate(templateName);
		Map<String, Object> model = gen.getModel();
		HandlerUtils.fillModel(xPath.getName(), xPath.getTargetURL(),
				xPath.getDate(), 0, xPath.getFileName(), "", model);

		// if we are in browse mode, show files to browse
		if (xPath.isCopyAll()) {
			List<Document> documents2 = new ArrayList<>();
			for (Document documentx : documents) {
				Document document2 = HandlerCopy.createDocumentBrowse(site,
						documentx.getXPath(), relativePathToRoot, model);
				documents2.add(document2);
			}
			document.setDocuments(documents2);

		} else {
			document.setDocuments(documents);
		}
		return document;
	}
}
