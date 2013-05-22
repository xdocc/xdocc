package net.xdocc;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This document class represents the content of the document. It is used on the
 * one hand to create the document using the model and template from freemarker.
 * On the other hand it can be passed to a freemarker template. Thus, the model
 * should match the methods.
 * 
 * @author Thomas Bocek
 * 
 */
public class Document implements Comparable<Document>, Serializable {

	private static final Logger LOG = LoggerFactory.getLogger(Document.class);
	private static final long serialVersionUID = 136066054966377823L;
	private final DocumentGenerator documentGenerator;
	private final Map<String, String> paths = new HashMap<>();
	private final XPath source;

	/**
	 * Set the document. The name will be set to xPath.getName() as default.
	 * 
	 * @param xPath
	 *            The parsed path of the document. This is either a single file
	 *            or a directory in case of a collection of documents.
	 * @param documentGenerator
	 *            The generator is lazy generating. Thus, paths can be adapted
	 *            until getContent() is called.
	 * @param url
	 *            The full URL from the root to this xPath. To be used with
	 *            relativePathToRoot
	 * @param relativePathToRoot
	 *            Set the relative path back to root. The path will look
	 *            something like this ../../
	 */
	public Document(XPath xPath, DocumentGenerator documentGenerator,
			String url, String relativePathToRoot) {
		this.documentGenerator = documentGenerator;
		this.source = xPath;
		setName(xPath.getName());
		setDate(xPath.getDate());
		setFilename(xPath.getFileName());
		setNr(xPath.getNr());
		setHighlight(xPath.isHighlight());
		initFilesize(xPath);
		// now set the paths
		addPath("url", url);
		addPath("highlightUrl", url);
		applyPath(relativePathToRoot);
		setPreview(false);
	}

	/**
	 * @param xPath
	 *            Sets the filesize of xPath
	 */
	private void initFilesize(XPath xPath) {
		if (!xPath.isDirectory()) {
			try {
				setFilesize(Files.size(xPath.getPath()));
			} catch (IOException e) {
				setFilesize(-1);
				if (LOG.isWarnEnabled()) {
					LOG.warn(
							"cannot get the file size, see syserr for more info",
							e);
				}
			}
		}
	}

	/**
	 * @return The xPath that represents this document
	 */
	public XPath getXPath() {
		return source;
	}

	/**
	 * @return The name of the document. Default is xPath.getName()
	 */
	public String getName() {
		return (String) documentGenerator.getModel().get("name");
	}

	/**
	 * @param name
	 *            The name of the document. Can be overwritten. Default is
	 *            xPath.getName()
	 * @return this class
	 */
	public Document setName(String name) {
		documentGenerator.getModel().put("name", name);
		return this;
	}

	/**
	 * @return The URL is always set using addPath() and modified by
	 *         applyPath().
	 */
	public String getUrl() {
		return (String) documentGenerator.getModel().get("url");
	}

	/**
	 * @return relative path from the link handler.
	 */
	public String getRelative() {
		return (String) documentGenerator.getModel().get("relative");
	}

	/**
	 * @param relative
	 *            Set the relative path in the link handler
	 * @return this class
	 */
	public Document setRelative(String relative) {
		documentGenerator.getModel().put("relative", relative);
		return this;
	}

	/**
	 * @return The date of the xPath. Default is xPath.getDate()
	 */
	public Date getDate() {
		return (Date) documentGenerator.getModel().get("date");
	}

	/**
	 * @param date
	 *            The date of the xPath. Can be overwritten. Default is
	 *            xPath.getDate()
	 * @return this class
	 */
	public Document setDate(Date date) {
		documentGenerator.getModel().put("date", date);
		return this;
	}

	/**
	 * @return The filename of xPath. Default is xPath.getFileName()
	 */
	public String getFilename() {
		return (String) documentGenerator.getModel().get("filename");
	}

	/**
	 * @param filename
	 *            The filename of xPath. Default is xPath.getFileName()
	 * @return this class
	 */
	public Document setFilename(String filename) {
		documentGenerator.getModel().put("filename", filename);
		return this;
	}

	/**
	 * @return The number of the document. Default is xPath.getNr().
	 */
	public long getNr() {
		return (long) documentGenerator.getModel().get("nr");
	}

	/**
	 * @param nr
	 *            The number of the document. Default is xPath.getNr().
	 * @return this class
	 */
	public Document setNr(long nr) {
		documentGenerator.getModel().put("nr", nr);
		return this;
	}

	/**
	 * @return Return if document marked as highlight. Default is
	 *         xPath.isHighlight()
	 */
	public boolean getHighlight() {
		return BooleanUtils.isTrue((Boolean) documentGenerator.getModel().get(
				"highlight"));
	}

	/**
	 * @param highlight
	 *            Set if document marked as highlight. Default is
	 *            xPath.isHighlight()
	 * @param this class
	 */
	public Document setHighlight(boolean highlight) {
		documentGenerator.getModel().put("highlight", highlight);
		return this;
	}

	/**
	 * @return The relative path to root
	 */
	public String getPath() {
		return (String) documentGenerator.getModel().get("path");
	}

	/**
	 * @param path
	 *            Set the relative path to root
	 * @return this class
	 */
	public Document setPath(String path) {
		documentGenerator.getModel().put("path", path);
		return this;
	}

	/**
	 * @return the file size if xPath is a file, or 0 if its a directory or
	 *         empty file.
	 */
	public long getSize() {
		Long val = (Long) documentGenerator.getModel().get("filesize");
		return val == null ? 0 : val;
	}

	/**
	 * @param size
	 *            Set the file size if xPath is a file
	 * @return this class
	 */
	public Document setFilesize(long filesize) {
		documentGenerator.getModel().put("filesize", filesize);
		return this;
	}

	public void increaseLevel() {

		Integer int0 = (int) documentGenerator.getModel().get("level");
		if (int0 == null) {
			documentGenerator.getModel().put("level", 1);
		} else {
			documentGenerator.getModel().put("level", int0 + 1);
		}
	}

	public int getLevel() {
		Integer int0 = (int) documentGenerator.getModel().get("level");
		return int0 == null ? 0 : int0;
	}

	/**
	 * Lazy loading of the content that will be generated on the fly.
	 * 
	 * @return The content that applies the model to the freemarker template
	 */
	public String getContent() {
		return documentGenerator.generate();
	}

	/**
	 * @return a list of documents if present in the model or null
	 */
	public List<Document> getDocuments() {
		@SuppressWarnings("unchecked")
		List<Document> documents = (List<Document>) documentGenerator
				.getModel().get("documents");
		return documents;
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

	public Boolean getPreview() {
		return BooleanUtils.isTrue((Boolean) documentGenerator.getModel().get(
				"preview"));
	}

	public void setPreview(boolean preview) {
		documentGenerator.getModel().put("preview", preview);
	}

	public void applyPath(String path) {
		setPath(path);
		for (Map.Entry<String, String> entry : paths.entrySet()) {
			documentGenerator.getModel().put(entry.getKey(),
					path + entry.getValue());
			documentGenerator.getModel().put(entry.getKey() + "_orig",
					entry.getValue());
		}
	}

	public void addPath(String key, String value) {
		paths.put(key, value);
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
			sb.append(",f:");
			sb.append(getFilename());
		}
		return sb.toString();
	}
}
