package net.xdocc.handlers;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	public abstract CompileResult compile(Site site, XPath xPath,
			Set<Path> dirtyset, Map<String, Object> model, String relativePathToRoot) throws Exception;

}
