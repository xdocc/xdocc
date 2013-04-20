package net.xdocc;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The result of a compilation can be a document or a collection of documents.
 * 
 * @author Thomas Bocek
 * 
 */
public class CompileResult implements Serializable {

	private static final long serialVersionUID = -673796597290628935L;
	public final static CompileResult EMPTY = new CompileResult(null, null);
	public final static CompileResult DONE = new CompileResult(null, null);
	public final static CompileResult ERROR = new CompileResult(null, null);
	private final Document document;
	private final Set<FileInfos> fileInfos;

	public CompileResult(Document document, Set<FileInfos> fileInfos) {
		this.document = document;
		this.fileInfos = fileInfos;
	}

	public CompileResult(Document document, Path source, Path... targets) {
		this.document = document;
		this.fileInfos = new HashSet<>();
		for (int i = 0; i < targets.length; i++) {
			try {
				long sourceSize = Files.size(source);
				long targetSize = Files.size(targets[i]);
				long targetTimestamp = Files.getLastModifiedTime(targets[i])
						.toMillis();
				long sourceTimestamp = Files.getLastModifiedTime(source)
						.toMillis();
				fileInfos.add(new FileInfos(targets[i], targetTimestamp,
						targetSize, sourceTimestamp, sourceSize));
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	public Document getDocument() {
		return document;
	}

	public Set<FileInfos> getFileInfos() {
		return fileInfos;
	}

	private final Map<Path, Set<Path>> dependenciesUp = new HashMap<>();
	private final Map<Path, Set<Path>> dependenciesDown = new HashMap<>();

	public void addDependencies(Path child, Path parent) {
		addDependencyUp(child, parent);
		addDependencyDown(child, parent);
	}

	public void addDependencyUp(Path child, Path parent) {
		Set<Path> parentSet = getDependenciesUp().get(child);
		if (parentSet == null) {
			parentSet = new HashSet<>();
			getDependenciesUp().put(child, parentSet);
		}
		parentSet.add(parent);
	}

	public void addDependencyDown(Path child, Path parent) {

		Set<Path> childSet = getDependenciesDown().get(parent);
		if (childSet == null) {
			childSet = new HashSet<>();
			getDependenciesDown().put(parent, childSet);
		}
		childSet.add(child);
	}

	public Set<Path> findDependencies(Path source) {
		Set<Path> result = new HashSet<>();
		// first all the ups
		findDependenciesUpRec(source, result);
		findDependenciesDownRec(source, result);
		return result;
	}

	public void findDependenciesUpRec(Path up, Set<Path> result) {
		CompileResult cr = Service.getCompileResult(up);
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

	public void findDependenciesDownRec(Path down, Set<Path> result) {
		CompileResult cr = Service.getCompileResult(down);
		if (cr == null) {
			return;
		}
		Set<Path> childSet = cr.getDependenciesDown().get(down);
		if (childSet == null) {
			return;
		}
		for (Path path : childSet) {
			result.add(path);
			findDependenciesUpRec(path, result);
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
}
