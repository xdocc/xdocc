package net.xdocc;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.xdocc.handlers.Handler;
import net.xdocc.handlers.HandlerBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The result of a compilation can be a document or a collection of documents.
 * 
 * @author Thomas Bocek
 * 
 */
public class CompileResult implements Serializable {

	private static final long serialVersionUID = -673796597290628935L;
	private static final Logger LOG = LoggerFactory
			.getLogger(CompileResult.class);

	public final static CompileResult DONE = new CompileResult(null, null,
			null, null);
	public final static CompileResult ERROR = new CompileResult(null, null,
			null, null);
	private final Document document;
	private final Set<FileInfos> fileInfos;
	private final HandlerBean handlerBean;
	private final Handler handler;

	private final Map<Key<Path>, Set<Key<Path>>> dependenciesUp = new HashMap<Key<Path>, Set<Key<Path>>>();
	private final Map<Key<Path>, Set<Key<Path>>> dependenciesDown = new HashMap<Key<Path>, Set<Key<Path>>>();

	public CompileResult(Document document, Set<FileInfos> fileInfos,
			HandlerBean handlerBean, Handler handler) {
		this.document = document;
		this.fileInfos = fileInfos;
		this.handlerBean = handlerBean;
		this.handler = handler;
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
	}

	public Document getDocument() {
		return document;
	}

	public Set<FileInfos> getFileInfos() {
		return fileInfos;
	}

	public void addDependencies(Key<Path> child,
			Key<Path> parent) {
		addDependencyUp(child, parent);
		addDependencyDown(child, parent);
	}

	private void addDependencyUp(Key<Path> child,
			Key<Path> parent) {
		Set<Key<Path>> parentSet = getDependenciesUp().get(child);
		if (parentSet == null) {
			parentSet = new HashSet<>();
			getDependenciesUp().put(child, parentSet);
		}
		parentSet.add(parent);
	}

	private void addDependencyDown(Key<Path> child,
			Key<Path> parent) {
		Set<Key<Path>> childSet = getDependenciesDown()
				.get(parent);
		if (childSet == null) {
			childSet = new HashSet<>();
			getDependenciesDown().put(parent, childSet);
		}
		childSet.add(child);
	}

	public Set<Key<Path>> findDependencies(
			Key<Path> source) {
		Set<Key<Path>> result = new HashSet<>();
		findDependenciesUpRec(source, result);
		findDependenciesDownRec(source, result);
		return result;
	}

	private void findDependenciesUpRec(Key<Path> up,
			Set<Key<Path>> result) {
		CompileResult cr = handlerBean.getSite().service().getCompileResult(up);
		if (cr == null) {
			return;
		}
		Set<Key<Path>> parentSet = cr.getDependenciesUp().get(up);
		if (parentSet == null) {
			return;
		}
		for (Key<Path> path : parentSet) {
			result.add(path);
			findDependenciesUpRec(path, result);
		}
	}

	private void findDependenciesDownRec(Key<Path> down,
			Set<Key<Path>> result) {
		CompileResult cr = handlerBean.getSite().service()
				.getCompileResult(down);
		if (cr == null) {
			return;
		}
		Set<Key<Path>> childSet = cr.getDependenciesDown().get(
				down);
		if (childSet == null) {
			return;
		}
		for (Key<Path> path : childSet) {
			result.add(path);
			findDependenciesDownRec(path, result);
		}
	}

	public Map<Key<Path>, Set<Key<Path>>> getDependenciesUp() {
		return dependenciesUp;
	}

	public Map<Key<Path>, Set<Key<Path>>> getDependenciesDown() {
		return dependenciesDown;
	}

	public void addAllDependencies(
			Map<Key<Path>, Set<Key<Path>>> dependenciesUp,
			Map<Key<Path>, Set<Key<Path>>> dependenciesDown) {
		this.dependenciesUp.putAll(dependenciesUp);
		this.dependenciesDown.putAll(dependenciesDown);
	}

	public HandlerBean getHandlerBean() {
		return handlerBean;
	}

	public Handler getHandler() {
		return handler;
	}

	public static class Key<T> {

		final private T source;
		final private T target;

		public Key(T source, T target) {
			this.source = source;
			this.target = target;
		}

		public T getSource() {
			return source;
		}

		public T getTarget() {
			return target;
		}
		
		@Override
		public int hashCode() {
			return source.hashCode() ^ target.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (!(obj instanceof Key<?>)) {
				return false;
			}
			Key<?> o = (Key<?>) obj;
			return source.equals(o.source) && target.equals(o.target);
		}
		
		@Override
		public String toString() {
			return "source: "+source.toString()+" target: "+target.toString();
		}

	}
}
