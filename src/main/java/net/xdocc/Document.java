package net.xdocc;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.SetMultimap;

import freemarker.template.TemplateException;

public class Document implements Comparable<Document>, Serializable {
	// can never change

	private static final long serialVersionUID = 136066054966377823L;
	private final String filename;
	private final DocumentGenerator documentGenerator;
	private boolean highlight;
	private boolean preview;
	private final Map<String, Object> model;
	private final XPath source;
	// may change
	private int level = 0;
	private Map<String, String> paths = new HashMap<>();

	public Document(XPath source, String name, String url, Date date, long nr,
			String filename, boolean highlight, String path,
			DocumentGenerator documentGenerator) {
		this.model = documentGenerator.getModel();
		this.documentGenerator = documentGenerator;
		this.source = source;
		setPath(path);
		setName(name);
		paths.put("url", url);
		paths.put("highlightUrl", url);
		applyPath(path);
		setDate(date);
		this.filename = filename;
		this.highlight = highlight;
		setPreview(false);
	}

	private Document(XPath source, String filename,
			DocumentGenerator documentGenerator) {
		this.model = documentGenerator.getModel();
		this.documentGenerator = documentGenerator;
		this.filename = filename;
		this.source = source;
		setPreview(false);
	}

	public String getName() {
		return (String) model.get("name");
	}

	public void setName(String name) {
		model.put("name", name);
	}

	public String getUrl() {
		return (String) model.get("url");
	}

	public void applyPath(String path) {
		setPath(path);
		for (Map.Entry<String, String> entry : paths.entrySet()) {
			model.put(entry.getKey(), path + entry.getValue());
			model.put(entry.getKey() + "_orig", entry.getValue());
		}
	}

	public void addPath(String key, String value) {
		paths.put(key, value);
	}

	public void setValue(String key, String value) {
		model.put(key, value);
	}

	public Date getDate() {
		return (Date) model.get("date");
	}

	public void setDate(Date date) {
		model.put("date", date);
	}

	public boolean isHighlight() {
		return highlight;
	}

	public long getNr() {
		return (long) model.get("nr");
	}

	public void setNr(long nr) {
		model.put("nr", nr);
	}

	public long getSize() {
		return (long) model.get("size");
	}

	public void setSize(long size) {
		model.put("size", size);
	}

	public String getFilename() {
		return filename;
	}

	public void increaseLevel() {
		level++;
	}

	public int getLevel() {
		return level;
	}

	public String getContent() throws TemplateException, IOException {
		return documentGenerator.generate();
	}

	public DocumentGenerator getDocumentGenerator() {
		return documentGenerator;
	}

	@Override
	public int compareTo(Document o) {
		long diff = getNr() - o.getNr();
		if (diff != 0) {
			return diff > 0 ? 1 : -1;
		} else if (getName() != null && o.getName() != null) {
			return getName().compareTo(o.getName());
		} else {
			return getFilename().compareTo(o.getFilename());
		}
	}

	@Override
	public int hashCode() {
		int hash = new Long(getNr()).hashCode();
		hash = hash ^ getFilename().hashCode();
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Document)) {
			return false;
		}
		Document o = (Document) obj;
		return compareTo(o) == 0;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("doc:");
		if (getName() != null) {
			sb.append("n:");
			sb.append(getName());
		}
		if (getFilename() != null) {
			sb.append("f:");
			sb.append(getFilename());
		}
		return sb.toString();
	}

	public void setOriginalUrl(String originalUrl) {
		paths.put("url", originalUrl);
	}

	public void setHighlightUrl(String highlightUrl) {
		paths.put("highlightUrl", highlightUrl);
	}

	public String getHighlightUrl() {
		return paths.get("highlightUrl");
	}

	public void setHighlight(boolean highlight) {
		this.highlight = highlight;
	}

	public String getPath() {
		return (String) model.get("path");
	}

	public void setPath(String path) {
		model.put("path", path);
	}

	public Document copy() {
		Document document = new Document(source, filename,
				getDocumentGenerator().copy());
		document.highlight = highlight;
		document.preview = preview;
		return document;
	}

	public XPath getXPath() {
		return source;
	}

	public Boolean isPreview() {
		return (Boolean) model.get("preview");
	}

	public void setPreview(boolean preview) {
		model.put("preview", preview);
	}
}
