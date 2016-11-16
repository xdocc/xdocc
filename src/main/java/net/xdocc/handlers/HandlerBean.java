package net.xdocc.handlers;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import net.xdocc.Site;
import net.xdocc.XPath;

public class HandlerBean {
	private Site site;
	private XPath xPath;
	private Map<String, Object> model;
	private String relativePathToRoot;
	private boolean forceCompile;
	
	public Site getSite() {
		return site;
	}
	public void setSite(Site site) {
		this.site = site;
	}
	public XPath getxPath() {
		return xPath;
	}
	public void setxPath(XPath xPath) {
		this.xPath = xPath;
	}
	
	public Map<String, Object> getModel() {
		return model;
	}
	public void setModel(Map<String, Object> model) {
		this.model = model;
	}
	public String getRelativePathToRoot() {
		return relativePathToRoot;
	}
	public void setRelativePathToRoot(String relativePathToRoot) {
		this.relativePathToRoot = relativePathToRoot;
	}
	public boolean isForceCompile() {
		return forceCompile;
	}
	public void setForceCompile(boolean forceCompile) {
		this.forceCompile = forceCompile;
	}
}
