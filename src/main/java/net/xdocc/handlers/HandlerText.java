package net.xdocc.handlers;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.xdocc.Cache;

import net.xdocc.XItem;
import net.xdocc.Site;
import net.xdocc.Utils;
import net.xdocc.XPath;

public class HandlerText implements Handler {

	// final private static Logger LOG = LoggerFactory.getLogger(
	// HandlerText.class );

	@Override
	public boolean canHandle(Site site, XPath xPath) {
		return xPath.isCompile() && !xPath.isDirectory()
				&& HandlerUtils.knowsExtension(knownExtensions(), xPath);
	}

	@Override
	public XItem compile(Site site, XPath xPath, Map<Path, Integer> filesCounter, Cache cache) throws Exception {
            
            final XItem doc;
            final Path generatedFile = xPath.resolveTargetFromBasePath(xPath.getTargetURL() + ".html");
            Cache.CacheEntry cached = cache.getCached(xPath);
            if (cached != null) {
                doc = cached.xItem();
                if (xPath.getParent().isItemWritten()) {
                    Utils.increase(filesCounter, Utils.listPaths(site, generatedFile));
                }
            } else {
                Charset charset = HandlerUtils.detectCharset(xPath.path());
		List<String> lines = Files.readAllLines(xPath.path(), charset);
		String htmlContent = convertHTML(lines);
		doc = Utils.createDocument(site, xPath, htmlContent, "text");
                if (xPath.getParent().isItemWritten()) {
		    Utils.writeHTML(xPath, doc, generatedFile);
                    Utils.increase(filesCounter, Utils.listPaths(site, generatedFile));
                }
                cache.setCached(xPath, doc, generatedFile);
            }
            return doc;
	}

	private String convertHTML(List<String> lines) {
		StringBuilder sb = new StringBuilder();
		int len = lines.size();
		for (int i = 0; i < len; i++) {
			sb.append(lines.get(i));
			if (i < (len - 1)) {
				sb.append("<br/>");
			}
		}
		return sb.toString();
	}

	@Override
	public List<String> knownExtensions() {
		return Arrays.asList(new String[] { "txt", "text", "TXT", "Text",
				"TEXT" });
	}
}