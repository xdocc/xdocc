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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.xdocc.Cache;
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
    public XItem compile(Site site, XPath xPath, Map<Path, Integer> filesCounter, Cache cache)
            throws TemplateException, IOException, InterruptedException {

        Path generatedFile = xPath.resolveTargetFromBasePath(xPath.getTargetURL() + xPath.extensions());
        Files.createDirectories(generatedFile.getParent());

        TemplateBean templateTextTop = site.getTemplate("image", xPath.getLayoutSuffix());
        Generator genTop = new XItem.FillGenerator(site, templateTextTop);
        XItem docTop = new XItem(xPath, genTop);

        XItem doc = convertOrig(xPath, generatedFile, site, false, filesCounter, cache);
        docTop.addItems(doc);

        // create a thumbnail
        doc = convertThumb(site, xPath, false, filesCounter, cache);
        docTop.addItems(doc);

        // create display size image
        doc = convertNorm(site, xPath, false, filesCounter, cache);
        docTop.addItems(doc);

        return docTop;
    }

    public XItem convertNorm(Site site, XPath xPath, boolean neverWriteToDisk, Map<Path, Integer> filesCounter, Cache cache)
            throws InterruptedException, TemplateException, IOException {

        Set<Path> generatedFiles = new HashSet<>();
        String sizeNorm = xPath.getRecursiveProperty("size_normal", "sn");
        if (sizeNorm == null) {
            sizeNorm = "800x600^";
        }

        if (!sizeNorm.startsWith("0x") && site.hasExactTemplate("image_norm", xPath.getLayoutSuffix())) {

            Path generatedFileNorm = xPath.resolveTargetFromBasePath(xPath.getTargetURL()
                    + "_n" + xPath.extensions());
            Path generatedFile2 = xPath
                        .resolveTargetFromBasePath(xPath.getTargetURL() + "_n.html");
            
            final XItem doc;
            Cache.CacheEntry cached = cache.getCached(xPath, generatedFileNorm);
            if (cached != null) {
                doc = cached.xItem();
                Utils.increase(filesCounter, Utils.listPaths(site, generatedFileNorm));
                if (!neverWriteToDisk && xPath.getParent().isItemWritten()) {
                    Utils.increase(filesCounter, Utils.listPaths(site, generatedFile2));
                }
            } else {
                TemplateBean templateText = site.getTemplate("image_norm", xPath.getLayoutSuffix());
                Generator gen = new XItem.FillGenerator(site, templateText);
                doc = new XItem(xPath, gen);
                generatedFiles.add(generatedFileNorm);
                if (sizeNorm.endsWith("c")) {
                    cropResize(xPath, generatedFileNorm, stripMod(sizeNorm, "c"));
                } else {
                    resize(xPath, generatedFileNorm, sizeNorm);
                }
                Utils.increase(filesCounter, Utils.listPaths(site, generatedFileNorm));
            
                doc.setTemplate("image_norm");
                doc.setOriginalPath(xPath.getTargetURL() + "_n" + xPath.extensions());

                if (!neverWriteToDisk && xPath.getParent().isItemWritten()) {
                    doc.setOriginalLink(xPath.getTargetURL() + "_n.html");
                
                    generatedFiles.add(generatedFile2);
                    Utils.writeHTML(xPath, doc, generatedFile2);
                    Utils.increase(filesCounter, Utils.listPaths(site, generatedFile2));
                }
                cache.setCached(xPath, doc, generatedFiles.toArray(new Path[generatedFiles.size()]));
            }
            return doc;
        } else {
            return new XItem(xPath, new XItem.EmptyGenerator());
        }

    }

    public XItem convertThumb(Site site, XPath xPath, boolean neverWriteToDisk, Map<Path, Integer> filesCounter, Cache cache)
            throws TemplateException, IOException, InterruptedException {

        Set<Path> generatedFiles = new HashSet<>();
        
        String sizeIcon = xPath.getRecursiveProperty("size_icon", "si");
        if (sizeIcon == null) {
            sizeIcon = "250x250^c";
        }
        if (!sizeIcon.startsWith("0x") && site.hasExactTemplate("image_thumb", xPath.getLayoutSuffix())) {
            
            Path generatedFileThumb = xPath.resolveTargetFromBasePath(xPath.getTargetURL()
                    + "_t" + xPath.extensions());
            Path generatedFile2 = xPath
                        .resolveTargetFromBasePath(xPath.getTargetURL() + "_t.html");
            
            final XItem doc;
            Cache.CacheEntry cached = cache.getCached(xPath, generatedFileThumb);
            if (cached != null) {
                doc = cached.xItem();
                Utils.increase(filesCounter, Utils.listPaths(site, generatedFileThumb));
                if (!neverWriteToDisk && xPath.getParent().isItemWritten()) {
                    Utils.increase(filesCounter, Utils.listPaths(site, generatedFile2));
                }
                
            } else {
                TemplateBean templateText = site.getTemplate("image_thumb", xPath.getLayoutSuffix());
                Generator gen = new XItem.FillGenerator(site, templateText);
                doc = new XItem(xPath, gen);
                generatedFiles.add(generatedFileThumb);
                if (sizeIcon.endsWith("c")) {
                    cropResize(xPath, generatedFileThumb, stripMod(sizeIcon, "c"));
                } else {
                    resize(xPath, generatedFileThumb, sizeIcon);
                }
                Utils.increase(filesCounter, Utils.listPaths(site, generatedFileThumb));
            
                doc.setTemplate("image_thumb");
                doc.setOriginalPath(xPath.getTargetURL() + "_t" + xPath.extensions());

                if (!neverWriteToDisk && xPath.getParent().isItemWritten()) {
                    doc.setOriginalLink(xPath.getTargetURL() + "_t.html");

                   
                    generatedFiles.add(generatedFile2);
                    Utils.writeHTML(xPath, doc, generatedFile2);
                    Utils.increase(filesCounter, Utils.listPaths(site, generatedFile2));
                }
                cache.setCached(xPath, doc, generatedFiles.toArray(new Path[generatedFiles.size()]));
            }
            return doc;
        } else {
            return new XItem(xPath, new XItem.EmptyGenerator());
        }

    }

    private XItem convertOrig(XPath xPath, Path generatedFile, Site site, boolean neverWriteToDisk, Map<Path, Integer> filesCounter, Cache cache)
            throws IOException, TemplateException {

        Set<Path> generatedFiles = new HashSet<>();
        if (xPath.isKeep()) {
            Path generatedFile2 = xPath
                        .resolveTargetFromBasePath(xPath.getTargetURL() + ".html");
            
            final XItem doc;
            Cache.CacheEntry cached = cache.getCached(xPath, generatedFile);
            if (cached != null) {
                doc = cached.xItem();
                Utils.increase(filesCounter, Utils.listPaths(site, generatedFile));
                if (!neverWriteToDisk && xPath.getParent().isItemWritten()) {
                    Utils.increase(filesCounter, Utils.listPaths(site, generatedFile2));
                }
            } else {
            
                // copy the original image
                Files.copy(xPath.path(), generatedFile,
                    StandardCopyOption.COPY_ATTRIBUTES,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES,
                    LinkOption.NOFOLLOW_LINKS);
                Utils.increase(filesCounter, Utils.listPaths(site, generatedFile));
                //generatedFiles.add(generatedFile);
            
                TemplateBean templateText = site.getTemplate("image_orig", xPath.getLayoutSuffix());
                Generator gen = new XItem.FillGenerator(site, templateText);
                doc = new XItem(xPath, gen);

                doc.setTemplate("image_orig");
                doc.setOriginalPath(xPath.getTargetURL() + xPath.extensions());

                if (!neverWriteToDisk && xPath.getParent().isItemWritten()) {
                    doc.setOriginalLink(xPath.getTargetURL() + ".html");
                    
                    generatedFiles.add(generatedFile2);
                    Utils.writeHTML(xPath, doc, generatedFile2);
                    Utils.increase(filesCounter, Utils.listPaths(site, generatedFile2));
                }

                cache.setCached(xPath, doc, generatedFiles.toArray(new Path[generatedFiles.size()]));
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
