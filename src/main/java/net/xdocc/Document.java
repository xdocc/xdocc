package net.xdocc;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This document class represents the content of the document. It is used on the
 * one hand to create the document using the model and template from freemarker.
 * On the other hand it can be passed to a freemarker template. Thus, the model
 * should match the methods.
 * 
 * Available variables are: - name - url - content - date - nr - filename
 * 
 * @author Thomas Bocek
 * 
 */
public class Document implements Comparable<Document>, Serializable {

	private static final Logger LOG = LoggerFactory.getLogger(Document.class);
	private static final long serialVersionUID = 136066054966377823L;
	private final DocumentGenerator documentGenerator;
	private final Map<String, String> paths = new HashMap<String, String>();
	private final XPath source;
	private final String url;

	// private final String type;

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
			String url, String type) {
		this.documentGenerator = documentGenerator;
		this.source = xPath;
		this.url = url;
		setName(xPath.getName());
		setDate(xPath.getDate());
		setFilename(xPath.getFileName());
		setNr(xPath.getNr());
		setHighlight(xPath.isHighlight());
		initFilesize(xPath);
		// now set the paths
		addPath("url", url);
		addPath("highlightUrl", url);
		// applyPath1(relativePathToRoot);
		// setPath(relativePathToRoot);
		setPreview(false);
		setType(type);
		setLayout(xPath.getLayoutSuffix());
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
				LOG.debug("cannot get the file size (probably file renamed): "+xPath.getPath());
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
	 * 
	 * @return The depth, i.e. the number of directories back to root
	 */
	public Integer getDepth() {
		return (Integer) documentGenerator.getModel().get("depth");
	}

	/**
	 * 
	 * @param depth
	 *            Set the number of directories back to root
	 * @return
	 */
	public Document setDepth(Integer depth) {
		documentGenerator.getModel().put("depth", depth);
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

	/**
	 * Lazy loading of the content that will be generated on the fly.
	 * 
	 * @return The content that applies the model to the freemarker template
	 */
	public String getGenerate() {
		return documentGenerator.generate();
	}

	/**
	 * @return a list of documents if present in the model or null
	 */
	public List<Document> getDocuments() {
		@SuppressWarnings("unchecked")
		List<Document> documents = (List<Document>) documentGenerator
				.getModel().get("documents");
		if (documents == null) {
			return Collections.emptyList();
		}
		return documents;
	}

	/**
	 * @param documents
	 *            The list of documents in a collection
	 * @return this class
	 */
	public Document setDocuments(List<Document> documents) {
		documentGenerator.getModel().put("documents", documents);
		documentGenerator.getModel().put("document_size", documents.size());
		return this;
	}

	/**
	 * Set the data for paging. This is the the URLs for the other pages and the
	 * current site
	 * 
	 * @param pageURLs
	 * @param current
	 */
	public void setPaging(List<String> pageURLs, Integer current) {
		documentGenerator.getModel().put("page_urls", pageURLs);
		documentGenerator.getModel().put("current_page", current);

	}
	
	@SuppressWarnings("unchecked")
	public List<String> getPageURLs() {
		return (List<String>) documentGenerator.getModel().get("page_urls");
	}
	
	public Integer getCurrent() {
		return (Integer) documentGenerator.getModel().get("current_page");

	}

	/**
	 * @return The content
	 */
	public String getContent() {
		return (String) documentGenerator.getModel().get("content");
	}

	/**
	 * @param path
	 *            Set the content
	 * @return this class
	 */
	public Document setContent(String content) {
		documentGenerator.getModel().put("content", content);
		return this;
	}

	/**
	 * @return The type
	 */
	public String getType() {
		return (String) documentGenerator.getModel().get("type");
	}

	/**
	 * @param path
	 *            Set the type
	 * @return this class
	 */
	public Document setType(String type) {
		documentGenerator.getModel().put("type", type);
		return this;
	}

	/**
	 * @return The template
	 */
	public String getTemplate() {
		return (String) documentGenerator.getModel().get("template");
	}

	/**
	 * @param path
	 *            Set the template
	 * @return this class
	 */
	public Document setTemplate(String template) {
		documentGenerator.getModel().put("template", template);
		return this;
	}

	/**
	 * @return The layout
	 */
	public String getLayout() {
		return (String) documentGenerator.getModel().get("layout");
	}

	/**
	 * @param path
	 *            Set the layout
	 * @return this class
	 */
	public Document setLayout(String layout) {
		documentGenerator.getModel().put("layout", layout);
		return this;
	}

	/**
	 * Print out all the available keys and a preview of the content
	 * 
	 * @return The debug string in a HTML format
	 */
	public String getDebug() {
		return Utils.getDebug(documentGenerator.getModel());
	}

	/**
	 * Creates a copy and sets the level of the copy. If it has document inside
	 * a document, the level increases
	 * 
	 * @param level
	 *            The level of the document
	 * @return this class
	 */
	public Document copy(int level) {
		DocumentGenerator gen = documentGenerator.copy();
		Document document = new Document(source, gen, url, getType());
		document.paths.putAll(new HashMap<>(paths));
		document.setLevel(level);
		// document name may have changed, also other parameters
		document.setPreview(getPreview());
		document.setName(getName());
		document.setHighlight(getHighlight());
		document.setDate(getDate());
		document.setType(getType());
		document.setDepth(getDepth());
		document.setPaging(getPageURLs(), getCurrent());
		document.setRelative(getRelative());
		return document;
	}

	public Document copy() {
		return copy(getLevel());
	}

	/**
	 * @param level
	 *            Set the level of this document
	 * @return this class
	 */
	public Document setLevel(int level) {
		documentGenerator.getModel().put("level", level);
		return this;
	}

	/**
	 * @return The level of this document
	 */
	public int getLevel() {
		Integer int0 = (Integer) documentGenerator.getModel().get("level");
		return int0 == null ? 0 : int0;
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

	/**
	 * Applies a new path to all documents and sub documents.
	 * 
	 * @param path
	 *            The path to apply
	 */
	public Document applyPath1(String path) {

		applyPath(path, new HashSet<Document>());
		return this;
	}

	/**
	 * To avoid a stack overflow, we need to know which document we already
	 * visited.
	 * 
	 * @param path
	 *            The path to apply
	 * @param seen
	 *            The documents we have already seen.
	 */
	private void applyPath(String path, Set<Document> seen) {
		if (seen.contains(this)) {
			// we have already processed this.
			return;
		}
		seen.add(this);
		setPath(path);
		setDepth(StringUtils.countMatches(path, "../"));
		for (Map.Entry<String, String> entry : paths.entrySet()) {
			documentGenerator.getModel().put(entry.getKey(),
					path + entry.getValue());
			documentGenerator.getModel().put(entry.getKey() + "_orig",
					entry.getValue());
		}

		if (getDocuments() != null) {
			for (Document doc : getDocuments()) {
				doc.applyPath(path, seen);
			}
		}
		if (getCompleteDocument() != null) {
			getCompleteDocument().applyPath(path, seen);
		}
	}

	public void addPath(String key, String value) {
		paths.put(key, value);
	}

	@Override
	public int compareTo(Document o) {
		return source.compareTo(o.source);
	}

	@Override
	public int hashCode() {
		return source.hashCode();
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

	/**
	 * @param documentFull
	 *            Set the full document. If this is set, preview is always true.
	 * @return this class
	 */
	public Document setCompleteDocument(Document documentFull) {
		documentGenerator.getModel().put("complete_document", documentFull);
		return this;

	}

	/**
	 * @return The the full document. If this is set, preview is always true.
	 */
	public Document getCompleteDocument() {
		return (Document) documentGenerator.getModel().get("complete_document");
	}

}
