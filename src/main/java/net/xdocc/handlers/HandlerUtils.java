package net.xdocc.handlers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.xdocc.Document;
import net.xdocc.Link;
import net.xdocc.Site;
import net.xdocc.Utils;
import net.xdocc.XPath;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

public class HandlerUtils {
	final private static Object lock = new Object();

	public static boolean knowsExtension(List<String> knownExtensions,
			XPath xPath) {
		for (String extension : knownExtensions) {
			if (xPath.containsExtension(extension)) {
				return true;
			}
		}
		return false;
	}

	public static Charset detectCharset(Path path) throws IOException {
		Charset charset = detectCharset0(path);
		if (charset == null) {
			// set this as a default
			charset = Charset.forName("UTF-8");
		}
		return charset;
	}

	private static Charset detectCharset0(Path path) throws IOException {
		// the detection library is not threadsafe
		synchronized (lock) {
			try (@SuppressWarnings("resource")
			BufferedInputStream bis = new BufferedInputStream(
					Files.newInputStream(path))) {
				CharsetDetector cd = new CharsetDetector();
				cd.setText(bis);
				CharsetMatch[] cm = cd.detectAll();
				if (cm != null) {
					for(int i=0;i<cm.length;i++) {
						//preference goes always to UTF-8
						if("UTF-8".equals(cm[i].getName())) {
							return Charset.forName(cm[i].getName());
						}
					}
					if(cm.length > 0) {
						return Charset.forName(cm[0].getName());
					}
				}
				return null;
			}
		}
	}

	public static Map<String, Object> fillModel(String documentName,
			String documentURL, Date documentDate, long documentNr,
			String documentFilename, String htmlContent, Map<String, Object> model) {
		model.put("name", documentName);
		model.put("url", documentURL);
		model.put("content", htmlContent);
		model.put("date", documentDate);
		model.put("nr", documentNr);
		model.put("filename", documentFilename);
		return model;
	}

	public static Map<String, Object> fillPage(Site site, XPath xPath,
			Document document) throws IOException {
		String relativePathToRoot = Utils.relativePathToRoot(site.getSource(),
				xPath.getPath());
		Map<String, Object> model = new HashMap<>();
		model.put("document", document);
		Link current = Utils.find(xPath.getParent(), site.getNavigation());
		List<Link> pathToRoot = Utils.linkToRoot(site.getSource(), xPath);
		model.put("current", current);
		model.put("breadcrumb", pathToRoot);
		model.put("navigation", site.getNavigation());
		model.put("path", relativePathToRoot);
		return model;
	}
	
	public static List<Document> copy (List<Document> documents, String pathToRoot) {
		return copy(documents, 1, pathToRoot);
	}
	
	public static List<Document> copy (List<Document> documents, int level, String pathToRoot) {
		List<Document> retVal = new ArrayList<>();
		for(Document document:documents) {
			if(document.getDocuments()!=null) {
				document.setDocuments(copy(document.getDocuments(), level + 1, pathToRoot));
			}
			final Document docCopy;
			if(Boolean.TRUE.equals(document.getPreview())) {
				docCopy = document.copy(level + 1);
				System.err.println(System.identityHashCode(docCopy) + "==" + System.identityHashCode(document));
				//docCopy.applyPath(pathToRoot);
				//TODO:enable
				retVal.add(docCopy);
			} else {
				docCopy = document.copy(level);
				//docCopy.applyPath(pathToRoot);
				//TODO:enable
				retVal.add(docCopy);
			}
		}
		return retVal;
	}
}
