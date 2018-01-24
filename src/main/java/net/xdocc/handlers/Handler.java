package net.xdocc.handlers;

import java.util.List;
import java.util.Map;
import net.xdocc.Cache;

import net.xdocc.XItem;
import net.xdocc.Site;
import net.xdocc.XPath;

public interface Handler {

    public boolean canHandle(Site site, XPath xPath);

    public List<String> knownExtensions();

    public XItem compile(Site site, XPath xPath, Map<String, Integer> filesCounter, Cache cache) throws Exception;
}
