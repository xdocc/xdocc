package net.xdocc.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import net.xdocc.XItem;
import net.xdocc.Site;
import net.xdocc.Site.TemplateBean;
import net.xdocc.Utils;
import net.xdocc.XPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.TemplateException;
import net.xdocc.XItem.Generator;

public class HandlerImage implements Handler {

    private static final Logger LOG = LoggerFactory
            .getLogger(HandlerImage.class);

    @Override
    public boolean canHandle(Site site, XPath xPath) {
        return xPath.isCompile() && !xPath.isDirectory()
                && HandlerUtils.knowsExtension(knownExtensions(), xPath);
    }

    @Override
    public XItem compile(Site site, XPath xPath)
            throws TemplateException, IOException, InterruptedException {

        Path generatedFile = xPath.resolveTargetFromBasePath(xPath.getTargetURL() + xPath.extensions());
        Files.createDirectories(generatedFile.getParent());

        TemplateBean templateTextTop = site.getTemplate("image", xPath.getLayoutSuffix());
        Generator genTop = new XItem.FillGenerator(site, templateTextTop);
        XItem docTop = new XItem(xPath, genTop);

        XItem doc = convertOrig(xPath, generatedFile, site, false);
        docTop.addItems(doc);

        // create a thumbnail
        doc = convertThumb(site, xPath, false);
        docTop.addItems(doc);

        // create display size image
        doc = convertNorm(site, xPath, false);
        docTop.addItems(doc);

        return docTop;
    }

    public XItem convertNorm(Site site, XPath xPath, boolean neverWriteToDisk)
            throws InterruptedException, TemplateException, IOException {

        String sizeNorm = xPath.getRecursiveProperty("size_normal", "sn");
        if (sizeNorm == null) {
            sizeNorm = "800x600^";
        }

        if (!sizeNorm.startsWith("0x") && site.hasExactTemplate("image_norm", xPath.getLayoutSuffix())) {
            TemplateBean templateText = site.getTemplate("image_norm", xPath.getLayoutSuffix());
            Generator gen = new XItem.FillGenerator(site, templateText);
            XItem doc = new XItem(xPath, gen);

            Path generatedFileNorm = xPath.resolveTargetFromBasePath(xPath.getTargetURL()
                    + "_n" + xPath.extensions());

            if (sizeNorm.endsWith("c")) {
                cropResize(xPath, generatedFileNorm, stripMod(sizeNorm, "c"));
            } else {
                resize(xPath, generatedFileNorm, sizeNorm);
            }
            doc.setTemplate("image_norm");
            doc.setOriginalPath(xPath.getTargetURL() + "_n" + xPath.extensions());

            if (!neverWriteToDisk && xPath.getParent().isItemWritten()) {
                doc.setOriginalLink(xPath.getTargetURL() + "_n.html");
                Path generatedFile2 = xPath
                        .resolveTargetFromBasePath(xPath.getTargetURL() + "_n.html");
                Utils.writeHTML(site, xPath, doc, generatedFile2);
            }
            return doc;
        } else {
            return new XItem(xPath, new XItem.EmptyGenerator());
        }

    }

    public XItem convertThumb(Site site, XPath xPath, boolean neverWriteToDisk)
            throws TemplateException, IOException, InterruptedException {

        String sizeIcon = xPath.getRecursiveProperty("size_icon", "si");
        if (sizeIcon == null) {
            sizeIcon = "250x250^c";
        }
        if (!sizeIcon.startsWith("0x") && site.hasExactTemplate("image_thumb", xPath.getLayoutSuffix())) {
            TemplateBean templateText = site.getTemplate("image_thumb", xPath.getLayoutSuffix());
            Generator gen = new XItem.FillGenerator(site, templateText);
            XItem doc = new XItem(xPath, gen);
            Path generatedFileThumb = xPath.resolveTargetFromBasePath(xPath.getTargetURL()
                    + "_t" + xPath.extensions());

            if (sizeIcon.endsWith("c")) {
                cropResize(xPath, generatedFileThumb, stripMod(sizeIcon, "c"));
            } else {
                resize(xPath, generatedFileThumb, sizeIcon);
            }
            doc.setTemplate("image_thumb");
            doc.setOriginalPath(xPath.getTargetURL() + "_t" + xPath.extensions());

            if (!neverWriteToDisk && xPath.getParent().isItemWritten()) {
                doc.setOriginalLink(xPath.getTargetURL() + "_t.html");

                Path generatedFile2 = xPath
                        .resolveTargetFromBasePath(xPath.getTargetURL() + "_t.html");
                Utils.writeHTML(site, xPath, doc, generatedFile2);
            }
            return doc;
        } else {
            return new XItem(xPath, new XItem.EmptyGenerator());
        }

    }

    private XItem convertOrig(XPath xPath, Path generatedFile, Site site, boolean neverWriteToDisk)
            throws IOException, TemplateException {

        if (xPath.isKeep()) {
            // copy the original image
            Files.copy(xPath.path(), generatedFile,
                    StandardCopyOption.COPY_ATTRIBUTES,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES,
                    LinkOption.NOFOLLOW_LINKS);

            TemplateBean templateText = site.getTemplate("image_orig", xPath.getLayoutSuffix());
            Generator gen = new XItem.FillGenerator(site, templateText);
            XItem doc = new XItem(xPath, gen);

            doc.setTemplate("image_orig");
            doc.setOriginalPath(xPath.getTargetURL() + xPath.extensions());

            if (!neverWriteToDisk && xPath.getParent().isItemWritten()) {
                doc.setOriginalLink(xPath.getTargetURL() + ".html");
                Path generatedFile2 = xPath
                        .resolveTargetFromBasePath(xPath.getTargetURL() + ".html");
                Utils.writeHTML(site, xPath, doc, generatedFile2);
            }

            return doc;

        } else {
            return new XItem(xPath, new XItem.EmptyGenerator());
        }

    }

    @Override
    public List<String> knownExtensions() {
        return Arrays.asList(new String[]{"png", "PNG", "jpg", "jpeg", "JPG",
            "JPEG", "gif", "GIF"});
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
                pb = new ProcessBuilder("/bin/sh", "-c",
                        "/usr/bin/sips " + image + " -z " + tmpHeight + " " + tmpWidth + " --out " + resizedImage + " > /dev/null");
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
                pb = new ProcessBuilder("/bin/sh", "-c",
                        "/usr/bin/sips " + image + " -c " + tmpHeight + " " + tmpWidth + " --out " + resizedImage + " > /dev/null");
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
