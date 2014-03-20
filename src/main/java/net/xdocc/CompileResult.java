package net.xdocc;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.xdocc.handlers.Handler;
import net.xdocc.handlers.HandlerBean;

/**
 * The result of a compilation can be a document or a collection of documents.
 * 
 * @author Thomas Bocek
 * 
 */
public class CompileResult implements Serializable {

	private static final long serialVersionUID = -673796597290628935L;
	private static final Logger LOG = LoggerFactory.getLogger(CompileResult.class);
	
	public final static CompileResult DONE = new CompileResult(null, null,
			null, null);
	public final static CompileResult ERROR = new CompileResult(null, null,
			null, null);
	private final Document document;
	private final Set<FileInfos> fileInfos;
	private final HandlerBean handlerBean;
	private final Handler handler;
	
	private final Map<Path, Set<Path>> dependenciesUp = new HashMap<>();
	private final Map<Path, Set<Path>> dependenciesDown = new HashMap<>();

	public CompileResult(Document document, Set<FileInfos> fileInfos,
			HandlerBean handlerBean, Handler handler) {
		this.document = document;
		this.fileInfos = fileInfos;
		this.handlerBean = handlerBean;
		this.handler = handler;
		LOG.info("created CR ");
		if(fileInfos != null) {
		for (FileInfos inf : fileInfos) {
			LOG.info("- fileInfo: " + inf.getTarget().toString());
			LOG.info(" -- sSize = " + inf.getSourceSize() + " sTime = "
					+ inf.getSourceTimestamp());
			LOG.info(" -- tSize = " + inf.getTargetSize() + " tTime = "
					+ inf.getTargetTimestamp());
		}
		}else {
			LOG.info(" - with NULL fileinfos");
		}
	}

	public CompileResult(Document document, Path source,
			HandlerBean handlerBean, Handler handler, Path... targets) {
		this.document = document;
		this.handlerBean = handlerBean;
		this.handler = handler;
		this.fileInfos = new HashSet<>();
		if (targets != null) {
			for (int i = 0; i < targets.length; i++) {
				if (targets[i] != null) {
					try {
						long sourceSize = Files.size(source);
						long targetSize = Files.size(targets[i]);
						long targetTimestamp = Files.getLastModifiedTime(
								targets[i]).toMillis();
						long sourceTimestamp = Files
								.getLastModifiedTime(source).toMillis();
						fileInfos.add(new FileInfos(targets[i],
								targetTimestamp, targetSize, sourceTimestamp,
								sourceSize));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		LOG.info("created CR ");
		for (FileInfos inf : fileInfos) {
			LOG.info("- fileInfo: " + inf.getTarget().toString());
			LOG.info(" -- sSize = " + inf.getSourceSize() + " sTime = "
					+ inf.getSourceTimestamp());
			LOG.info(" -- tSize = " + inf.getTargetSize() + " tTime = "
					+ inf.getTargetTimestamp());
		}
	}

	public Document getDocument() {
		return document;
	}

	public Set<FileInfos> getFileInfos() {
		return fileInfos;
	}

	public void addDependencies(Path child, Path parent) {
		addDependencyUp(child, parent);
		addDependencyDown(child, parent);
	}

	private void addDependencyUp(Path child, Path parent) {
		Set<Path> parentSet = getDependenciesUp().get(child);
		if (parentSet == null) {
			parentSet = new HashSet<>();
			getDependenciesUp().put(child, parentSet);
		}
		parentSet.add(parent);
	}

	private void addDependencyDown(Path child, Path parent) {

		Set<Path> childSet = getDependenciesDown().get(parent);
		if (childSet == null) {
			childSet = new HashSet<>();
			getDependenciesDown().put(parent, childSet);
		}
		childSet.add(child);
	}

	public Set<Path> findDependencies(Path source) {
		Set<Path> result = new HashSet<>();
		findDependenciesUpRec(source, result);
		findDependenciesDownRec(source, result);
		return result;
	}
	
	private void findDependenciesUpRec(Path up, Set<Path> result) {
		CompileResult cr = handlerBean.getSite().service().getCompileResult(up);
		if (cr == null) {
			return;
		}
		Set<Path> parentSet = cr.getDependenciesUp().get(up);
		if (parentSet == null) {
			return;
		}
		for (Path path : parentSet) {
			result.add(path);
			findDependenciesUpRec(path, result);
		}
	}

	private void findDependenciesDownRec(Path down, Set<Path> result) {
		CompileResult cr = handlerBean.getSite().service().getCompileResult(down);
		if (cr == null) {
			return;
		}
		Set<Path> childSet = cr.getDependenciesDown().get(down);
		if (childSet == null) {
			return;
		}
		for (Path path : childSet) {
			result.add(path);
			findDependenciesDownRec(path, result);
		}
	}

	public Map<Path, Set<Path>> getDependenciesUp() {
		return dependenciesUp;
	}

	public Map<Path, Set<Path>> getDependenciesDown() {
		return dependenciesDown;
	}

	public void addAllDependencies(Map<Path, Set<Path>> dependenciesUp,
			Map<Path, Set<Path>> dependenciesDown) {
		this.dependenciesUp.putAll(dependenciesUp);
		this.dependenciesDown.putAll(dependenciesDown);
	}

	public CompileResult copyDocument() {
		if (document != null) {
			copy(document, document.getDocuments());
			CompileResult tmp = new CompileResult(document.copy(), fileInfos, handlerBean,
					handler);
			tmp.addAllDependencies(dependenciesUp, dependenciesDown);
			return tmp;
		} else
			return this;
	}

	private void copy(Document documentOrig, List<Document> documents) {
		if (documentOrig.getCompleteDocument() != null) {
			documentOrig.setCompleteDocument(documentOrig.getCompleteDocument()
					.copy());
		}
		if (documents == null) {
			return;
		}
		List<Document> copies = new ArrayList<>();
		for (Document document : documentOrig.getDocuments()) {
			if (document.getDocuments() != null) {
				copy(document, document.getDocuments());
			}
			copies.add(document.copy());
		}
		documentOrig.setDocuments(copies);
	}

	public HandlerBean getHandlerBean() {
		return handlerBean;
	}

	public Handler getHandler() {
		return handler;
	}
}
