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
import net.xdocc.CompileResult.Key;
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
				handlerBean.getxPath(), handlerBean);
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
			return new CompileResult(null, handlerBean.getxPath().getPath(), handlerBean, this, generatedDir);
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
			model.put(Document.PAGE_NR, list.size());
			model.put(Document.PAGE_URLS, pageURLs);
			model.put(Document.CURRENT_PAGE, counter);
			model.put(Document.DOCUMENT_SIZE, documents.size());
			model.put(Document.URL, url);
			if (!handlerBean.getxPath().isRoot()) {
				Link current = handlerBean
						.getSite()
						.service()
						.readNavigation(handlerBean.getSite(),
								handlerBean.getxPath());
				model.put(Document.LOCAL_NAVIGATION, current);
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

	
	private List<Document> recursiveHandler(Site site, XPath xPath, HandlerBean fromHandler)
			throws Exception {
		final List<XPath> children = Utils.getNonHiddenChildren(site,
				xPath.getPath());
		final List<CompileResult> aggregate = new ArrayList<>();
		final List<Document> documents = new ArrayList<>();
		final Key<Path> crkParent = new Key<Path>(xPath.getPath(), xPath.getPath());
		final boolean ascending;
		if (xPath.isAutoSort()) {
			ascending = Utils.guessAutoSort(children);
		} else {
			ascending = xPath.isAscending();
		}
		Utils.sort2(children, ascending);
		for (XPath xPathChild : children) {
			// 1. check if is directory
			// -> no: no recompiling needed
			if (!xPathChild.isDirectory() && !fromHandler.isForceCompile()) {
				
				Key<Path> crk = new Key<Path>(xPathChild.getPath(), xPathChild.getPath());
				site.service().waitFor(crk);
				CompileResult result = site.service().getCompileResult(crk);
				result.addDependencies(crk, crkParent);
				boolean pre = xPathChild.isPreview();
				//TODO: full is default, no need for isFull
				boolean full = xPathChild.isFull() && !pre;
				boolean nav = xPathChild.isNavigation() && !pre && !full ;
				boolean none = xPathChild.isNone() && !pre && !full && !nav;
				
				if (!nav && !none) {
					aggregate.add(result);
				}
			// --> yes: recompile with relPathToRoot from fromHandler
			}else {
				Key<Path> crk = new Key<Path>(xPathChild.getPath(), xPathChild.getPath());
				Key<Path> crkNew = new Key<Path>(xPathChild.getPath(), fromHandler.getxPath().getPath());
				CompileResult crNew;
				// get old result first (regular cr)
				site.service().waitFor(crk);
				CompileResult compileResult = site.service().getCompileResult(crk);
				if(site.service().getCompileResult(crkNew) == null) {
					// copy handler and set new relPathToRoot
					HandlerBean hbNew = new HandlerBean();
					hbNew.setDirtyset(compileResult.getHandlerBean().getDirtyset());
					hbNew.setModel(compileResult.getHandlerBean().getModel());
					hbNew.setRelativePathToRoot(fromHandler.getRelativePathToRoot());
					hbNew.setSite(compileResult.getHandlerBean().getSite());
					XPath xNew = compileResult.getHandlerBean().getxPath();
					hbNew.setxPath(xNew);
					hbNew.setForceCompile(true);
					// no need to write, only compile
					crNew = compileResult.getHandler().compile(hbNew, false);
					// add new cr
					site.service().addCompileResult(crkNew, crNew);
				}else {
					// crNew already compiled
					crNew = site.service().getCompileResult(crkNew);
				}
				// adding dependencies
				compileResult.addDependencies(crk, crkNew);
				crNew.addDependencies(crk, crkParent);
				
				boolean pre = xPathChild.isPreview();
				//TODO: full is default, no need for isFull
				boolean full = xPathChild.isFull() && !pre;
				boolean nav = xPathChild.isNavigation() && !pre && !full ;
				boolean none = xPathChild.isNone() && !pre && !full && !nav;
				
				if (!nav && !none) {
					aggregate.add(crNew);
				}
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
		
		Key<Path> crk = new Key<Path>(xPath.getPath(), xPath.getPath());
		TemplateBean templateText = site.getTemplate(prefix, templateName,
				crk);

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
