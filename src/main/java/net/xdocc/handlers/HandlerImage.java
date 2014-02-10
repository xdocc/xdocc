package net.xdocc.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import net.xdocc.CompileResult;
import net.xdocc.Document;
import net.xdocc.DocumentGenerator;
import net.xdocc.Site;
import net.xdocc.Site.TemplateBean;
import net.xdocc.Utils;
import net.xdocc.XPath;

import org.eclipse.mylyn.wikitext.core.parser.ImageAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.TemplateException;

public class HandlerImage implements Handler {

	private static final Logger LOG = LoggerFactory
			.getLogger(HandlerImage.class);

	@Override
	public boolean canHandle(Site site, XPath xPath) {
		return xPath.isCompile() && !xPath.isDirectory()
				&& HandlerUtils.knowsExtension(knownExtensions(), xPath);
	}

	public CompileResult compile(Site site, XPath xPath, Set<Path> dirtyset,
			ImageAttributes attributes, String relativePathToRoot,
			HandlerBean handlerBean, boolean writeToDisk)
			throws TemplateException, IOException, InterruptedException {
		// copy the original image
		Path generatedFile = xPath.getTargetPath(xPath.getTargetURL()
				+ xPath.getExtensions());
		dirtyset.add(generatedFile);
		Path generatedDir = Files.createDirectories(generatedFile.getParent());
		dirtyset.add(generatedDir);
		if (!site.service().isCached(xPath.getSite(), xPath.getPath(), generatedFile)) {
			Files.copy(xPath.getPath(), generatedFile,
					StandardCopyOption.COPY_ATTRIBUTES,
					StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.COPY_ATTRIBUTES,
					LinkOption.NOFOLLOW_LINKS);
		}
		dirtyset.add(generatedFile);
		// create a thumbnail
		String sizeIcon = Utils.searchPropertySizeIcon(xPath, site);
		Path generatedFileThumb = xPath.getTargetPath(xPath.getTargetURL()
				+ "_t" + xPath.getExtensions());
		if (!site.service().isCached(xPath.getSite(), xPath.getPath(), generatedFileThumb)) {
			if (sizeIcon.endsWith("c")) {
				cropResize(xPath, generatedFileThumb, stripMod(sizeIcon, "c"));
			} else {
				resize(xPath, generatedFileThumb, sizeIcon);
			}
		}
		dirtyset.add(generatedFileThumb);
		// create display size image
		String sizeNorm = Utils.searchPropertySizeNormal(xPath, site);

		Path generatedFileNorm = xPath.getTargetPath(xPath.getTargetURL()
				+ "_n" + xPath.getExtensions());
		if (!site.service().isCached(xPath.getSite(), xPath.getPath(), generatedFileNorm)) {
			if (sizeNorm.endsWith("c")) {
				cropResize(xPath, generatedFileNorm, stripMod(sizeNorm, "c"));
			} else {
				resize(xPath, generatedFileNorm, sizeNorm);
			}
		}
		dirtyset.add(generatedFileNorm);

		// apply text ftl
		TemplateBean templateText = site.getTemplate(xPath.getLayoutSuffix(),
				"image", xPath.getPath());
		String documentName = xPath.getName();
		String documentURL = xPath.getTargetURL() + xPath.getExtensions();
		Date documentDate = xPath.getDate();
		long documentNr = xPath.getNr();
		String documentFilename = xPath.getFileName();
		DocumentGenerator gen = new DocumentGenerator(site, templateText);
		Map<String, Object> model = gen.getModel();

		HandlerUtils.fillModel(documentName, documentURL, documentDate,
				documentNr, documentFilename, "", model);

		model.put("group", xPath.getParent().getTargetURL());
		if (attributes != null && attributes.getCssClass() != null) {
			model.put("css_class", attributes.getCssClass());
		} else {
			model.put("css_class", "image");
		}

		Document doc = new Document(xPath, gen, xPath.getTargetURL() + ".html",
				"file");
		doc.addPath("image_normal",
				xPath.getTargetURL() + "_n" + xPath.getExtensions());
		doc.addPath("image_thumb",
				xPath.getTargetURL() + "_t" + xPath.getExtensions());
		doc.applyPath1(relativePathToRoot);
		// TODO:enable
		doc.setTemplate("image");
		//
		// create the site to layout ftl
		TemplateBean templateSite = site.getTemplate(xPath.getLayoutSuffix(),
				"page", xPath.getPath());
		Map<String, Object> modelSite = HandlerUtils.fillPage(site, xPath, doc);
		modelSite.put("type", "file");
		String htmlSite = Utils.applyTemplate(site, templateSite, modelSite);
		// write to disk
		Path generatedFile2 = xPath.getTargetPath(xPath.getTargetURL()
				+ ".html");
		dirtyset.add(generatedFile2);
		Path generatedDir2 = Files
				.createDirectories(generatedFile2.getParent());
		dirtyset.add(generatedDir2);
		if (writeToDisk) {
			if (!site.service().isCached(xPath.getSite(), xPath.getPath(), generatedFile2)) {
				Utils.write(htmlSite, xPath, generatedFile2);
			}
		}
		return new CompileResult(doc, xPath.getPath(), handlerBean, this,
				generatedFile, generatedFileThumb, generatedFileNorm,
				generatedFile2);
	}

	@Override
	public CompileResult compile(HandlerBean handlerBean, boolean writeToDisk)
			throws Exception {
		return compile(handlerBean.getSite(), handlerBean.getxPath(),
				handlerBean.getDirtyset(), (ImageAttributes) null,
				handlerBean.getRelativePathToRoot(), handlerBean, writeToDisk);
	}

	@Override
	public List<String> knownExtensions() {
		return Arrays.asList(new String[] { "png", "PNG", "jpg", "jpeg", "JPG",
				"JPEG", "gif", "GIF" });
	}

	public static void resize(XPath xPath, Path generatedFile, String size)
			throws IOException, InterruptedException {
		executeConvert(xPath.toString(), size, generatedFile.toString());
	}

	public static void cropResize(XPath xPath, Path generatedFile, String size)
			throws IOException, InterruptedException {
		// String sizeRaw = stripMod(size, "^", "!", ">", "<");
		// String sizeMod = getMod(size, "^", "!", ">", "<");
		String sizeMod = "^";
		executeConvertCrop(xPath.toString(), size, sizeMod,
				generatedFile.toString());
	}

	private static void executeConvert(String image, String size,
			String resizedImage) throws IOException, InterruptedException {
		// START OS hack
		ProcessBuilder pb;
		switch (Utils.getOSType()) {
		case LINUX:
			pb = new ProcessBuilder("/usr/bin/convert", image, "-resize", size,
					resizedImage);
			break;
		case WIN:
			pb = new ProcessBuilder("/usr/bin/convert", image, "-resize", size,
					resizedImage);
			break;
		case MAC:
			StringTokenizer tokenizer = new StringTokenizer(size);
			String tmpWidth = tokenizer.nextToken("x");
			String tmpHeight = tokenizer.nextToken("x");
			image = image.replace("|", "\\|");
			image = image.replace(" ", "\\ ");
			image = image.replace(",", "\\,");
			image = image.replace("=", "\\=");
			resizedImage = resizedImage.replace("|", "\\|");
			resizedImage = resizedImage.replace(" ", "\\ ");
			resizedImage = resizedImage.replace(",", "\\,");
			resizedImage = resizedImage.replace("=", "\\=");
			pb = new ProcessBuilder("/bin/sh", "-c", "/usr/bin/sips "+image+" -z "+tmpHeight+" "+tmpWidth+" --out "+resizedImage+" > /dev/null");
			break;
		case OTHER:
			pb = new ProcessBuilder("/usr/bin/convert", image, "-resize", size,
					resizedImage);
			break;
		default:
			pb = new ProcessBuilder("/usr/bin/convert", image, "-resize", size,
					resizedImage);
			break;
		}
		// END OS hack
		executeAndOutput(pb);
	}

	public static void executeConvertCrop(String image, String size,
			String sizeMod, String resizedImage) throws IOException,
			InterruptedException {
		// START OS hack
		ProcessBuilder pb;
		switch (Utils.getOSType()) {
		case LINUX:
			pb = new ProcessBuilder("/usr/bin/convert", image, "-resize", size
					+ sizeMod, "-gravity", "center", "-crop", size + "+0+0",
					"+repage", resizedImage);
			break;
		case WIN:
			pb = new ProcessBuilder("/usr/bin/convert", image, "-resize", size
					+ sizeMod, "-gravity", "center", "-crop", size + "+0+0",
					"+repage", resizedImage);
			break;
		case MAC:
			StringTokenizer tokenizer = new StringTokenizer(size);
			String tmpWidth = tokenizer.nextToken("x");
			String tmpHeight = tokenizer.nextToken("x");
			image = image.replace("|", "\\|");
			image = image.replace(" ", "\\ ");
			image = image.replace(",", "\\,");
			image = image.replace("=", "\\=");
			resizedImage = resizedImage.replace("|", "\\|");
			resizedImage = resizedImage.replace(" ", "\\ ");
			resizedImage = resizedImage.replace(",", "\\,");
			resizedImage = resizedImage.replace("=", "\\=");
			pb = new ProcessBuilder("/bin/sh", "-c", "/usr/bin/sips "+image+" -c "+tmpHeight+" "+tmpWidth+" --out "+resizedImage+" > /dev/null");
			break;
		case OTHER:
			pb = new ProcessBuilder("/usr/bin/convert", image, "-resize", size
					+ sizeMod, "-gravity", "center", "-crop", size + "+0+0",
					"+repage", resizedImage);
			break;
		default:
			pb = new ProcessBuilder("/usr/bin/convert", image, "-resize", size
					+ sizeMod, "-gravity", "center", "-crop", size + "+0+0",
					"+repage", resizedImage);
			break;
		}
		// END OS hack
		executeAndOutput(pb);
	}

	private static void executeAndOutput(ProcessBuilder pb) throws IOException,
			InterruptedException {
		pb.redirectErrorStream(true);
		// START cmd log hack
//		String cmd = "";
//		for (String s : pb.command()) {
//			cmd += s + " ";
//		}
//		LOG.warn(cmd);
		// END cmd log hack

		Process p = pb.start();
		BufferedReader br = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		String line = null;
		while ((line = br.readLine()) != null) {
			LOG.error(line);
		}
		p.waitFor();
	}

	private static String stripMod(String size, String... mods) {
		for (String mod : mods) {
			size = size.replace(mod, "");
		}
		return size;
	}

}
