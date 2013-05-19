package net.xdocc.handlers;

import java.nio.charset.Charset;
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

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

public class HandlerHTML implements Handler {

	// final private static Logger LOG = LoggerFactory.getLogger(
	// HandlerText.class );

	@Override
	public boolean canHandle(Site site, XPath xPath) {
		return xPath.isCompile() && !xPath.isDirectory()
				&& HandlerUtils.knowsExtension(knownExtensions(), xPath);
	}
	
	@Override
	public List<String> knownExtensions() {
		return Arrays.asList(new String[] { "html", "HTML", "htm",
				"HTM" });
	}

	@Override
	public CompileResult compile(Site site, XPath xPath, Set<Path> dirtyset, Map<String, Object> previousModel, String relativePathToRoot)
			throws Exception {
		
		Path generatedFile = xPath
				.getTargetPath(xPath.getTargetURL() + ".html");
		
		Charset charset = HandlerUtils.detectCharset(xPath.getPath());
		String all = FileUtils.readFileToString(xPath.getPath().toFile(), charset);
		
		org.jsoup.nodes.Document docj = Jsoup.parse(all);
		Elements e = docj.getElementsByTag("body");
		
		String htmlContent = e.toString();
		Document doc = Utils.createDocument(site, xPath, relativePathToRoot,
				htmlContent, "text");
		// always create a single page for that
		Utils.writeHTML(site, xPath, dirtyset, relativePathToRoot, doc, generatedFile, "single");
		return new CompileResult(doc, xPath.getPath(), generatedFile);
	}
}