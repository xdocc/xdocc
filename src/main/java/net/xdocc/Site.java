package net.xdocc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.xdocc.handlers.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.NullCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.xdocc.handlers.HandlerCopy;
import org.reflections.Reflections;

@Accessors(chain = true, fluent = true)
public class Site {

    private static final Logger LOG = LoggerFactory.getLogger(Site.class);

    @Getter @Setter
    private Path source;

    @Getter @Setter
    private Path generated;

    @Getter @Setter
    private List<Handler> handlers;

    @Getter @Setter
    private Properties properties;

    @Getter @Setter
    private Service service;

    @Getter @Setter
    private Configuration freemakerEngine;

    @Getter @Setter
    private Link globalNavigation;

    @Getter @Setter
    private Path templatePath;

    final private Map<String, TemplateBean> templates = new HashMap<>();

    public Site(Service service, Path source, Path generated) throws IOException {
        this(service, source, generated, findHandlers(), new Properties());
    }

    public Site(Service service, Path source, Path generated,
            List<Handler> handlers, Properties properties) throws IOException {
        this.service = service;
        this.source = source;
        this.generated = generated;
        this.handlers = handlers;
        this.properties = properties;
        this.templatePath = this.source.resolve(".templates");
        freemakerEngine = createTemplateEngine(templatePath);
        loadTemplates(templatePath);
        this.globalNavigation = loadGlobalNavigation();
    }

    public TemplateBean getTemplate(final String name, final String suffix)
            throws IOException {
        TemplateBean templateBean = templates.get(
                name + suffix + ".ftl");
        if (templateBean == null || templateBean.file() == null) {
            templateBean = templates.get(name + ".ftl");
            if (templateBean == null || templateBean.file() == null) {
                throw new FileNotFoundException("Template " + name
                        + ".ftl not found, there should be a file called "
                        + (source + "/.templates/" + name + suffix + ".ftl"));
            }
        }

        if (templateBean.isDirty()) {
            templateBean = loadTemplate(templateBean.file());
            templates.put(templateBean.file().getFileName().toString(), templateBean);
        }
        return templateBean;
    }

    private Configuration createTemplateEngine(Path templateDirectory)
            throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);
        // Specify the data source where the template files come from.
        // Here I set a file directory for it:
        if (Files.exists(templateDirectory)) {
            cfg.setDirectoryForTemplateLoading(templateDirectory.toFile());
            cfg.setCacheStorage(new NullCacheStorage());
            // Specify how templates will see the data-model. This is an
            // advanced topic...
            // but just use this:
            cfg.setObjectWrapper(new DefaultObjectWrapper(Configuration.VERSION_2_3_25));
        } else {
            LOG.warn("could not find the directory: {}", templateDirectory);
        }
        return cfg;
    }

    private void loadTemplates(Path templatePath) throws IOException {

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

    private TemplateBean loadTemplate(Path p) throws IOException {
        final FileTime fileTime = Files.getLastModifiedTime(p);
        final long filesize = Files.size(p);

        final Template template;
        synchronized (Utils.lock) {
            template = freemakerEngine.getTemplate(p.getFileName().toString());
        }

        TemplateBean templateBean = new TemplateBean()
                .file(p)
                .template(template)
                .timestamp(fileTime.toMillis())
                .filesize(filesize);

        return templateBean;
    }

    private Link loadGlobalNavigation() throws IOException {
        return loadNavigation(new XPath(this, source()));
    }

    public Link loadNavigation(XPath source) throws IOException {
        Link root = new Link(source, null);
        List<XPath> children = Utils.getNonHiddenChildren(this, source.path());
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
                loadNavigation(this, link, xPath.path());
            }
        }
        return root;
    }

    private void loadNavigation(Site site, Link parent, Path parentPath)
            throws IOException {
        List<XPath> children = Utils.getNonHiddenChildren(site, parentPath);

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
                loadNavigation(site, link, xPath.path());
            }
        }
    }

    public static class TemplateBean {

        @Getter @Setter
        private Template template;
        @Getter @Setter
        private long timestamp;
        @Getter @Setter
        private long filesize;
        @Getter @Setter
        private Path file;

        public boolean isDirty() {
            try {
                final FileTime fileTime = Files.getLastModifiedTime(this.file);
                final long filesize = Files.size(this.file);
                boolean dirty = this.timestamp != fileTime.toMillis()
                        || this.filesize != filesize;
                return dirty;
            } catch (IOException e) {
                LOG.info("file removed?: {}", file, e);
                return true;
            }
        }

    }

    public String getProperty(String key) {
        return properties.getProperty(key);
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
