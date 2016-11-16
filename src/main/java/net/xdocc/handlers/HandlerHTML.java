package net.xdocc.handlers;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;


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
	public Document compile(HandlerBean handlerBean, boolean writeToDisk)
			throws Exception {
		
		Charset charset = HandlerUtils.detectCharset(handlerBean.getxPath().path());
		String all = FileUtils.readFileToString(handlerBean.getxPath().path().toFile(), charset);
		
		org.jsoup.nodes.Document docj = Jsoup.parse(all);
		Elements e = docj.getElementsByTag("body");
		
		String htmlContent = e.toString();
		Document doc = Utils.createDocument(handlerBean.getSite(), handlerBean.getxPath(), handlerBean.getRelativePathToRoot(),
				htmlContent, "text", "file");
		// always create a single page for that
		Path generatedFile = null;
		if(writeToDisk) {
			generatedFile = handlerBean.getxPath().getTargetPath(handlerBean.getxPath().getTargetURL() + ".html");
			Utils.writeHTML(handlerBean.getSite(), handlerBean.getxPath(), handlerBean.getRelativePathToRoot(), doc, generatedFile, "single");
		}
		return doc;
	}
}