package net.xdocc.handlers;

import java.util.List;
import java.util.Map;

import net.xdocc.Document;
import net.xdocc.Site;
import net.xdocc.XPath;

public interface Handler {
	boolean canHandle(Site site, XPath xPath);
        List<String> knownExtensions();
        Document compile(Site site, XPath xPath, Map<String, Object> model, String relativePathToRoot) throws Exception;
}
