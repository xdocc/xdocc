package net.xdocc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.xdocc.handlers.Handler;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.NullCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

public class Site {
	private static final Logger LOG = LoggerFactory.getLogger(Site.class);

	final private Path source;

	final private Path generated;

	final private List<Handler> handlers;

	final private Properties properties;

	final private Service service;

	private Configuration freemakerEngine;

	private Link link;

	private Path templatePath;

	public Site(Service service, String source, String generated,
			List<Handler> handlers, Properties properties) throws IOException {
		this(service, Paths.get(filterXPath(source)), Paths
				.get(filterXPath(generated)), handlers, properties);
	}

	private static String filterXPath(final String path) throws IOException {
		final String PREFIX_FILE = "file:";
		final String PREFIX_XPATH = "xpath:";
		if (StringUtils.indexOfIgnoreCase(path.trim(), PREFIX_FILE) == 0) {
			return path.trim().substring(PREFIX_FILE.length());
		} else if (StringUtils.indexOfIgnoreCase(path.trim(), PREFIX_XPATH) == 0) {
			String pathStripped = path.trim().substring(PREFIX_XPATH.length());
			Path tmp = Paths.get(pathStripped);
			String xPath = tmp.getFileName().toString();
			Path parent = tmp.getParent();
			String foundxPath = Utils.searchXPath(parent, xPath);
			if (foundxPath != null) {
				return foundxPath;
			} else {
				LOG.warn("could not find the xpath [" + xPath + "] in "
						+ parent);
			}
		}
		return path;
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
		loadTemplates(templatePath, this);
	}

	public Path getTemplatePath() {
		return templatePath;
	}

	public Path getSource() {
		return source;
	}

	public XPath getSourceX() {
		return new XPath(this, source);
	}

	public Path getGenerated() {
		return generated;
	}

	public List<Handler> getHandlers() {
		return handlers;
	}

	public TemplateBean getTemplate(String suffix, String name, Path dependency)
			throws IOException {
		TemplateBean templateBean = service.getTemplateBeans(this).get(
				name + suffix + ".ftl");
		if (templateBean == null || templateBean.getFile() == null) {
			templateBean = service.getTemplateBeans(this).get(name + ".ftl");
			if (templateBean == null || templateBean.getFile() == null) {
				throw new FileNotFoundException("Template "
						+ name
						+ ".ftl"
						+ " not found, there should be a file called "
						+ (source + "/" + name + ".ftl" + " / " + (name
								+ ".ftl" + suffix)));
			} else {
				name = name + ".ftl";
			}
		} else {
			name = name + suffix + ".ftl";
		}

		if (templateBean.isDirty()) {
			Template template;
			synchronized (Utils.lock) {
				template = freemakerEngine.getTemplate(templateBean.getFile()
						.getFileName().toString());
			}
			templateBean.setTemplate(template);
			FileTime fileTime = Files.getLastModifiedTime(templateBean
					.getFile());
			long filesize = Files.size(templateBean.getFile());
			templateBean.setTimestamp(fileTime.toMillis());
			templateBean.setFilesize(filesize);

		}
		if (dependency != null) {
			templateBean.addDependency(dependency);
		}
		return templateBean;
	}

	private Configuration createTemplateEngine(Path templateDirectory)
			throws IOException {
		Configuration cfg = new Configuration();
		// Specify the data source where the template files come from.
		// Here I set a file directory for it:
		if (Files.exists(templateDirectory)) {
			cfg.setDirectoryForTemplateLoading(templateDirectory.toFile());
			cfg.setCacheStorage(new NullCacheStorage());
			// Specify how templates will see the data-model. This is an
			// advanced topic...
			// but just use this:
			cfg.setObjectWrapper(new DefaultObjectWrapper());
		} else {
			LOG.warn("could not find the directory: " + templateDirectory);
		}
		return cfg;
	}

	private void loadTemplates(Path source, final Site site) throws IOException {

		final Map<String, TemplateBean> tmps;
		// check if cache already created
		if (service.getTemplateBeans(site) == null) {
			tmps = new HashMap<String, TemplateBean>();
		} else {
			tmps = service.getTemplateBeans(site);
		}

		Files.walkFileTree(source, new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir,
					BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {

				if (file.getFileName().toString().toLowerCase()
						.endsWith(".ftl")) {

					// check if template cached
					TemplateBean templateBean = tmps.get(file.getFileName());
					if (templateBean != null) {
						return FileVisitResult.CONTINUE;
					}
					// template not cached
					templateBean = new TemplateBean();
					templateBean.setFile(file);
					Template template;
					synchronized (Utils.lock) {
						template = freemakerEngine.getTemplate(templateBean
								.getFile().getFileName().toString());
					}
					templateBean.setTemplate(template);
					FileTime fileTime = Files.getLastModifiedTime(templateBean
							.getFile());
					long filesize = Files.size(templateBean.getFile());
					templateBean.setTimestamp(fileTime.toMillis());
					templateBean.setFilesize(filesize);
					tmps.put(file.getFileName().toString(), templateBean);
					LOG.debug("adding template: "
							+ file.getFileName().toString()
							+ " to: " + site.toString());
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc)
					throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
					throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});

		service.addTemplateBeans(site, tmps);
	}

	public static class TemplateBean {

		private Template template;
		private Long timestamp;
		private long filesize;
		private Path file;
		private Collection<Path> dependencies = new HashSet<>();
		private Collection<Collection<Path>> forgeinDependencies = new HashSet<>();

		public Template getTemplate() {
			return template;
		}

		public boolean isDirty() {
			try {
				FileTime fileTime = Files.getLastModifiedTime(getFile());
				long filesize = Files.size(getFile());
				boolean dirty = getTimestamp() == null
						|| getTimestamp().longValue() != fileTime.toMillis()
						|| getFilesize() != filesize;
				return dirty;
			} catch (IOException e) {
				LOG.info("file removed?: " + file.toString());
				return true;
			}
		}

		public Long getTimestamp() {
			return timestamp;
		}

		public Path getFile() {
			return file;
		}

		public long getFilesize() {
			return filesize;
		}

		public Collection<Path> getFlatDependencies() {
			final Collection<Path> result;
			synchronized (dependencies) {
				result = new HashSet<>(dependencies);
			}
			synchronized (forgeinDependencies) {
				for (Collection<Path> tmp : forgeinDependencies) {
					result.addAll(tmp);
				}
			}
			return result;
		}

		public void addDependency(Path dependency) {
			synchronized (dependencies) {
				dependencies.add(dependency);
			}
		}

		public void addDependencies(TemplateBean parent) {
			final Collection<Path> tmp;
			synchronized (parent.dependencies) {
				tmp = new HashSet<>(parent.dependencies);
			}
			synchronized (forgeinDependencies) {
				forgeinDependencies.add(tmp);
			}
		}

		public void setTemplate(Template template) {
			this.template = template;
		}

		public void setTimestamp(Long timestamp) {
			this.timestamp = timestamp;
		}

		public void setFilesize(long filesize) {
			this.filesize = filesize;
		}

		public void setFile(Path file) {
			this.file = file;
		}

	}

	public void setNavigation(Link link) {
		this.link = link;
	}

	public Link getNavigation() throws IOException {
		if(link == null) {
			link = service.readNavigation(this);
		}
		return link;
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
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("site: src=");
		sb.append(source);
		sb.append(", gen=");
		sb.append(generated);
		return sb.toString();
	}

	public Service service() {
		return service;
	}
}
