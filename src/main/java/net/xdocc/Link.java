package net.xdocc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Link {
	final private String url;

	final private String name;

	final private List<Link> children = new ArrayList<>();

	final private Link parent;

	final private XPath target;

	final private Map<String, String> properties;

	private boolean selected;

	public Link(XPath target, Link parent) {
		this.target = target;
		this.url = target.getTargetURL();
		this.name = target.name() == null ? target.fileName() : target
				.name();
		this.parent = parent;
		this.properties = target.properties();
	}

	public String getUrl() {
		return url;
	}

	public String getName() {
		return name;
	}

	public void addChildren(Link link) {
		children.add(link);
	}

	public List<Link> getChildren() {
		return children;
	}

	public Link getParent() {
		return parent;
	}

	public XPath getTarget() {
		return target;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public String getProperty(String key) {
		return properties.get(key);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
                }
		if (obj == null) {
			return false;
                }
		if (getClass() != obj.getClass()) {
			return false;
                }
		Link other = (Link) obj;
		if (url == null) {
			if (other.url != null) {
				return false;
                        }
		} else if (!url.equals(other.url)) {
			return false;
                }
		if (name == null) {
			if (other.name != null) {
				return false;
                        }
		} else if (!name.equals(other.name)) {
			return false;
                }
                
		return children.equals(other.children);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Link:");
		return sb.append(name).append(",url:").append(url).toString();
	}

	public Link copy() {
		Link copy = new Link(target, parent);
		for (Link link : children) {
			copy.children.add(link.copy());
		}
		return copy;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isSelected() {
		return selected;
	}
}
