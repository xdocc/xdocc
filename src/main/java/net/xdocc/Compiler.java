package net.xdocc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.xdocc.CompileResult.Key;
import net.xdocc.handlers.Handler;
import net.xdocc.handlers.HandlerBean;
import net.xdocc.handlers.HandlerCopy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Compiler implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(Compiler.class);

	private static final AtomicInteger COMPILER_COUNTER = new AtomicInteger();

	final private List<Handler> handlers;

	final private Path siteToCompile;

	final private Site site;

	final private Set<Path> dirtyset;

	final private Handler handlerCopy = new HandlerCopy();
	
	final private  Map<String, Object> model;

	public Compiler(Site site, Path path, Set<Path> dirtyset, Map<String, Object> model) {
		COMPILER_COUNTER.incrementAndGet();
		this.handlers = site.getHandlers();
		this.siteToCompile = path;
		this.site = site;
		this.dirtyset = dirtyset;
		this.model = model;
	}

	@Override
	public void run() {
		try {
			compile(siteToCompile);
		} catch (Exception e) {
			LOG.error("interrupted: " + e);
		}
		if (COMPILER_COUNTER.decrementAndGet() == 0) {
			try {
				site.service().compileDone(site);
			} catch (IOException e) {
				LOG.error("cannot execute compile done: " + e);
				e.printStackTrace();
			}
		}

	}

	public void compile(Path siteToCompile) throws InterruptedException {
		if(siteToCompile.getFileName().toString().endsWith("~")) {
			return;
		}
		XPath xPath = new XPath(site, siteToCompile);
		if (xPath.isHidden()) {
			return;
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("compile " + siteToCompile);
		}
		recursive();

		boolean foundHandler = false;

		for (Handler handler : handlers) {
			if (handler.canHandle(site, xPath)) {
				compile(handler, xPath);
				foundHandler = true;
			}
		}

		// compile as last
		if (!foundHandler) {
			boolean compile = compile(handlerCopy, xPath);
			if (compile) {
				foundHandler = true;
			}
		}
		if (!foundHandler) {
			LOG.warn("no handler found for " + xPath.getFileName());
		}
	}

	private boolean compile(Handler handler, XPath xPath) {
		Key<Path> crk = new Key<Path>(xPath.getPath(), xPath.getPath());
		CompileResult result = site.service().getCompileResult(crk);
		if(result != null) {
			if(site.service().isCached(site, crk)) {
				for(FileInfos fileInfos : site.service().getFromCache(crk)) {
					dirtyset.add(fileInfos.getTarget().toPath());
				}
				site.service().notifyFor();
				return true;
			} 
		}
		try {
			String relativePathToRoot = Utils.relativePathToRoot(site.getSource(),
					xPath.getPath());
			HandlerBean handlerBean = new HandlerBean();
			handlerBean.setSite(site);
			handlerBean.setxPath(xPath);
			handlerBean.setDirtyset(dirtyset);
			handlerBean.setModel(model);
			handlerBean.setRelativePathToRoot(relativePathToRoot);
			result = handler.compile(handlerBean, true);
			site.service().addCompileResult(crk, result);
			site.service().notifyFor();
			return true;
		} catch (Throwable t) {
			LOG.error("could not compile " + siteToCompile, t);
			site.service().addCompileResult(crk, CompileResult.ERROR);
		}
		return false;
	}

	private void recursive() {
		try {
			List<XPath> children = Utils.getNonHiddenChildren(site,
					siteToCompile);
			int sizeDocuments = Utils.countVisibleDocmuntes(children);
			Map<String, Object> model = new HashMap<>();
			model.put("document_size", sizeDocuments);
			for (XPath child : children) {
				site.service().compile(site, child.getPath(), model);
			}
		} catch (IOException e) {
			LOG.error("could not fetch children - " + e);
		}
	}

}
