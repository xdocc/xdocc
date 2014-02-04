package net.xdocc.handlers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.xdocc.CompileResult;
import net.xdocc.Document;
import net.xdocc.FileInfos;
import net.xdocc.Service;
import net.xdocc.Site;
import net.xdocc.Utils;
import net.xdocc.XPath;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

public class HandlerLink implements Handler {

	@Override
	public boolean canHandle(Site site, XPath xPath) {
		return xPath.isCompile() && !xPath.isDirectory()
				&& HandlerUtils.knowsExtension(knownExtensions(), xPath);
	}

	@Override
	public List<String> knownExtensions() {
		return Arrays.asList(new String[] { "link", "Link", "LINK" });
	}

	@Override
	public CompileResult compile(HandlerBean handlerBean, boolean writeToDisk)
			throws Exception {

		Configuration config = new PropertiesConfiguration(handlerBean
				.getxPath().getPath().toFile());

		List<Object> urls = config.getList("url", new ArrayList<>());
		int limit = config.getInt("limit", -1);

		List<XPath> founds = new ArrayList<>();

		for (Object url : urls) {
			founds.addAll(Utils.findURL(handlerBean.getSite(),
					handlerBean.getxPath(), (String) url));
		}
		if (founds.size() == 0
				|| (founds.size() > 0 && !founds.get(0).isVisible())) {
			return CompileResult.DONE;
		} else {
			List<Document> documents = new ArrayList<>();

			final boolean ascending;
			if (handlerBean.getxPath().isAutoSort()) {
				ascending = Utils.guessAutoSort(founds);
			} else {
				ascending = handlerBean.getxPath().isAscending();
			}
			Utils.sort2(founds, ascending);

			for (XPath found : founds) {
				handlerBean.getSite().service().waitFor(found.getPath());
				CompileResult compileResult = handlerBean.getSite().service().getCompileResult(found
						.getPath());

				compileResult.addDependencies(found.getPath(), handlerBean
						.getxPath().getPath());
				Map<Path, Set<Path>> dependenciesUp = compileResult
						.getDependenciesUp();
				Map<Path, Set<Path>> dependenciesDown = compileResult
						.getDependenciesDown();

				if (compileResult.getDocument() != null
						&& compileResult.getHandler() != this) {
					compileResult.getHandlerBean().getModel()
							.put("relative", found.getTargetURLPath());
					CompileResult compileResult2 = compileResult.getHandler()
							.compile(compileResult.getHandlerBean(), false);
					// this may happen is a file is not hidden, but also not
					// visible
					documents.add(compileResult2.getDocument());
				}

				Set<FileInfos> result = new HashSet<>();
				if (compileResult.getFileInfos() != null) {
					for (FileInfos fileInfos : compileResult.getFileInfos()) {
						long sourceSize = Files.size(handlerBean.getxPath()
								.getPath());
						long sourceTimestamp = Files.getLastModifiedTime(
								handlerBean.getxPath().getPath()).toMillis();
						result.add(fileInfos.copy(sourceTimestamp, sourceSize));
					}
					compileResult = new CompileResult(
							compileResult.getDocument(), result, handlerBean,
							this);

				}
				compileResult.addAllDependencies(dependenciesUp,
						dependenciesDown);
			}

			if (limit >= 0) {
				documents = documents.subList(0,
						documents.size() < limit ? documents.size() : limit);
			}

			Document doc = HandlerDirectory.createDocumentCollection(
					handlerBean.getSite(), handlerBean.getxPath(),
					handlerBean.getxPath(),
					handlerBean.getRelativePathToRoot(), documents,
					handlerBean.getModel(), "link", "link", new String[0], 0);
			
			Path generatedFile = null;
			if (writeToDisk) {
				generatedFile = handlerBean.getxPath().getTargetPath(
						handlerBean.getxPath().getTargetURL() + ".html");
				Utils.writeHTML(handlerBean.getSite(), handlerBean.getxPath(),
						handlerBean.getDirtyset(),
						handlerBean.getRelativePathToRoot(), doc,
						generatedFile, "link");
			}
			return new CompileResult(doc, handlerBean.getxPath().getPath(),
					handlerBean, this, generatedFile);
		}
	}
}
