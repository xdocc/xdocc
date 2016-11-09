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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.xdocc.CompileResult;
import net.xdocc.CompileResult.Key;
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
	public CompileResult compile(HandlerBean handlerBean, boolean writeToDisk)
			throws Exception {
		
		String path = handlerBean.getRelativePathToRoot();

		// edit for link special
		Key<Path> crk = new Key<Path>(handlerBean.getxPath().getPath(),	handlerBean.getxPath().getPath());
		
		// apply text ftl
		TemplateBean templateText = handlerBean.getSite().getTemplate(
				"wikitext", handlerBean.getxPath().getLayoutSuffix());

		// String htmlText = Utils.applyTemplate( templateText, model );
		// create the document
		DocumentGenerator documentGenerator = new WikiTextDocumentGenerator(
				templateText, handlerBean.getSite(), handlerBean.getxPath(),
				handlerBean.getDirtyset(), handlerBean, writeToDisk);
		Document doc = new Document(handlerBean.getxPath(), documentGenerator,
				handlerBean.getxPath().getTargetURL() + ".html", "file");
		doc.setPreview(handlerBean.getxPath().isSummary());
		doc.setTemplate("wikitext");
		doc.applyPath1(path);
		// create the site to layout ftl
		TemplateBean templateSite = handlerBean.getSite().getTemplate(
				"page", handlerBean.getxPath().getLayoutSuffix());
		Map<String, Object> model = HandlerUtils.fillPage(
				handlerBean.getSite(), handlerBean.getxPath(), doc);
		model.put(Document.TYPE, "document");

		String htmlSite = Utils.applyTemplate(handlerBean.getSite(),
				templateSite, model);
		Path generatedFile = null;
		// write to disk
		if (writeToDisk) {
			
			generatedFile = handlerBean.getxPath().getTargetPath(handlerBean.getxPath().getTargetURL() + ".html");
			handlerBean.getDirtyset().add(generatedFile);
			Path generatedDir = Files.createDirectories(generatedFile
					.getParent());
			handlerBean.getDirtyset().add(generatedDir);
			Utils.write(htmlSite, handlerBean.getxPath(), generatedFile);
		}
		return new CompileResult(doc, handlerBean.getxPath().getPath(),
				handlerBean, this, generatedFile);

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
		final private HandlerBean handlerBean;
		final private boolean writeToDisk;

		public XdoccHtmlDocumentBuilder(Writer out, Site site,
				Set<Path> dirtyset, XPath current, HandlerBean handlerBean,
				boolean writeToDisk) {
			super(out);
			this.site = site;
			this.dirtyset = dirtyset;
			this.current = current;
			this.handlerBean = handlerBean;
			this.writeToDisk = writeToDisk;
		}

		@Override
		public void image(Attributes attributes, String url) {
			String path = Utils.relativePathToRoot(site.source(), current
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
					String relativePathToRoot = Utils.relativePathToRoot(
							site.source(), found.getPath());
					CompileResult compileResult = handlerImage.compile(site,
							found, dirtyset, (ImageAttributes) attributes,
							relativePathToRoot, handlerBean, writeToDisk);
					String base = getBase() == null ? null : getBase()
							.toString();
					if (StringUtils.isEmpty(base)) {
						compileResult.getDocument().applyPath1(path);
						// TODO:enable
					} else {
						compileResult.getDocument().applyPath1(
								base + "/" + path);
						// TODO:enable
					}
					super.charactersUnescaped(compileResult.getDocument()
							.getGenerate());
				}
			} catch (Exception e) {
				LOG.error("cannot create xdocc image " + e);
				e.printStackTrace();
			}
		}
		
		/**
		 * 
		 */
		@Override
		protected void emitAnchorHref(String href) {
			if(!href.startsWith("http")) {
				href = href.replace("../", "");
				String value =  makeUrlAbsolute(handlerBean.getRelativePathToRoot() + href);
//				LOG.info("rPath: "+handlerBean.getRelativePathToRoot()+" href: "+href+" --- WRITING URL: "+value);
				writer.writeAttribute("href", value); //$NON-NLS-1$
			} else {
				super.emitAnchorHref(href);
			}
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
		final private HandlerBean handlerBean;
		final private boolean writeToDisk;

		public WikiTextDocumentGenerator(TemplateBean templateText, Site site,
				XPath xPath, Set<Path> dirtyset, HandlerBean handlerBean,
				boolean writeToDisk) {
			super(site, templateText, handlerBean.getModel());
			this.site = site;
			this.xPath = xPath;
			this.dirtyset = dirtyset;
			this.handlerBean = handlerBean;
			this.writeToDisk = writeToDisk;
		}

		public String generate() {
			try {
				fillHTML(site, xPath, dirtyset, "BLABAL", handlerBean,
						writeToDisk);
				return Utils.applyTemplate(site, templateBean(), model());
			} catch (IOException | TemplateException e) {
				if (LOG.isWarnEnabled()) {
					LOG.warn("cannot generate wiki document "
							+ templateBean().file().getFileName()
							+ ". Model is " + model(), e);
				}
				return null;
			}

		}

		private void fillHTML(Site site, XPath xPath, Set<Path> dirtyset,
				String linkRel, HandlerBean handlerBean, boolean writeToDisk)
				throws IOException {
			StringWriter writer = new StringWriter();
			HtmlDocumentBuilder builder = new XdoccHtmlDocumentBuilder(writer,
					site, dirtyset, xPath, handlerBean, writeToDisk);

			try {
				if (model().containsKey(Document.RELATIVE)) {
					String rel = (String) model().get(Document.RELATIVE);
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
			model().put(Document.HANDLER, type);
			model().put(Document.CONTENT, writer.toString());
		}

		@Override
		public String toString() {
			return "WIKI" + super.toString();
		}

	}

}
