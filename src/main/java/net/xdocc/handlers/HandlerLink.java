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
	public CompileResult compile(Site site, XPath xPath, Set<Path> dirtyset, Map<String, Object> previousModel, String relativePathToRoot)
			throws Exception {
	
		Configuration config = new PropertiesConfiguration(xPath.getPath().toFile());
		
		List<Object> urls = config.getList("url", new ArrayList<>());
		int limit = config.getInt("limit", -1);
		
		List<XPath> founds = new ArrayList<>();
		
		for(Object url:urls) {
			founds.addAll(Utils.findURL(site, xPath, (String)url));
		}
		if (founds.size() == 0
				|| (founds.size() > 0 && !founds.get(0).isVisible())) {
			return CompileResult.DONE;
		} else {
			List<Document> documents = new ArrayList<>();
			
			final boolean ascending;
			if (xPath.isAutoSort()) {
				ascending = Utils.guessAutoSort(founds);
			} else {
				ascending = xPath.isAscending();
			}
			Utils.sort2(founds, ascending);
			
			for(XPath found:founds) {
				Service.waitFor(found.getPath());
				CompileResult compileResult = Service.getCompileResult(
						found.getPath());
				
				compileResult.addDependencies(found.getPath(), xPath.getPath());
				Map<Path, Set<Path>> dependenciesUp = compileResult.getDependenciesUp();
				Map<Path, Set<Path>> dependenciesDown = compileResult.getDependenciesDown();
				
				//documents.add(compileResult.getDocument());
				
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
					setRelavtive(compileResult.getDocument(), relativePathToRoot);
				}
				//this may happen is a file is not hidden, but also not visible  
				if(compileResult.getDocument()!=null) {
					documents.add(compileResult.getDocument());
				}
			}
			
			if(limit >= 0) {
				documents = documents.subList(0, documents.size() < limit? documents.size():limit );
			}
			
			int pageSize = xPath.getPageSize();
			int pages = pageSize == 0 ? 0 : documents.size() / (pageSize + 1);
			List<List<Document>> tmp = Utils.split(documents, pages, pageSize);
			String[] pageURLs=Utils.paging(xPath, pages);
			int counter = 0;
			
			
			
			Document doc0 = null;
			Path generatedFile0 = null;
			for(List<Document> list:tmp) {

				Document doc = HandlerDirectory.createDocumentCollection(site, xPath, xPath, relativePathToRoot, list, previousModel, "link", pageURLs, counter);
				final Path generatedFile;
				if(counter==0) {
					generatedFile = xPath.getTargetPath(xPath.getTargetURL() + ".html");
					generatedFile0 = generatedFile;
					doc0 = doc;
				} else {
					generatedFile = xPath.getTargetPath(xPath.getTargetURL() + "_"+counter+".html");
				}
				counter++;
				Utils.writeHTML(site, xPath, dirtyset, relativePathToRoot, doc, generatedFile, "collection");
			
			}
			
			return new CompileResult(doc0, xPath.getPath(), generatedFile0);
		}
	}

	//TODO: is this method really necessary? 
	private void setRelavtive(Document document, String url) {
		document.setRelative(url);
		@SuppressWarnings("unchecked")
		List<Document> documents = document.getDocuments();
		if (documents != null) {
			for (Document document2 : documents) {
				setRelavtive(document2, url);
			}
		}
	}
}
