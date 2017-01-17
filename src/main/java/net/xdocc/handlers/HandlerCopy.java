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


import net.xdocc.XItem;
import net.xdocc.XItem.Generator;
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
	public XItem compile(Site site, XPath xPath, Map<String, Object> model2) {
		Map<String, Object> model = new HashMap<>(model2);
		final Path generatedFile;
		if (xPath.isVisible()) {
			String filename = xPath.fileName();
			String extension = filename.substring(filename.lastIndexOf("."));
			generatedFile = xPath.resolveTargetFromBasePath(
					xPath.getTargetURL()+extension);
		} else {
			generatedFile = xPath.resolveTargetFromBasePath(
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
			if (xPath.isCopy() || xPath.isVisible()) {
				return createDocumentBrowse(site, xPath,
						"", model);
				
			} else if (xPath.isVisible()) {
				return createDocumentFile(site, xPath,
						"", model);
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

	public static XItem createDocumentFile(Site site, XPath xPath,
			String path, Map<String, Object> model) throws IOException {
		TemplateBean templateText = site.getTemplate("file", xPath.getLayoutSuffix());
		Generator documentGenerator = new Generator(site,
				templateText);
		XItem document = new XItem(xPath, documentGenerator,
				xPath.getTargetURLFilename());
		Date lastModified = new Date(Files.getLastModifiedTime(xPath.path())
				.toMillis());
		document.setDate(lastModified);
		document.setTemplate("file");
		return document;
	}

	public static XItem createDocumentBrowse(Site site, XPath xPath,
			String path, Map<String, Object> model) throws IOException {
		TemplateBean templateText = site.getTemplate("browse", xPath.getLayoutSuffix());
		Generator documentGenerator = new Generator(site,
				templateText);
		XItem document = new XItem(xPath, documentGenerator,
				xPath.getTargetURL());
		Date lastModified = new Date(Files.getLastModifiedTime(xPath.path())
				.toMillis());
		document.setDate(lastModified);
		document.setName(xPath.fileName());
		document.setTemplate("browse");
		return document;
	}

}
