package net.xdocc;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
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

    // generic
    public static final String NAVIGATION = "globalnav";
    public static final String LOCALNAV = "localnav";
    public static final String LOCALNAV_ISCHILD = "ischildnav";
    public static final String CURRENT_NAV = "currentnav";
    public static final String PATH_TO_ROOT = "pathtoroot";
    public static final String PATH = "path";
    public static final String BREADCRUMB = "breadcrumb";
    public static final String CONTENT = "content";
    public static final String TEMPLATE = "template";
    public static final String LINK = "link";
    public static final String LINK_ORIGINAL = "linkoriganal";
    
    // list
    public static final String ITEMS = "items";
    public static final String ITEMS_SIZE = "documentsize";
    public static final String DEPTH = "depth";
    public static final String PROMOTE_DEPTH_ORIGINAL = "promotedepthoriginal";
    public static final String PROMOTE_DEPTH = "promotedepth";
   
    // Utils
    public static final String DEBUG = "debug";
    
    // Image
    public static final String URL_IMG_NORMAL = "imgurl";
    public static final String URL_IMG_THUMB = "imgurlthumb";
    public static final String URL_IMG_ORIG = "imgurlorig";

    private static final Logger LOG = LoggerFactory.getLogger(XItem.class);
    private static final long serialVersionUID = 136066054966377823L;
    
    private final Generator generator;
    private final XPath xPath;

    /**
     * Set the document. The name will be set to xPath.getName() as default.
     *
     * @param xPath The parsed path of the document. This is either a single file or a directory in case of a
     * collection of documents.
     * @param documentGenerator The generator is lazy generating. Thus, paths can be adapted until
     * getContent() is called.
     * @param url The full URL from the root to this xPath. To be used with relativePathToRoot
     */
    public XItem(XPath xPath, Generator documentGenerator) {
        this.generator = documentGenerator;
        this.xPath = xPath;
        initXPath();
        initNavigation();
        initDepth();
    }
    
    Generator documentGenerator() {
        return generator;
    }
    
    public XPath xPath() {
        return xPath;
    }
    
    private void initDepth() {
        generator.model().put(XItem.DEPTH, xPath.getTargetDepth());
    }
    
    private void initXPath() {
        generator.model().put(XPath.NAME, xPath.name());
        generator.model().put(XPath.URL, xPath.url());
        generator.model().put(XPath.DATE, xPath.date());
        generator.model().put(XPath.NR, xPath.nr());
        generator.model().put(XPath.ORIGINAL_PATH, xPath.originalPath());
        generator.model().put(XPath.ORIGINAL_PATH_TO_ROOT, xPath.originalPathToRoot());
        
        generator.model().put(XPath.FILENAME, xPath.fileName());
        generator.model().put(XPath.FILESCOUNT, xPath.filesCount());
        generator.model().put(XPath.FILESIZE, xPath.fileSize());
        generator.model().put(XPath.EXTENSIONS, xPath.extensions());
        generator.model().put(XPath.EXTENSION_LIST, xPath.extensionList());
        generator.model().put(XPath.PROPERTIES, xPath.properties());
        generator.model().put(XPath.PAGES, xPath.getPageSize());
        generator.model().put(XPath.LAYOUT, xPath.getLayoutSuffix());
        //
        generator.model().put(XPath.IS_ASCENDING, xPath.isAscending());
        generator.model().put(XPath.IS_AUTOSORT, xPath.isAutoSort());
        generator.model().put(XPath.IS_COMPILE, xPath.isCompile());
        generator.model().put(XPath.IS_DESCENDING, xPath.isDescending());
        generator.model().put(XPath.IS_DIRECTORY, xPath.isDirectory());
        generator.model().put(XPath.IS_HIDDEN, xPath.isHidden());
        generator.model().put(XPath.IS_HIGHLIGHT, xPath.isHighlight());
        generator.model().put(XPath.IS_NAVIGATION, xPath.isNavigation());
        generator.model().put(XPath.IS_NOINDEX, xPath.isNoIndex());
        generator.model().put(XPath.IS_COPY, xPath.isCopy());
        generator.model().put(XPath.IS_PAGE, xPath.isPage());
        generator.model().put(XPath.IS_PROMOTED, xPath.isPromoted());
        generator.model().put(XPath.IS_ROOT, xPath.isRoot());
        generator.model().put(XPath.IS_VISIBLE, xPath.isVisible());
        generator.model().put(XPath.IS_WRITE, xPath.isItemWritten());
        
        generator.model().put(XPath.IS_KEEP, xPath.isKeep());
    }
    
    private void initNavigation() {
        Link global = xPath.site().globalNavigation();
        generator.model().put(NAVIGATION, global.getChildren());
        Link local = xPath.site().loadLocalNavigation(xPath);
        generator.model().put(LOCALNAV_ISCHILD, Utils.isChild(global, local));
        generator.model().put(LOCALNAV, local.getChildren());
        List<Link> pathToRoot = Utils.linkToRoot(xPath.site().source(), xPath);
        generator.model().put(BREADCRUMB, pathToRoot);
        Link current = Utils.find(xPath.isDirectory()? xPath: xPath.getParent(), xPath.site().globalNavigation());
        generator.model().put(XItem.CURRENT_NAV, current);
    }
    
    /**
     * @return a list of documents if present in the model or null
     */
    public List<XItem> getItems() {
        @SuppressWarnings("unchecked")
        java.util.List<XItem> documents = (java.util.List<XItem>) documentGenerator()
                .model().get(ITEMS);
        if (documents == null) {
            return Collections.emptyList();
        }
        return documents;
    }

    /**
     * @param documents The list of documents in a collection
     * @return this class
     */
    public XItem setItems(List<XItem> documents) {
        documentGenerator().model().put(ITEMS, documents);
        documentGenerator().model().put(ITEMS_SIZE, documents.size());
        return this;
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
    
    public XItem setOriginalPath(String originalPath) {
        generator.model().put(XPath.ORIGINAL_PATH, originalPath);
        return this;
    }
    
    public String getOriginalPath() {
        return (String) generator.model().get(XPath.ORIGINAL_PATH);
    }
    
    public String getPath() {
        return (String) generator.model().get(PATH);
    }

    public XItem setPath(String path) {
        generator.model().put(PATH, path);
        return this;
    }
    
    public XItem setOriginalLink(String originalLink) {
        generator.model().put(LINK_ORIGINAL, originalLink);
        return this;
    }
    
    public String getOriginalLink() {
        return (String) generator.model().get(LINK_ORIGINAL);
    }
    
    public String getLink() {
        return (String) generator.model().get(LINK);
    }

    public XItem setLink(String link) {
        generator.model().put(LINK, link);
        return this;
    }
    
    public String getOriginalPathToRoot() {
        return (String) generator.model().get(XPath.ORIGINAL_PATH_TO_ROOT);
    }
    
    public String getPathToRoot() {
        return (String) generator.model().get(PATH_TO_ROOT);
    }

    public XItem setPathToRoot(String pathToRoot) {
        generator.model().put(PATH_TO_ROOT, pathToRoot);
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
    
    public void setDepth(Integer depth, Integer promoteDepth) {
        generator.model().put(DEPTH, depth);
        generator.model().put(PROMOTE_DEPTH_ORIGINAL, promoteDepth);
    }
    
    public Integer getDepth() {
        return (Integer) generator.model().get(DEPTH);
    }
    
    public Integer getPromoteDepthOriginal() {
        return (Integer) generator.model().get(PROMOTE_DEPTH_ORIGINAL);
    }
    
    public Integer getPromoteDepth() {
        return (Integer) generator.model().get(PROMOTE_DEPTH);
    }
    
    public XItem setPromoteDepth(Integer promoteDepth) {
        generator.model().put(PROMOTE_DEPTH, promoteDepth);
        return this;
    }

    @Override
    public int compareTo(XItem o) {
        return xPath.compareTo(o.xPath);
    }

    @Override
    public int hashCode() {
        return xPath.hashCode();
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
        sb.append(",u:");
            sb.append(xPath.originalPath());
        if (getFileName() != null) {
            sb.append(",f:");
            sb.append(getFileName());
        }
        return sb.toString();
    }    

    

    public void addItems(XItem doc) {
        List<XItem> items = (List<XItem>) documentGenerator().model().get(ITEMS);
        if(items == null) {
            items = new ArrayList<XItem>(1);
        }
        items.add(doc);
        setItems(items);
    }

    

    //public void setTemplateBean(Site.TemplateBean templateBean) {
    //    generator.templateBean(templateBean);
    //}
    
    public interface Generator {
        public String generate();
        public Map<String, Object> model();
        public Generator templateBean(Site.TemplateBean templateBean);
    }

    public static class EmptyGenerator implements Generator {
        final private static Map<String, Object> MODEL = new HashMap<String, Object>();
        @Override
        public String generate() {
            return "";
        }

        @Override
        public Map<String, Object> model() {
            return MODEL;
        }

        @Override
        public Generator templateBean(Site.TemplateBean templateBean) {
            return this;
        }
    }

    @Accessors(chain = true, fluent = true)
    public static class FillGenerator implements Generator, Serializable {

        private static final Logger LOG = LoggerFactory
                .getLogger(Generator.class);
        private static final long serialVersionUID = -8512427831292951263L;

        @Getter @Setter
        private Site.TemplateBean templateBean;

        @Getter
        final private Map<String, Object> model;
        final private Site site;

        public FillGenerator(Site site, Site.TemplateBean templateBean) {
            this.site = site;
            this.templateBean = templateBean;
            this.model = new HashMap<String, Object>();
        }

        public FillGenerator(Site site, Site.TemplateBean templateBean, Map<String, Object> model) {
            this(site, templateBean);
            this.model.putAll(model);
        }

        public String generate() {
            try {
                String html = Utils.applyTemplate(site, templateBean, model);
                html = Utils.postApplyTemplate(html, this.model, "path", "pathtoroot");
                return html;
            } catch (TemplateException | IOException e) {
                LOG.warn("cannot generate document {}. Model is {}",
                        templateBean.file().getFileName(), model, e);
                return null;
            }
        }
    }
}
