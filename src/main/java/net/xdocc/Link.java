package net.xdocc;

import com.google.common.base.Strings;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class Link implements Serializable {

	private static final long serialVersionUID = 3858186167015394059L;

	final private String nr;
	final private String url;
	final private String name;
	final private List<Link> children = new ArrayList<>();
	final private Link parent;
	final private XPath target;
	final private Map<String, String> properties;
	@Setter
	private boolean selected;

	public Link(XPath target, Link parent) {
		this.target = target;
		this.url = target.getTargetURL();
		this.name = target.name() == null ? target.fileName() : target
				.name();
		this.parent = parent;
		this.properties = target.properties();
		this.nr = Long.toString(target.nr());
	}

	
	public void addChildren(Link link) {
		children.add(link);
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
        
        public Stream<Link> flattened() {
            return Stream.concat(
                    Stream.of(this),
                    children.stream().flatMap(Link::flattened));
        }
        public List<Link> flat() {
        	return flattened().collect(Collectors.toList());
        }
}
