package net.xdocc.handlers;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.xdocc.CompileResult;
import net.xdocc.Document;
import net.xdocc.Site;
import net.xdocc.Utils;
import net.xdocc.XPath;
import net.xdocc.CompileResult.Key;

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

		final Key<Path> crkParent = new Key<Path>(handlerBean.getxPath().getPath(), handlerBean.getxPath().getPath());
		
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
			return new CompileResult(null, null, handlerBean, this);
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
				
				// regular CR
				final Key<Path> crk = new Key<Path>(found.getPath(), found.getPath());
				handlerBean.getSite().service().waitFor(crk);
				CompileResult compileResult = handlerBean.getSite().service().getCompileResult(crk);
				
				// special CR
				final Key<Path> crkNew = new Key<Path>(found.getPath(), handlerBean.getxPath().getPath());
				CompileResult specialCR;
				boolean isCompiled = false;
				if(handlerBean.getSite().service().getCompileResult(crkNew) == null) {
					HandlerBean hbNew = new HandlerBean();
					hbNew.setDirtyset(compileResult.getHandlerBean().getDirtyset());
					hbNew.setModel(compileResult.getHandlerBean().getModel());
					hbNew.setRelativePathToRoot(handlerBean.getRelativePathToRoot());
					hbNew.setSite(compileResult.getHandlerBean().getSite());
					XPath xNew = compileResult.getHandlerBean().getxPath();
					hbNew.setxPath(xNew);
					specialCR = compileResult.getHandler().compile(hbNew, false);
					isCompiled = true;
				}else {
					specialCR = handlerBean.getSite().service().getCompileResult(crkNew);
				}
				compileResult.addDependencies(crk, crkParent);
				compileResult.addDependencies(crk, crkNew);
				specialCR.addDependencies(crk, crkParent);
				
				documents.add(specialCR.getDocument());

				// put specialCR in cache
				if(isCompiled) {
					handlerBean.getSite().service().addCompileResult(crkNew, specialCR);
				}
				
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
