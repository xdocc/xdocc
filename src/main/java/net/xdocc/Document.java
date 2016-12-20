package net.xdocc;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;

import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This document class represents the content of the document. It is used on the one hand to create the
 * document using the model and template from freemarker. On the other hand it can be passed to a freemarker
 * template. Thus, the model should match the methods.
 *
 * Available variables are: - name - url - content - date - nr - filename
 *
 * @author Thomas Bocek
 *
 */
public class Document implements Comparable<Document>, Serializable {

    // model constants for handlers
    
    
    public static final String RELATIVE = "relative";
    public static final String HIGHLIGHT = "highlight";
    public static final String DEPTH = "depth";
    public static final String PAGE_URLS = "page_urls";
    public static final String CURRENT_PAGE = "current_page";
    public static final String CONTENT = "content";
    // shared 
    public static final String TEMPLATE = "template";
    public static final String HIGHLIGHT_URL = "highlightUrl";
    
    
    
    // page
    public static final String DOCUMENT = "document";
    public static final String CURRENT = "current";
    public static final String BREADCRUMB = "breadcrumb";
    public static final String NAVIGATION = "navigation";

    // HandlerImage
    public static final String GROUP = "group";
    public static final String CSS_CLASS = "css_class";
    public static final String IMAGE_NORMAL = "image_normal";
    public static final String IMAGE_THUMB = "image_thumb";

    // HandlerWikiText
    public static final String HANDLER = "handler";

    // HandlerDirectory
    public static final String PAGE_NR = "page_nr";
    public static final String LOCAL_NAVIGATION = "local_navigation";

    // Utils
    public static final String DEBUG = "debug";

    private static final Logger LOG = LoggerFactory.getLogger(Document.class);
    private static final long serialVersionUID = 136066054966377823L;
    
    private final DocumentGenerator documentGenerator;
    
    private final XPath source;
    private final String url;

    /**
     * Set the document. The name will be set to xPath.getName() as default.
     *
     * @param xPath The parsed path of the document. This is either a single file or a directory in case of a
     * collection of documents.
     * @param documentGenerator The generator is lazy generating. Thus, paths can be adapted until
     * getContent() is called.
     * @param url The full URL from the root to this xPath. To be used with relativePathToRoot
     */
    public Document(XPath xPath, DocumentGenerator documentGenerator,
            String url) {
        this.documentGenerator = documentGenerator;
        this.source = xPath;
        this.url = url;
        initXPath();
    }
    
    DocumentGenerator documentGenerator() {
        return documentGenerator;
    }
    
    private void initXPath() {
        documentGenerator.model().put(XPath.NAME, source.name());
        documentGenerator.model().put(XPath.URL, source.url());
        documentGenerator.model().put(XPath.DATE, source.date());
        documentGenerator.model().put(XPath.NR, source.nr());
        documentGenerator.model().put(XPath.PATH, source.relativePath(documentGenerator.site));
        
        documentGenerator.model().put(XPath.FILENAME, source.fileName());
        documentGenerator.model().put(XPath.FILESCOUNT, source.filesCount());
        documentGenerator.model().put(XPath.FILESIZE, source.fileSize());
        documentGenerator.model().put(XPath.EXTENSIONS, source.extensions());
        documentGenerator.model().put(XPath.EXTENSION_LIST, source.extensionList());
        documentGenerator.model().put(XPath.PROPERTIES, source.properties());
        documentGenerator.model().put(XPath.PAGES, source.getPageSize());
        documentGenerator.model().put(XPath.LAYOUT, source.getLayoutSuffix());
        //
        documentGenerator.model().put(XPath.IS_ALL_VISIBLE, source.isAllVisible());
        documentGenerator.model().put(XPath.IS_ASCENDING, source.isAscending());
        documentGenerator.model().put(XPath.IS_AUTOSORT, source.isAutoSort());
        documentGenerator.model().put(XPath.IS_COMPILE, source.isCompile());
        documentGenerator.model().put(XPath.IS_DESCENDING, source.isDescending());
        documentGenerator.model().put(XPath.IS_DIRECTORY, source.isDirectory());
        documentGenerator.model().put(XPath.IS_HIDDEN, source.isHidden());
        documentGenerator.model().put(XPath.IS_HIGHLIGHT, source.isHighlight());
        documentGenerator.model().put(XPath.IS_LINK_PAGE, source.isLinkPage());
        documentGenerator.model().put(XPath.IS_LIST, source.isList());
        documentGenerator.model().put(XPath.IS_NAVIGATION, source.isNavigation());
        documentGenerator.model().put(XPath.IS_NONE_VISIBLE, source.isNoneVisible());
        documentGenerator.model().put(XPath.IS_PAGE, source.isPage());
        documentGenerator.model().put(XPath.IS_PROMOTED, source.isPromoted());
        documentGenerator.model().put(XPath.IS_RAW, source.isRaw());
        documentGenerator.model().put(XPath.IS_REGULAR_VISIBLE, source.isRegularVisible());
        documentGenerator.model().put(XPath.IS_ROOT, source.isRoot());
        documentGenerator.model().put(XPath.IS_SUMMARY, source.isSummary());
        documentGenerator.model().put(XPath.IS_VISIBLE, source.isVisible());
        documentGenerator.model().put(XPath.IS_WRITE, source.isItemWritten());
        
    }

    public String getName() {
        return (String) documentGenerator.model().get(XPath.NAME);
    }

    public Document setName(String name) {
        documentGenerator.model().put(XPath.NAME, name);
        return this;
    }

    public String getUrl() {
        return (String) documentGenerator.model().get(XPath.URL);
    }
    
    public Document setUrl(String url) {
        documentGenerator.model().put(XPath.URL, url);
        return this;
    }

    public Date getDate() {
        return (Date) documentGenerator.model().get(XPath.DATE);
    }

    public Document setDate(Date date) {
        documentGenerator.model().put(XPath.DATE, date);
        return this;
    }
    
    public long getNr() {
        return (long) documentGenerator.model().get(XPath.NR);
    }

    public Document setNr(long nr) {
        documentGenerator.model().put(XPath.NR, nr);
        return this;
    }
    
    public String getPath() {
        return (String) documentGenerator.model().get(XPath.PATH);
    }

    public Document setPath(String path) {
        documentGenerator.model().put(XPath.PATH, path);
        return this;
    }
    
    public String getFileName() {
        return (String) documentGenerator.model().get(XPath.FILENAME);
    }

    public Document setFileName(String fileName) {
        documentGenerator.model().put(XPath.FILENAME, fileName);
        return this;
    }
    
    public String getFileSize() {
        return (String) documentGenerator.model().get(XPath.FILESIZE);
    }

    public Document setFileSize(String fileSize) {
        documentGenerator.model().put(XPath.FILESIZE, fileSize);
        return this;
    }
    
    public String getFilesCount() {
        return (String) documentGenerator.model().get(XPath.FILESCOUNT);
    }

    public Document setFilesCount(long filesCount) {
        documentGenerator.model().put(XPath.FILESCOUNT, filesCount);
        return this;
    }

    

    /**
     * @return Return if document marked as highlight. Default is xPath.isHighlight()
     */
    public boolean getHighlight() {
        return BooleanUtils.isTrue((Boolean) documentGenerator.model().get(HIGHLIGHT));
    }

    /**
     * @param highlight Set if document marked as highlight. Default is xPath.isHighlight()
     * @param this class
     */
    public Document setHighlight(boolean highlight) {
        documentGenerator.model().put(HIGHLIGHT, highlight);
        return this;
    }

    

    /**
     *
     * @return The depth, i.e. the number of directories back to root
     */
    /*public Integer getDepth() {
        return (Integer) documentGenerator.model().get(DEPTH);
    }*/

    /**
     *
     * @param depth Set the number of directories back to root
     * @return
     */
    /*public Document setDepth(Integer depth) {
        documentGenerator.model().put(DEPTH, depth);
        return this;
    }*/

    /**
     * Lazy loading of the content that will be generated on the fly.
     *
     * @return The content that applies the model to the freemarker template
     */
    public String getGenerate() {
        return documentGenerator.generate();
    }

    /**
     * @return The content
     */
    public String getContent() {
        return (String) documentGenerator.model().get(CONTENT);
    }

    /**
     * @param path Set the content
     * @return this class
     */
    public Document setContent(String content) {
        documentGenerator.model().put(CONTENT, content);
        return this;
    }

    /**
     * @return The type
     */
    /*public String getType() {
        return (String) documentGenerator.model().get(TYPE);
    }*/

    /**
     * @param path Set the type
     * @return this class
     */
    /*public Document setType(String type) {
        documentGenerator.model().put(TYPE, type);
        return this;
    }*/

    /**
     * @return The template
     */
    public String getTemplate() {
        return (String) documentGenerator.model().get(TEMPLATE);
    }

    /**
     * @param path Set the template
     * @return this class
     */
    public Document setTemplate(String template) {
        documentGenerator.model().put(TEMPLATE, template);
        return this;
    }

    /**
     * @return The layout
     */
    public String getLayout() {
        return (String) documentGenerator.model().get(XPath.LAYOUT);
    }

    /**
     * @param path Set the layout
     * @return this class
     */
    public Document setLayout(String layout) {
        documentGenerator.model().put(XPath.LAYOUT, layout);
        return this;
    }

    /**
     * Print out all the available keys and a preview of the content
     *
     * @return The debug string in a HTML format
     */
    public String getDebug() {
        return Utils.getDebug(documentGenerator.model());
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
        if (getFileName() != null) {
            sb.append(",f:");
            sb.append(getFileName());
        }
        return sb.toString();
    }

    @Accessors(chain = true, fluent = true)
    public static class DocumentGenerator implements Serializable {

        private static final Logger LOG = LoggerFactory
                .getLogger(DocumentGenerator.class);
        private static final long serialVersionUID = -8512427831292951263L;

        @Getter
        final private Site.TemplateBean templateBean;

        @Getter
        final private Map<String, Object> model;
        final private Site site;

        public DocumentGenerator(Site site, Site.TemplateBean templateBean) {
            this.site = site;
            this.templateBean = templateBean;
            this.model = new HashMap<String, Object>();
        }

        public DocumentGenerator(Site site, Site.TemplateBean templateBean, Map<String, Object> model) {
            this(site, templateBean);
            this.model.putAll(model);
        }

        public String generate() {
            try {
                return Utils.applyTemplate(site, templateBean, model);
            } catch (TemplateException | IOException e) {
                LOG.warn("cannot generate document {}. Model is {}",
                        templateBean.file().getFileName(), model, e);
                return null;
            }
        }

        public DocumentGenerator copy() {
            DocumentGenerator documentGenerator = new DocumentGenerator(site, templateBean, model);
            return documentGenerator;
        }

    }

}
