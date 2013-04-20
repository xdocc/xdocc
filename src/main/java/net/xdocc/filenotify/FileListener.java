package net.xdocc.filenotify;

import java.util.List;

import net.xdocc.XPath;

public interface FileListener {
	public abstract void filesChanged(List<XPath> changedSet,
			List<XPath> createdSet, List<XPath> deletedSet);
}
