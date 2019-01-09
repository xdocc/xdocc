package net.xdocc;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.xdocc.handlers.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.NullCacheStorage;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.reflections.Reflections;

@Accessors(chain = true, fluent = true)
public class Site implements Serializable {

    private static final long serialVersionUID = 1713078455424279658L;
	private static final Logger LOG = LoggerFactory.getLogger(Site.class);

    @Getter @Setter
    private String source;

    @Getter @Setter
    private String generated;

    @Getter @Setter
    private Link globalNavigation;

    transient private Configuration freemakerEngine;
    transient private List<Handler> handlers;

    final private Map<String, TemplateBean> templates = new HashMap<>();

    public Site(Path source, Path generated) throws IOException {
        this.source = source.toString();
        this.generated = generated.toString();
        this.handlers = findHandlers();
        Path p=source.resolve(".templates");
        freemakerEngine = createTemplateEngine(p);
        loadTemplates(p);
        this.globalNavigation = loadGlobalNavigation();
    }

    public void init(Site site) {
        if(this.handlers == null) {
            this.handlers = site.handlers();
        }
        if(this.freemakerEngine == null) {
            this.freemakerEngine = site.freemakerEngine();
        }
    }
    
    public void reloadGlobalNavigation() throws IOException {
    	this.globalNavigation = loadGlobalNavigation();
    }
    
    public void reloadTemplates() throws IOException {
    	Path p=Paths.get(source).resolve(".templates");
    	loadTemplates(p);
    }

    public List<Handler> handlers() {
        return handlers;
    }

    public Configuration freemakerEngine() {
        return freemakerEngine;
    }

    public List<Path> templates() {
        List<Path> retVal = new ArrayList<>();
        for(TemplateBean t:templates.values()) {
            if(!t.internal()) {
                Path p = Paths.get(t.file());
                if(Files.exists(p)) {
                    retVal.add(p);
                }
            }
        }
        return retVal;
    }


    public TemplateBean getTemplate(final String name)
            throws IOException {
        TemplateBean templateBean = templates.get(name + ".ftl");
        
        if (templateBean == null) {
             throw new FileNotFoundException("Template " + name + ".ftl not found");
        }

        if (templateBean.isDirty()) {
            templateBean = loadTemplate(Paths.get(templateBean.file()));
            templates.put(templateBean.file(), templateBean);
        }
        return templateBean;
    }
    
    private Map<String, String> defaults() {
        final Map<String, String> map = new HashMap<String, String>();
        map.putAll(HandlerHTML.MAP);
        map.putAll(HandlerImage.MAP);
        map.putAll(HandlerLink.MAP);
        map.putAll(HandlerMarkdown.MAP);
        map.putAll(HandlerText.MAP);
        map.putAll(HandlerDirectory.MAP);
        map.putAll(HandlerWikiText.MAP);
        map.putAll(HandlerCopy.MAP);
        map.putAll(HandlerCommand.MAP);
        return map;
    }

    private Configuration createTemplateEngine(Path templateDirectory)
            throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);

        StringTemplateLoader stl = new StringTemplateLoader();
        for(Map.Entry<String,String> entry:defaults().entrySet()) {
            stl.putTemplate(entry.getKey(), entry.getValue());
        }

        if (Files.exists(templateDirectory)) {
            FileTemplateLoader ftl = new FileTemplateLoader(templateDirectory.toFile());
            cfg.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[]{ftl, stl}));

        } else {
            LOG.warn("could not find the directory: {}", templateDirectory);
            cfg.setTemplateLoader(new MultiTemplateLoader(new TemplateLoader[]{stl}));
        }
        cfg.setCacheStorage(new NullCacheStorage());
        cfg.setObjectWrapper(new DefaultObjectWrapper(Configuration.VERSION_2_3_28));
        return cfg;
    }

    private void loadTemplates(Path templatePath) throws IOException {
    	templates.clear();
    	if(Files.exists(templatePath)) {
    	    try (DirectoryStream<Path> ds = Files.newDirectoryStream(templatePath)) {
                for (Path p : ds) {
                    if (!Files.isRegularFile(p)) {
                        continue;
                    }
                    if (!p.toString().toLowerCase().endsWith(".ftl")) {
                        continue;
                    }
                    TemplateBean templateBean = loadTemplate(p);
                    templates.put(p.getFileName().toString(), templateBean);
                }
            }
        }
        for(Map.Entry<String,String> entry:defaults().entrySet()) {
            if(!templates.containsKey(entry.getKey())) {
                TemplateBean templateBean = new TemplateBean(this)
                        .file(entry.getKey()).internal(true);
                templates.put(entry.getKey(), templateBean);
            }
        }
    }

    private TemplateBean loadTemplate(Path p) throws IOException {
        final FileTime fileTime = Files.getLastModifiedTime(p);
        final long filesize = Files.size(p);

        TemplateBean templateBean = new TemplateBean(this)
                .file(p.toString())
                .timestamp(fileTime.toMillis())
                .filesize(filesize);

        return templateBean;
    }

    private Link loadGlobalNavigation() throws IOException {
        Path p = Paths.get(this.source);
        return loadNavigation(XPath.get(this, p));
    }
    
    public Link loadLocalNavigation(XPath source) /*throws IOException*/ {
        if(source.isDirectory()) {
            return loadNavigation(source);
        } else {
            return loadNavigation(source.getParent());
        }
    }

    private Link loadNavigation(XPath source) /*throws IOException*/ {
        Link root = new Link(source, null);
        List<XPath> children;
        try {
            Path p = Paths.get(source.path());
            children = Utils.getNonHiddenChildren(this, p);
        } catch (IOException ex) {
            LOG.error("cannot load navigation", ex);
            return root;
        }
        final boolean ascending;
        if (source.isAutoSort()) {
            ascending = Utils.guessAutoSort(children);
        } else {
            ascending = source.isAscending();
        }
        Utils.sort2(children, ascending);
        for (XPath xPath : children) {
            if (xPath.isNavigation()) {
                Link link = new Link(xPath, root);
                root.addChildren(link);
                loadNavigation(this, link, Paths.get(xPath.path()));
            }
        }
        return root;
    }

    private void loadNavigation(Site site, Link parent, Path parentPath)
            /**throws IOException*/ {
        List<XPath> children;
        try {
            children = Utils.getNonHiddenChildren(site, parentPath);
        } catch (IOException ex) {
            LOG.error("cannot load navigation", ex);
            return;
        }

        final boolean ascending;
        if (parent.getTarget().isAutoSort()) {
            ascending = Utils.guessAutoSort(children);
        } else {
            ascending = parent.getTarget().isAscending();
        }
        Utils.sort2(children, ascending);
        for (XPath xPath : children) {
            if (xPath.isNavigation()) {
                Link link = new Link(xPath, parent);
                parent.addChildren(link);
                loadNavigation(site, link, Paths.get(xPath.path()));
            }
        }
    }



    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Site)) {
            return false;
        }
        Site site = (Site) obj;
        return source.equals(site.source) && generated.equals(site.generated);
    }

    @Override
    public int hashCode() {
        return source.hashCode() ^ generated.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("site: src=");
        sb.append(source);
        sb.append(", gen=");
        sb.append(generated);
        return sb.toString();
    }
    
    /**
     * Search for file handlers of type 
     * @return 
     */
    private static List<Handler> findHandlers() {
        Reflections reflections = new Reflections("net.xdocc");
        Set<Class<? extends Handler>> subTypes = reflections.getSubTypesOf(Handler.class);
        final List<Handler> handlers = new ArrayList<>();
        boolean foundHandlerCopy = false;
        for (Class<? extends Handler> clazz : subTypes) {
            if (clazz.equals(HandlerCopy.class)) {
                foundHandlerCopy = true;
            } else {
                try {
                    handlers.add(clazz.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    LOG.error("failed to initialize handler {}", clazz.toString(), e);
                }
            }
        }
        //copy needs to go last, otherwise we will match all files and directories
        if(foundHandlerCopy) {
            try {
                    handlers.add(HandlerCopy.class.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    LOG.error("failed to initialize handler {}", HandlerCopy.class.toString(), e);
                }
        }
        return handlers;
    }
}
