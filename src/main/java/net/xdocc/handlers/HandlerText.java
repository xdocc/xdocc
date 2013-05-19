package net.xdocc.handlers;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.xdocc.CompileResult;
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
	public CompileResult compile(Site site, XPath xPath, Set<Path> dirtyset, Map<String, Object> previousModel, String relativePathToRoot)
			throws Exception {
		
		Path generatedFile = xPath
				.getTargetPath(xPath.getTargetURL() + ".html");
		
		Charset charset = HandlerUtils.detectCharset(xPath.getPath());
		List<String> lines = Files.readAllLines(xPath.getPath(), charset);
		String htmlContent = convertHTML(lines);
		Document doc = Utils.createDocument(site, xPath, relativePathToRoot,
				htmlContent, "text");
		// always create a single page for that
		Utils.writeHTML(site, xPath, dirtyset, relativePathToRoot, doc, generatedFile, "single");
		return new CompileResult(doc, xPath.getPath(), generatedFile);
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