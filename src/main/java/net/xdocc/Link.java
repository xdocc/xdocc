package net.xdocc;

import com.google.common.base.Strings;
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
		return toStringRec("Link:");
	}
        
        public String toStringRec(String header) {
		StringBuilder sb = new StringBuilder(header);
                if(Strings.isNullOrEmpty(name)) {
                    sb.append(url);
                } else {
                    sb.append(name).append("|").append(url);
                }
                for(Link child:children) {
                    sb.append(child.toStringRec("\n+"));
                }        
                return sb.toString();
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isSelected() {
		return selected;
	}
}
