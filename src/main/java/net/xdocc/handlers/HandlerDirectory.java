package net.xdocc.handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.xdocc.CompileResult;
import net.xdocc.Document;
import net.xdocc.DocumentGenerator;
import net.xdocc.Link;
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
	public CompileResult compile(HandlerBean handlerBean, boolean writeToDisk)
			throws Exception {
		//
		List<Document> documents = recursiveHandler(handlerBean.getSite(),
				handlerBean.getxPath());
		// Utils.adjustUrls(xPath, documents, path);

		String url = handlerBean.getxPath().getTargetURL();

		if (documents.size() == 0) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("The directory [" + handlerBean.getxPath()
						+ "] has no elements");
			}

			Path p = handlerBean.getxPath().getTargetPath(url);
			Path generatedDir = Files.createDirectories(p);
			handlerBean.getDirtyset().add(generatedDir);
			return CompileResult.DONE;
		}
		int pageSize = handlerBean.getxPath().getPageSize();

		documents = promoteLinks(documents);
		// model.put("preview", xPath.isPreview());
		String target = handlerBean.getxPath().resolveTargetURL("index.html");

		int pages = pageSize == 0 ? 0 : documents.size() / (pageSize + 1);
		List<List<Document>> tmp = Utils.split(documents, pages, pageSize);
		String[] pageURLs = Utils.paging(handlerBean.getxPath(), pages);
		int counter = 0;

		// create the site

		Document doc0 = null;
		Path generatedFile0 = null;
		final CompileResult compileResult;

		for (List<Document> list : tmp) {
			Document doc = HandlerDirectory.createDocumentCollection(
					handlerBean.getSite(), handlerBean.getxPath(),
					handlerBean.getxPath(),
					handlerBean.getRelativePathToRoot(), list,
					handlerBean.getModel(), "collection", "directory",
					pageURLs, counter);
			doc.applyPath1(handlerBean.getRelativePathToRoot());
			// doc.applyPath1("BBB"+relativePathToRoot);
			final Path generatedFile;
			if (counter == 0) {
				generatedFile = handlerBean.getxPath().getTargetPath(
						handlerBean.getxPath().resolveTargetURL("index.html"));
				generatedFile0 = generatedFile;
				doc0 = doc;
			} else {
				generatedFile = handlerBean.getxPath().getTargetPath(
						handlerBean.getxPath().resolveTargetURL(
								"index_" + counter + ".html"));
			}
			Map<String, Object> model = new HashMap<>();
			model.put("page_nr", list.size());
			model.put("page_urls", pageURLs);
			model.put("current_page", counter);
			model.put("document_size", documents.size());
			model.put("url", url);
			if (!handlerBean.getxPath().isRoot()) {
				Link current = handlerBean
						.getSite()
						.service()
						.readNavigation(handlerBean.getSite(),
								handlerBean.getxPath());
				model.put("local_navigation", current);
			}
			if (writeToDisk && !handlerBean.getxPath().isPreview()) {
				Utils.writeHTML(handlerBean.getSite(), handlerBean.getxPath(),
						handlerBean.getDirtyset(),
						handlerBean.getRelativePathToRoot(), doc,
						generatedFile, "directory", model);
			}
			counter++;
		}

		if (handlerBean.getxPath().isPreview()) {
			Document documentPreview = Utils.searchHighlight(doc0
					.getDocuments());
			documentPreview.setHighlightUrl(target);
			documentPreview.setHighlight(true);
			documentPreview.setPreview(true);
			documentPreview.setCompleteDocument(doc0);
			documentPreview.setDate(handlerBean.getxPath().getDate());
			if (writeToDisk) {
				Utils.writeHTML(handlerBean.getSite(), handlerBean.getxPath(),
						handlerBean.getDirtyset(),
						handlerBean.getRelativePathToRoot(), documentPreview,
						generatedFile0, "directory", handlerBean.getModel());
			}
			compileResult = new CompileResult(documentPreview, handlerBean
					.getxPath().getPath(), handlerBean, this, generatedFile0);
		} else {
			compileResult = new CompileResult(doc0, handlerBean.getxPath()
					.getPath(), handlerBean, this, generatedFile0);
		}
		if(compileResult.getHandler() == null || compileResult.getHandlerBean() == null || compileResult.getFileInfos() == null) {
			LOG.info("bad compile result: "+handlerBean.getxPath());
		}
		return compileResult;
	}

	private List<Document> promoteLinks(List<Document> documents) {
		List<Document> retVal = new ArrayList<>();
		for (Document document : documents) {
			if ("link".equals(document.getType())
					&& document.getDocuments() != null) {
				retVal.addAll(document.getDocuments());
			} else {
				retVal.add(document);
			}
		}
		return retVal;
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

	// private void applyPath(List<Document> documents, String path) {
	/*
	 * for (Document document : documents) {
	 * 
	 * if (document.getHighlight()) {
	 * document.setOriginalUrl(document.getHighlightUrl()); }
	 * document.applyPath(path); List<Document> documents2 =
	 * document.getDocuments(); if (documents2 != null) { applyPath(documents2,
	 * path); } }
	 */
	// }

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

			site.service().waitFor(xPathChild.getPath());
			CompileResult result = site.service().getCompileResult(
					xPathChild.getPath());
			result.addDependencies(xPathChild.getPath(), xPath.getPath());
			boolean pre = xPathChild.isPreview();
			//TODO: full is default, no need for isFull
			boolean full = xPathChild.isFull() && !pre;
			boolean nav = xPathChild.isNavigation() && !pre && !full ;
			boolean none = xPathChild.isNone() && !pre && !full && !nav;

			if (!nav && !none) {
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
			XPath original, String relativePathToRoot,
			List<Document> documentsA, Map<String, Object> previousModel,
			String templateName, String type, String[] pageURLs, int current)
			throws IOException {
		// since we set the level, we need to work on a copy of this
		List<Document> documents = HandlerUtils.copy(documentsA,
				relativePathToRoot);
		String prefix = original.getLayoutSuffix();
		if (prefix.equals("")) {
			prefix = xPath.getLayoutSuffix();
		}

		TemplateBean templateText = site.getTemplate(prefix, templateName,
				xPath.getPath());

		DocumentGenerator gen = new DocumentGenerator(site, templateText);

		Document document = new Document(xPath, gen, xPath.getTargetURL(), type);
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
