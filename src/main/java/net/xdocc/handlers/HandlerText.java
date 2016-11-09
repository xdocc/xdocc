package net.xdocc.handlers;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

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
	public CompileResult compile(HandlerBean handlerBean, boolean writeToDisk)
			throws Exception {
		Charset charset = HandlerUtils.detectCharset(handlerBean.getxPath().path());
		List<String> lines = Files.readAllLines(handlerBean.getxPath().path(), charset);
		String htmlContent = convertHTML(lines);
		Document doc = 
				Utils.createDocument(handlerBean.getSite(), handlerBean.getxPath(), handlerBean.getRelativePathToRoot(),
				htmlContent, "text", "file");
		// always create a single page for that
		Path generatedFile = null;
		if (writeToDisk) {
			generatedFile = handlerBean.getxPath()
					.getTargetPath(handlerBean.getxPath().getTargetURL() + ".html");
			Utils.writeHTML(handlerBean.getSite(), handlerBean.getxPath(), handlerBean.getDirtyset(), handlerBean.getRelativePathToRoot(), doc, generatedFile, "single");
		}
		return new CompileResult(doc, handlerBean.getxPath().path(), handlerBean, this, generatedFile);
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