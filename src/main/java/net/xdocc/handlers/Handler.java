package net.xdocc.handlers;

import java.util.List;

import net.xdocc.CompileResult;
import net.xdocc.Site;
import net.xdocc.XPath;

public interface Handler {
	public abstract boolean canHandle(Site site, XPath xPath);

	public abstract List<String> knownExtensions();

	/**
	 * 
	 * @param site
	 * @param siteToCompile
	 * @return
	 * @throws Exception
	 */
	public abstract CompileResult compile(HandlerBean handlerBean, boolean writeToDisk) throws Exception;

}
