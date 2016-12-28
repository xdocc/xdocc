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
public class XItem implements Comparable<XItem>, Serializable {

    // model constants for handlers
    public static final String NAVIGATION = "navigation";
    public static final String LOCALNAV = "localnav";
    
    
    public static final String RELATIVE = "relative";
    public static final String HIGHLIGHT = "highlight";
    public static final String DEPTH = "depth";
    public static final String PAGE_URLS = "page_urls";
    public static final String CURRENT_PAGE = "current_page";
    public static final String CONTENT = "content";
    //public static final String HTML = "html";
    // shared 
    public static final String TEMPLATE = "template";
    public static final String HIGHLIGHT_URL = "highlightUrl";
    
    
    
    // page
    public static final String DOCUMENT = "document";
    public static final String CURRENT = "current";
    public static final String BREADCRUMB = "breadcrumb";
    

    // HandlerImage
    public static final String GROUP = "group";
    public static final String CSS_CLASS = "css_class";
    public static final String IMAGE_NORMAL = "image_normal";
    public static final String IMAGE_THUMB = "image_thumb";

    // HandlerWikiText
    public static final String HANDLER = "handler";

    // HandlerDirectory
    public static final String PAGE_NR = "page_nr";
    

    // Utils
    public static final String DEBUG = "debug";

    private static final Logger LOG = LoggerFactory.getLogger(XItem.class);
    private static final long serialVersionUID = 136066054966377823L;
    
    private final Generator generator;
    
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
    public XItem(XPath xPath, Generator documentGenerator,
            String url) throws IOException {
        this.generator = documentGenerator;
        this.source = xPath;
        this.url = url;
        initXPath();
        initNavigation(xPath);
    }
    
    Generator documentGenerator() {
        return generator;
    }
    
    private void initXPath() {
        generator.model().put(XPath.NAME, source.name());
        generator.model().put(XPath.URL, source.url());
        generator.model().put(XPath.DATE, source.date());
        generator.model().put(XPath.NR, source.nr());
        generator.model().put(XPath.PATH, source.relativePath(generator.site));
        
        generator.model().put(XPath.FILENAME, source.fileName());
        generator.model().put(XPath.FILESCOUNT, source.filesCount());
        generator.model().put(XPath.FILESIZE, source.fileSize());
        generator.model().put(XPath.EXTENSIONS, source.extensions());
        generator.model().put(XPath.EXTENSION_LIST, source.extensionList());
        generator.model().put(XPath.PROPERTIES, source.properties());
        generator.model().put(XPath.PAGES, source.getPageSize());
        generator.model().put(XPath.LAYOUT, source.getLayoutSuffix());
        //
        generator.model().put(XPath.IS_ASCENDING, source.isAscending());
        generator.model().put(XPath.IS_AUTOSORT, source.isAutoSort());
        generator.model().put(XPath.IS_COMPILE, source.isCompile());
        generator.model().put(XPath.IS_DESCENDING, source.isDescending());
        generator.model().put(XPath.IS_DIRECTORY, source.isDirectory());
        generator.model().put(XPath.IS_HIDDEN, source.isHidden());
        generator.model().put(XPath.IS_HIGHLIGHT, source.isHighlight());
        generator.model().put(XPath.IS_NAVIGATION, source.isNavigation());
        generator.model().put(XPath.IS_NOINDEX, source.isNoIndex());
        generator.model().put(XPath.IS_COPY, source.isCopy());
        generator.model().put(XPath.IS_PAGE, source.isPage());
        generator.model().put(XPath.IS_PROMOTED, source.isPromoted());
        generator.model().put(XPath.IS_ROOT, source.isRoot());
        generator.model().put(XPath.IS_VISIBLE, source.isVisible());
        generator.model().put(XPath.IS_WRITE, source.isItemWritten());
        
    }
    
    private void initNavigation(XPath xPath) throws IOException {
        generator.model().put(NAVIGATION, xPath.site().globalNavigation().getChildren());
        Link local = xPath.site().loadNavigation(xPath);
        if(!xPath.site().globalNavigation().equals(local)) {
            generator.model().put(LOCALNAV, local.getChildren());
        }
    }

    public String getName() {
        return (String) generator.model().get(XPath.NAME);
    }

    public XItem setName(String name) {
        generator.model().put(XPath.NAME, name);
        return this;
    }

    public String getUrl() {
        return (String) generator.model().get(XPath.URL);
    }
    
    public XItem setUrl(String url) {
        generator.model().put(XPath.URL, url);
        return this;
    }

    public Date getDate() {
        return (Date) generator.model().get(XPath.DATE);
    }

    public XItem setDate(Date date) {
        generator.model().put(XPath.DATE, date);
        return this;
    }
    
    public long getNr() {
        return (long) generator.model().get(XPath.NR);
    }

    public XItem setNr(long nr) {
        generator.model().put(XPath.NR, nr);
        return this;
    }
    
    public String getPath() {
        return (String) generator.model().get(XPath.PATH);
    }

    public XItem setPath(String path) {
        generator.model().put(XPath.PATH, path);
        return this;
    }
    
    public String getFileName() {
        return (String) generator.model().get(XPath.FILENAME);
    }

    public XItem setFileName(String fileName) {
        generator.model().put(XPath.FILENAME, fileName);
        return this;
    }
    
    public String getFileSize() {
        return (String) generator.model().get(XPath.FILESIZE);
    }

    public XItem setFileSize(String fileSize) {
        generator.model().put(XPath.FILESIZE, fileSize);
        return this;
    }
    
    public String getFilesCount() {
        return (String) generator.model().get(XPath.FILESCOUNT);
    }

    public XItem setFilesCount(long filesCount) {
        generator.model().put(XPath.FILESCOUNT, filesCount);
        return this;
    }

    

    /**
     * @return Return if document marked as highlight. Default is xPath.isHighlight()
     */
    public boolean getHighlight() {
        return BooleanUtils.isTrue((Boolean) generator.model().get(HIGHLIGHT));
    }

    /**
     * @param highlight Set if document marked as highlight. Default is xPath.isHighlight()
     * @param this class
     */
    public XItem setHighlight(boolean highlight) {
        generator.model().put(HIGHLIGHT, highlight);
        return this;
    }

    public boolean getPromoted() {
        return BooleanUtils.isTrue((Boolean) generator.model().get(XPath.IS_PROMOTED));
    }
    
    public boolean getDirectory() {
        return BooleanUtils.isTrue((Boolean) generator.model().get(XPath.IS_DIRECTORY));
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
    public String getContent() {
        return generator.generate();
    }

    /**
     * @return The content
     */
    public String getHTML() {
        return (String) generator.model().get(CONTENT);
    }

    /**
     * @param path Set the content
     * @return this class
     */
    public XItem setHTML(String content) {
        generator.model().put(CONTENT, content);
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
        return (String) generator.model().get(TEMPLATE);
    }

    /**
     * @param path Set the template
     * @return this class
     */
    public XItem setTemplate(String template) {
        generator.model().put(TEMPLATE, template);
        return this;
    }

    /**
     * @return The layout
     */
    public String getLayout() {
        return (String) generator.model().get(XPath.LAYOUT);
    }

    /**
     * @param path Set the layout
     * @return this class
     */
    public XItem setLayout(String layout) {
        generator.model().put(XPath.LAYOUT, layout);
        return this;
    }

    /**
     * Print out all the available keys and a preview of the content
     *
     * @return The debug string in a HTML format
     */
    public String getDebug() {
        return Utils.getDebug(generator.model());
    }

    @Override
    public int compareTo(XItem o) {
        return source.compareTo(o.source);
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof XItem)) {
            return false;
        }
        XItem o = (XItem) obj;
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
    public static class Generator implements Serializable {

        private static final Logger LOG = LoggerFactory
                .getLogger(Generator.class);
        private static final long serialVersionUID = -8512427831292951263L;

        @Getter
        final private Site.TemplateBean templateBean;

        @Getter
        final private Map<String, Object> model;
        final private Site site;

        public Generator(Site site, Site.TemplateBean templateBean) {
            this.site = site;
            this.templateBean = templateBean;
            this.model = new HashMap<String, Object>();
        }

        public Generator(Site site, Site.TemplateBean templateBean, Map<String, Object> model) {
            this(site, templateBean);
            this.model.putAll(model);
        }

        public String generate() {
            try {
                /*if(model.containsKey(CONTENT)) {
                    model.put("generate", model.get(CONTENT));
                }*/
                return Utils.applyTemplate(site, templateBean, model);
            } catch (TemplateException | IOException e) {
                LOG.warn("cannot generate document {}. Model is {}",
                        templateBean.file().getFileName(), model, e);
                return null;
            }
        }
    }
}
