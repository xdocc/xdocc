package net.xdocc.handlers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.xdocc.CompileResult;
import net.xdocc.Document;
import net.xdocc.FileInfos;
import net.xdocc.Service;
import net.xdocc.Site;
import net.xdocc.Utils;
import net.xdocc.XPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlerLink implements Handler {

	private static final Logger LOG = LoggerFactory
			.getLogger(HandlerLink.class);

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
	public CompileResult compile(Site site, XPath xPath, Set<Path> dirtyset, Map<String, Object> previousModel, String relativePathToRoot)
			throws Exception {
		// String path = Utils.relativePathToRoot(site.getSource(),
		// xPath.getPath());
		// copy the original image
		// Path generatedFile = xPath.getTargetPath( xPath.getTargetURL() +
		// xPath.getExtensions() );
		// dirtyset.add( generatedFile );
		// Path generatedDir = Files.createDirectories(
		// generatedFile.getParent() );
		// dirtyset.add( generatedDir );

		Properties properties = new Properties();
		properties.load(Files.newInputStream(xPath.getPath()));
		String url = properties.getProperty("url");
		int limit;
		try {
			limit = Integer.parseInt(properties.getProperty("limit"));
		} catch (Exception e) {
			limit = Integer.MAX_VALUE;
		}

		List<XPath> founds = Utils.findURL(site, xPath, url);

		if (founds.size() > 1) {
			LOG.warn("found more than one URLs for url=" + url + " ->" + founds);
			return CompileResult.DONE;
		} else if (founds.size() == 0
				|| (founds.size() > 0 && !founds.get(0).isVisible())) {
			return CompileResult.DONE;
		} else {
			XPath found = founds.get(0);
			
			Service.waitFor(found.getPath());
			CompileResult compileResult = Service.getCompileResult(
					found.getPath());
			
			compileResult.addDependencies(found.getPath(), xPath.getPath());
			Map<Path, Set<Path>> dependenciesUp = compileResult.getDependenciesUp();
			Map<Path, Set<Path>> dependenciesDown = compileResult.getDependenciesDown();
			compileResult = Utils.subList(compileResult, limit);
			
			//we need to update the cached data for the source, or we won't use the .link file, but the original.
			
			Set<FileInfos> result = new HashSet<>();
			if(compileResult.getFileInfos() != null) {
				for(FileInfos fileInfos:compileResult.getFileInfos()) {
					long sourceSize = Files.size(xPath.getPath());
					long sourceTimestamp = Files.getLastModifiedTime(xPath.getPath())
							.toMillis();
					result.add(fileInfos.copy(sourceTimestamp, sourceSize));
				}
				compileResult = new CompileResult(compileResult.getDocument(), result);
				
			}
			compileResult.addAllDependencies(dependenciesUp, dependenciesDown);

			if (compileResult.getDocument() != null) {
				setRelavtive(compileResult.getDocument(), url);
			}
			return compileResult;
			//TODO write HTML file
		}
	}

	private void setRelavtive(Document document, String url) {
		document.setValue("relative", url);
		@SuppressWarnings("unchecked")
		List<Document> documents = (List<Document>) document
				.getDocumentGenerator().getModel().get("documents");
		if (documents != null) {
			for (Document document2 : documents) {
				setRelavtive(document2, url);
			}
		}
	}
}
