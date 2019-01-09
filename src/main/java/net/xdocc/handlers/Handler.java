package net.xdocc.handlers;

import net.xdocc.Cache;
import net.xdocc.Site;
import net.xdocc.XItem;
import net.xdocc.XPath;

import java.util.List;
import java.util.Map;

public interface Handler {

    public boolean canHandle(Site site, XPath xPath);

    public List<String> knownExtensions();

    public XItem compile(Site site, XPath xPath, Map<String, Integer> filesCounter, Cache cache) throws Exception;
}
