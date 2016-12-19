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


import net.xdocc.Document;
import net.xdocc.Document.DocumentGenerator;
import net.xdocc.Site;
import net.xdocc.Site.TemplateBean;
import net.xdocc.XPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlerCopy implements Handler {
	private static final Logger LOG = LoggerFactory.getLogger(HandlerCopy.class);

	@Override
	public boolean canHandle(Site site, XPath xPath) {
		return true;
	}

	@Override
	public Document compile(Site site, XPath xPath, Map<String, Object> model2, 
                String relativePathToRoot) {
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
			if (xPath.getParent().isItemWritten()) {
				
                            Files.createDirectories(generatedFile.getParent());
                            Files.copy(xPath.path(), generatedFile, 
                                    StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS);
                            LOG.debug("copy {} to {}",xPath.path(), generatedFile);		
			}
			if (xPath.isRaw() || xPath.isVisible()) {
				return createDocumentBrowse(site, xPath,
						relativePathToRoot, model);
				
			} else if (xPath.isVisible()) {
				return createDocumentFile(site, xPath,
						relativePathToRoot, model);
			}
		} catch (IOException e) {
			LOG.error("Copy handler faild, cannot copy from {} to {}", xPath.path(), generatedFile, e);
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
				xPath.getTargetURLFilename());
		Date lastModified = new Date(Files.getLastModifiedTime(xPath.path())
				.toMillis());
		document.setDate(lastModified);
		document.setTemplate("file");
		return document;
	}

	public static Document createDocumentBrowse(Site site, XPath xPath,
			String path, Map<String, Object> model) throws IOException {
		TemplateBean templateText = site.getTemplate("browse", xPath.getLayoutSuffix());
		DocumentGenerator documentGenerator = new DocumentGenerator(site,
				templateText);
		Document document = new Document(xPath, documentGenerator,
				xPath.getTargetURL());
		Date lastModified = new Date(Files.getLastModifiedTime(xPath.path())
				.toMillis());
		document.setDate(lastModified);
		document.setName(xPath.getFileName());
		document.setTemplate("browse");
		return document;
	}

}
