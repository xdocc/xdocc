package net.xdocc.handlers;

import java.util.List;
import java.util.Map;

import net.xdocc.Document;
import net.xdocc.Site;
import net.xdocc.XPath;

public interface Handler {
	public boolean canHandle(Site site, XPath xPath);

	public List<String> knownExtensions();

	/**
	 * 
	 * @param site
	 * @param siteToCompile
	 * @return
	 * @throws Exception
	 */
	public Document compile(Site site, XPath xPath, Map<String, Object> model, 
                String relativePathToRoot, boolean writeToDisk) throws Exception;

}
