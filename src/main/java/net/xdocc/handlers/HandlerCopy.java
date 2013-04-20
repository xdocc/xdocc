package net.xdocc.handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.xdocc.CompileResult;
import net.xdocc.Document;
import net.xdocc.DocumentGenerator;
import net.xdocc.Service;
import net.xdocc.Site;
import net.xdocc.Site.TemplateBean;
import net.xdocc.XPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlerCopy implements Handler {
	private static final Logger LOG = LoggerFactory
			.getLogger(HandlerCopy.class);

	@Override
	public boolean canHandle(Site site, XPath xPath) {
		// if we have the tag "copyall" then, handle directory by
		// HandlerDirectory since we want an index.html to be generated.
		// Otherwise we can handle it
		// boolean isCopyAll = (!xPath.isCopyAll() || !xPath.isDirectory());
		// boolean retVal = isCopyAll && (!xPath.isVisible() ||
		// xPath.isCopyAll()) && (!xPath.isRoot());
		// return retVal;
		return false;
	}

	@Override
	public CompileResult compile(Site site, XPath xPath, Set<Path> dirtyset, Map<String, Object> previousModel, String relativePathToRoot) {
		Map<String, Object> model = new HashMap<>(previousModel);
		final Path generatedFile;
		if (xPath.isVisible()) {
			generatedFile = xPath.getTargetPath(xPath.getTargetURLFilename());
		} else {
			generatedFile = xPath.getTargetPath(xPath.getTargetURL());
		}
		try {
			if (Files.isDirectory(generatedFile)) {
				Path generatedDir2 = Files.createDirectories(generatedFile);
				dirtyset.add(generatedDir2);
				if (LOG.isDebugEnabled()) {
					LOG.debug("copy / create directory: " + generatedFile);
				}
			} else {
				Path generatedDir2 = Files.createDirectories(generatedFile
						.getParent());
				dirtyset.add(generatedDir2);
				if (!Service.isCached(xPath.getPath(), generatedFile)) {
					Files.copy(xPath.getPath(), generatedFile,
							StandardCopyOption.COPY_ATTRIBUTES,
							StandardCopyOption.REPLACE_EXISTING,
							StandardCopyOption.COPY_ATTRIBUTES,
							LinkOption.NOFOLLOW_LINKS);
				}
				dirtyset.add(generatedFile);
				if (LOG.isDebugEnabled()) {
					LOG.debug("copy " + xPath.getPath() + " to "
							+ generatedFile);
				}
			}
			if (xPath.isCopyAll()) {
				// new Date(Files.getLastModifiedTime( xPath.getPath()
				// ).toMillis())
				// create the document
				Document document = createDocumentBrowse(site, xPath, relativePathToRoot, model);
				return new CompileResult(document, xPath.getPath(), generatedFile);
			} else if (xPath.isVisible()) {
				// we have a file with an unknown extension, but we want to have
				// it visible
				Document document = createDocumentFile(site, xPath, relativePathToRoot, model);
				return new CompileResult(document, xPath.getPath(), generatedFile);
			}
		} catch (IOException e) {
			LOG.error("Copy handler faild, cannot copy from " + xPath.getPath()
					+ " to " + generatedFile + " - " + e);
		}

		return new CompileResult(null, xPath.getPath(), generatedFile);
	}

	@Override
	public List<String> knownExtensions() {
		return Arrays.asList(new String[0]);
	}

	public static Document createDocumentFile(Site site, XPath xPath,
			String path, Map<String, Object> model) throws IOException {
		HandlerUtils.fillModel(xPath.getName(),
				xPath.getTargetURLFilename(), new Date(), 0,
				xPath.getFileName(), "", model);
		TemplateBean templateText = site.getTemplate(xPath.getLayoutSuffix(),
				"file", xPath.getPath());
		Date lastModified = new Date(Files.getLastModifiedTime(xPath.getPath())
				.toMillis());
		Document document = new Document(xPath, xPath.getName(),
				xPath.getTargetURLFilename(), lastModified, 0,
				xPath.getFileName(), false, path, new DocumentGenerator(site, 
						templateText, model));
		if (!xPath.isDirectory()) {
			document.setSize(Files.size(xPath.getPath()));
		}
		return document;
	}

	public static Document createDocumentBrowse(Site site, XPath xPath,
			String path, Map<String, Object> model) throws IOException {
		model = new HashMap<String, Object>();
		HandlerUtils.fillModel(xPath.getFileName(),
				xPath.getTargetURL(), new Date(), 0, xPath.getFileName(), "", model);
		TemplateBean templateText = site.getTemplate(xPath.getLayoutSuffix(),
				"browse", xPath.getPath());
		Date lastModified = new Date(Files.getLastModifiedTime(xPath.getPath())
				.toMillis());
		Document document = new Document(xPath, xPath.getFileName(),
				xPath.getTargetURL(), lastModified, 0, xPath.getFileName(),
				false, path, new DocumentGenerator(site, templateText, model));
		if (!xPath.isDirectory()) {
			document.setSize(Files.size(xPath.getPath()));
		}
		return document;
	}
}
