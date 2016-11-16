package net.xdocc.handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import net.xdocc.Document;
import net.xdocc.Document.DocumentGenerator;
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
	public Document compile(Site site, XPath xPath, Map<String, Object> model2, 
                String relativePathToRoot, boolean writeToDisk) {
		Map<String, Object> model = new HashMap<>(model2);
		final Path generatedFile;
		if (xPath.isVisible()) {
			String filename = xPath.getFileName();
			String extension = filename.substring(filename.lastIndexOf("."));
			generatedFile = xPath.getTargetPath(
					xPath.getTargetURL()+extension);
		} else {
			generatedFile = xPath.getTargetPath(
					xPath.getTargetURLFilename());
		}
		try {
			if (writeToDisk) {
				if (Files.isDirectory(xPath.path())) {
					Path generatedDir2 = Files.createDirectories(generatedFile);
					
					if (LOG.isDebugEnabled()) {
						LOG.debug("copy / create directory: " + generatedFile);
					}
				} else {
					Path generatedDir2 = Files.createDirectories(generatedFile
							.getParent());
					
					
					
						Files.copy(xPath.path(),
								generatedFile,
								StandardCopyOption.COPY_ATTRIBUTES,
								StandardCopyOption.REPLACE_EXISTING,
								StandardCopyOption.COPY_ATTRIBUTES,
								LinkOption.NOFOLLOW_LINKS);
					
					
					if (LOG.isDebugEnabled()) {
						LOG.debug("copy " + xPath.path()
								+ " to " + generatedFile);
					}
				}
			}
			if (xPath.isRaw()) {
				// new Date(Files.getLastModifiedTime( xPath.getPath()
				// ).toMillis())
				// create the document
				Document document = createDocumentBrowse(site,
						xPath,
						relativePathToRoot, model);
                                return document;
				
			} else if (xPath.isVisible()) {
				// we have a file with an unknown extension, but we want to have
				// it visible
				Document document = createDocumentFile(site,
						xPath,
						relativePathToRoot, model);
				return document;
			}
		} catch (IOException e) {
			LOG.error("Copy handler faild, cannot copy from "
					+ xPath.path() + " to " + generatedFile
					+ " - " + e);
		}
		
		
				
		return null;
	}

	@Override
	public List<String> knownExtensions() {
		return Arrays.asList(new String[0]);
	}

	public static Document createDocumentFile(Site site, XPath xPath,
			String path, Map<String, Object> model) throws IOException {
		TemplateBean templateText = site.getTemplate("file", xPath.getLayoutSuffix());
		DocumentGenerator documentGenerator = new DocumentGenerator(site,
				templateText);
		Document document = new Document(xPath, documentGenerator,
				xPath.getTargetURLFilename(), "file");
		Date lastModified = new Date(Files.getLastModifiedTime(xPath.path())
				.toMillis());
		document.setDate(lastModified);
		document.setTemplate("file");
		document.applyPath1(path);
		return document;
	}

	public static Document createDocumentBrowse(Site site, XPath xPath,
			String path, Map<String, Object> model) throws IOException {
		TemplateBean templateText = site.getTemplate("browse", xPath.getLayoutSuffix());
		DocumentGenerator documentGenerator = new DocumentGenerator(site,
				templateText);
		Document document = new Document(xPath, documentGenerator,
				xPath.getTargetURL(), "file");
		Date lastModified = new Date(Files.getLastModifiedTime(xPath.path())
				.toMillis());
		document.setDate(lastModified);
		document.setName(xPath.getFileName());
		document.setTemplate("file");
		document.applyPath1(path);
		return document;
	}

}
