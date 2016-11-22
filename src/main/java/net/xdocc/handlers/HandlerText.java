package net.xdocc.handlers;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.xdocc.Document;
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
	public Document compile(Site site, XPath xPath, Map<String, Object> model, 
                String relativePathToRoot)
			throws Exception {
		Charset charset = HandlerUtils.detectCharset(xPath.path());
		List<String> lines = Files.readAllLines(xPath.path(), charset);
		String htmlContent = convertHTML(lines);
		Document doc = 
				Utils.createDocument(site, xPath, relativePathToRoot,
				htmlContent, "text");
		// always create a single page for that
		Path generatedFile = null;
		if (xPath.getParent().isItemWritten()) {
			generatedFile = xPath
					.getTargetPath(xPath.getTargetURL() + ".html");
			Utils.writeHTML(site, xPath, relativePathToRoot, doc, generatedFile);
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