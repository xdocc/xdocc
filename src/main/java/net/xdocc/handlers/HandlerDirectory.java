package net.xdocc.handlers;

import net.xdocc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandlerDirectory implements Handler {

	private static final Logger LOG = LoggerFactory.getLogger(HandlerDirectory.class);

	@Override
	public boolean canHandle(Site site, XPath xPath) {return xPath.isDirectory() && xPath.isCopy();}

	@Override
	public XItem compile(Site site, XPath xPath, Map<Path, Integer> filesCounter, Cache cache) throws Exception {

	    final Path generatedFile;
		if (xPath.isVisible()) {
			String filename = xPath.fileName();
			String extension = filename.substring(filename.lastIndexOf("."));
			generatedFile = xPath.resolveTargetFromBasePath(
					xPath.getTargetURL() + extension);
		} else {
			generatedFile = xPath.resolveTargetFromBasePath(
					xPath.getTargetURLFilename());
		}

		Files.createDirectories(generatedFile.getParent());

		Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
		LOG.debug("copy {} to {}", xPath.path(), generatedFile);

		if (xPath.isCopy() || xPath.isVisible()) {
			XItem item = HandlerCopy.createDocumentBrowse(site, xPath, "");
			return item;
		}
        return null;
	}

    @Override
    public List<String> knownExtensions() {
        return Arrays.asList(new String[0]);
    }
}