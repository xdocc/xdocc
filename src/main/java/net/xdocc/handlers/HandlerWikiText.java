package net.xdocc.handlers;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.xdocc.CompileResult;
import net.xdocc.Document;
import net.xdocc.DocumentGenerator;
import net.xdocc.Site;
import net.xdocc.Site.TemplateBean;
import net.xdocc.Utils;
import net.xdocc.XPath;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.mylyn.wikitext.confluence.core.ConfluenceLanguage;
import org.eclipse.mylyn.wikitext.core.parser.Attributes;
import org.eclipse.mylyn.wikitext.core.parser.ImageAttributes;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder;
import org.eclipse.mylyn.wikitext.mediawiki.core.MediaWikiLanguage;
import org.eclipse.mylyn.wikitext.textile.core.TextileLanguage;
import org.eclipse.mylyn.wikitext.tracwiki.core.TracWikiLanguage;
import org.eclipse.mylyn.wikitext.twiki.core.TWikiLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.TemplateException;

public class HandlerWikiText implements Handler {
	private static final Logger LOG = LoggerFactory
			.getLogger(HandlerWikiText.class);

	final private static String[] MEDIA_WIKI_EXT = { "mediawiki", "Mediawiki",
			"MediaWiki", "MEDIAWIKI" };
	final private static String[] TEXTILE_EXT = { "textile", "Textile",
			"TEXTILE" };
	final private static String[] TRAC_WIKI_EXT = { "tracwiki", "Tracwiki",
			"TracWiki", "TRACWIKI" };
	final private static String[] T_WIKI_EXT = { "twiki", "twiki", "tWiki",
			"TWIKI" };
	final private static String[] CONFLUENCE_EXT = { "confluence",
			"Confluence", "CONFLUENCE" };

	@Override
	public boolean canHandle(Site site, XPath xPath) {
		return xPath.isCompile() && !xPath.isDirectory()
				&& HandlerUtils.knowsExtension(knownExtensions(), xPath);
	}

	@Override
	public CompileResult compile(Site site, XPath xPath, Set<Path> dirtyset, Map<String, Object> previousModel, String relativePathToRoot)
			throws Exception {
		String path = Utils.relativePathToRoot(site.getSource(),
				xPath.getPath());

		// apply text ftl
		TemplateBean templateText = site.getTemplate(xPath.getLayoutSuffix(),
				"wikitext", xPath.getPath());

		String documentName = xPath.getName();
		String documentURL = xPath.getTargetURL() + ".html";
		Date documentDate = xPath.getDate();
		long documentNr = xPath.getNr();
		String documentFilename = xPath.getFileName();
		Map<String, Object> model = new HashMap<>();
		Utils.copyModelValues(model, previousModel, "document_size");
		model.put("preview", xPath.isPreview());
		HandlerUtils.fillModel(documentName,
				documentURL, documentDate, documentNr, documentFilename, null, model);

		// String htmlText = Utils.applyTemplate( templateText, model );
		// create the document
		Document doc = new Document(xPath, xPath.getName(),
				xPath.getTargetURL() + ".html", xPath.getDate(), xPath.getNr(),
				xPath.getFileName(), xPath.isHighlight(), path,
				new WikiTextDocumentGenerator(templateText, model, site, xPath,
						dirtyset));

		// create the site to layout ftl
		TemplateBean templateSite = site.getTemplate(xPath.getLayoutSuffix(),
				"document", xPath.getPath());
		Map<String, Object> modelSite = HandlerUtils
				.fillModel(site, xPath, doc);
		modelSite.put("type", "single");
		Utils.copyModelValues(modelSite, model, "document_size");
		model.put("preview", xPath.isPreview());
	
		String htmlSite = Utils.applyTemplate(site, templateSite, modelSite);
		// write to disk
		Path generatedFile = xPath
				.getTargetPath(xPath.getTargetURL() + ".html");
		dirtyset.add(generatedFile);
		Path generatedDir = Files.createDirectories(generatedFile.getParent());
		dirtyset.add(generatedDir);
		Utils.write(htmlSite, xPath, generatedFile);
		return new CompileResult(doc, xPath.getPath(), generatedFile);

	}

	@Override
	public List<String> knownExtensions() {
		return Arrays.asList(join(MEDIA_WIKI_EXT, TEXTILE_EXT, TRAC_WIKI_EXT,
				T_WIKI_EXT, CONFLUENCE_EXT));
	}

	public static String[] join(String[]... parms) {
		// calculate size of target array
		int size = 0;
		for (String[] array : parms) {
			size += array.length;
		}

		String[] result = new String[size];

		int j = 0;
		for (String[] array : parms) {
			for (String s : array) {
				result[j++] = s;
			}
		}
		return result;
	}

	private static class XdoccHtmlDocumentBuilder extends HtmlDocumentBuilder {
		final private Site site;
		final private Set<Path> dirtyset;
		final private XPath current;
		final private Map<String, Object> model;

		public XdoccHtmlDocumentBuilder(Writer out, Site site,
				Set<Path> dirtyset, XPath current,  Map<String, Object> model) {
			super(out);
			this.site = site;
			this.dirtyset = dirtyset;
			this.current = current;
			this.model = model;
		}

		@Override
		public void image(Attributes attributes, String url) {
			String path = Utils.relativePathToRoot(site.getSource(), current
					.getParent().getPath());
			/*
			 * if(!current.isVisible()) { super.image(attributes, url); }
			 */
			try {
				String[] parsedURL = url.split("/");
				// XPath xPath = Utils.findXPathFromURL(url, site,
				// current.getParent());
				List<XPath> founds = new ArrayList<>();
				founds.add(current.getParent());
				HandlerImage handlerImage = new HandlerImage();
				for (int i = 0; i < parsedURL.length; i++) {
					String pURL = parsedURL[i];
					List<XPath> result = new ArrayList<>();
					for (Iterator<XPath> iterator = founds.iterator(); iterator
							.hasNext();) {
						XPath found = iterator.next();
						String extension = parseExtension(pURL, handlerImage);
						String filename = parseURL(pURL, handlerImage);
						result.addAll(Utils.findChildURL(site, found, filename,
								extension));
					}
					founds = result;
				}
				if (founds.size() > 1) {
					LOG.warn("found more than one URLs for url=" + url + " ->"
							+ founds);
				} else if (founds.size() == 0
						|| (founds.size() > 0 && !founds.get(0).isVisible())) {
					super.image(attributes, url);
					return;
				} else {
					XPath found = founds.get(0);
					String relativePathToRoot = Utils.relativePathToRoot(site.getSource(),
							found.getPath());
					CompileResult compileResult = handlerImage.compile(site,
							found, dirtyset, (ImageAttributes) attributes, model, relativePathToRoot);
					String base = getBase() == null ? null : getBase()
							.toString();
					if (StringUtils.isEmpty(base)) {
						compileResult.getDocument().applyPath(path);
					} else {
						compileResult.getDocument()
								.applyPath(base + "/" + path);
					}
					super.charactersUnescaped(compileResult.getDocument()
							.getContent());
				}
			} catch (Exception e) {
				LOG.error("cannot create xdocc image " + e);
				e.printStackTrace();
			}
		}

		@Override
		public void imageLink(Attributes linkAttributes,
				Attributes imageAttributes, String href, String imageUrl) {
			// TODO Auto-generated method stub
			super.imageLink(linkAttributes, imageAttributes, href, imageUrl);
		}

		private String parseExtension(String pURL, HandlerImage handlerImage) {
			for (String extension : handlerImage.knownExtensions()) {
				if (pURL.endsWith("." + extension)) {
					return "." + extension;
				}
			}
			return null;
		}

		private String parseURL(String pURL, HandlerImage handlerImage) {
			for (String extension : handlerImage.knownExtensions()) {
				if (pURL.endsWith("." + extension)) {
					return pURL.substring(0, pURL.length() - extension.length()
							- 1);
				}
			}
			return pURL;
		}

	}

	private class WikiTextDocumentGenerator extends DocumentGenerator {
		
		private static final long serialVersionUID = -6008311072604987744L;
		final private Site site;
		final private XPath xPath;
		final private Set<Path> dirtyset;
		final private Map<String, Object> model;

		public WikiTextDocumentGenerator(TemplateBean templateText,
				Map<String, Object> model, Site site, XPath xPath,
				Set<Path> dirtyset) {
			super(site, templateText, model);
			this.site = site;
			this.xPath = xPath;
			this.dirtyset = dirtyset;
			this.model = model;
		}

		public String generate() throws TemplateException, IOException {
			fillHTML(site, xPath, dirtyset, "BLABAL");
			return Utils.applyTemplate(site, getTemplateText(), getModel());
		}

		private void fillHTML(Site site, XPath xPath, Set<Path> dirtyset,
				String linkRel) throws IOException {
			StringWriter writer = new StringWriter();
			HtmlDocumentBuilder builder = new XdoccHtmlDocumentBuilder(writer,
					site, dirtyset, xPath, model);
			try {
				if (getModel().containsKey("relative")) {
					String rel = (String) getModel().get("relative");
					builder.setBase(new URI(rel));
				}
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// avoid the <html> and <body> tags
			MarkupParser parser = null;
			builder.setEmitAsDocument(false);
			String type = null;
			for (String ext : TEXTILE_EXT) {
				if (xPath.getExtensionList().contains(ext)) {
					parser = new MarkupParser(new TextileLanguage());
					type = "textile";
				}
			}
			for (String ext : MEDIA_WIKI_EXT) {
				if (xPath.getExtensionList().contains(ext)) {
					parser = new MarkupParser(new MediaWikiLanguage());
					type = "mediawiki";
				}
			}
			for (String ext : TRAC_WIKI_EXT) {
				if (xPath.getExtensionList().contains(ext)) {
					parser = new MarkupParser(new TracWikiLanguage());
					type = "tracwiki";
				}
			}
			for (String ext : T_WIKI_EXT) {
				if (xPath.getExtensionList().contains(ext)) {
					parser = new MarkupParser(new TWikiLanguage());
					type = "twiki";
				}
			}
			for (String ext : CONFLUENCE_EXT) {
				if (xPath.getExtensionList().contains(ext)) {
					parser = new MarkupParser(new ConfluenceLanguage());
					type = "confluence";
				}
			}

			parser.setBuilder(builder);
			Charset charset = HandlerUtils.detectCharset(xPath.getPath());
			String rawFileContent = FileUtils.readFileToString(xPath.getPath()
					.toFile(), charset);
			parser.parse(rawFileContent);
			getModel().put("type", type);
			getModel().put("content", writer.toString());
		}

	}

}
