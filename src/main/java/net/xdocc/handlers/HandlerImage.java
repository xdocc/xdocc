package net.xdocc.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import net.xdocc.*;
import net.xdocc.Site.TemplateBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.TemplateException;
import net.xdocc.XItem.Generator;

/**
 * Create responsive images
 *
 * https://responsiveimages.org/demos/on-a-grid/index.html
 * http://www.responsivebreakpoints.com/
 * http://stackoverflow.com/questions/21262466/imagemagick-how-to-minimally-crop-an-image-to-a-certain-aspect-ratio
 *
 * Responsive images are hard :)
 *
 * If an image can be compiled -> it start eg. with 1-img.jpg,
 * then a series of scaled down images by 50% are created. These
 * images can be used for srcset, where the browser handles the
 * correct loading of the image
 *
 * The user can choose to crop the image or set if a smaller preview
 * image should be linked to another site.
 *
 * 1-img|n=Boat|link.jpg
 * 1-img|n=Boat|link|crop=16-9.jpg
 */
public class HandlerImage implements Handler {
    
    public static final Map<String, String> MAP = new HashMap<String, String>() {{
        put("image.ftl",
                "<figure>"+
                "<#if link??><a href=\"${path}/${link}\"></#if>" +
                "<img src=\"${path}/${srcsets?last.src}\" " +
                    "srcset=\"<#list srcsets as srcset>${path}/${srcset.src} ${srcset.attribute}<#sep>,</#list>\" " +
                    "sizes=\"90vw\">" +
                "<figcaption>${name}</figcaption>" +
                "<#if link??></a></#if></figure>");
        put("image_detail.ftl",
                "<figure>"+
                "<img src=\"${path}/${srcsets?last.src}\" " +
                    "srcset=\"<#list srcsets as srcset>${path}/${srcset.src} ${srcset.attribute}<#sep>,</#list>\" " +
                    "sizes=\"90vw\">" +
                "<figcaption>${name}</figcaption>" +
                "</figure>");
    }};
    
    private static final Logger LOG = LoggerFactory.getLogger(HandlerImage.class);

    @Override
    public boolean canHandle(Site site, XPath xPath) {
        return xPath.isCompile() && !xPath.isDirectory()
                && HandlerUtils.knowsExtension(knownExtensions(), xPath);
    }

    @Override
    public XItem compile(Site site, XPath xPath, Map<Path, Integer> filesCounter, Cache cache)
            throws TemplateException, IOException, InterruptedException {

        Path generatedFile2 = xPath
                .resolveTargetFromBasePath(xPath.getTargetURL() + ".html");

        Cache.CacheEntry cached = cache.getCached(xPath);
        if (cached != null) {
            if(xPath.hasRecursiveProperty("link","l") && xPath.getParent().isItemWritten()) {
                Utils.increase(filesCounter, Utils.listPaths(site, generatedFile2));
            }
            return cached.xItem();
        }

        TemplateBean templateImage = site.getTemplate("image", xPath.getLayoutSuffix());
        Generator genImage = new XItem.FillGenerator(site, templateImage);
        XItem docTop = new XItem(xPath, genImage);

        //check if we need to crop
        List<Pair<Path,String>> resizeList = null;
        String crop = xPath.getRecursiveProperty("crop");
        List<Path> generated = new ArrayList<>();
        if(crop !=null) {
            crop = crop.replace("-","/");
            List<Pair<Path,String>> cropList = HandlerImage.cropImages(xPath, crop, 100);
            for(Pair<Path,String> p:cropList) {
                generated.add(p.element0());
            }
            docTop.setSrcSets(convert(xPath, site, filesCounter, cropList));
        }
        else {
            resizeList = HandlerImage.resizeImages(xPath, 100);
            for(Pair<Path,String> p:resizeList) {
                generated.add(p.element0());
            }
            docTop.setSrcSets(convert(xPath, site, filesCounter, resizeList));
        }

        //check if link is required
        if(xPath.hasRecursiveProperty("link","l") && xPath.getParent().isItemWritten()) {
            //create file
            TemplateBean templateLink = site.getTemplate("image_detail", xPath.getLayoutSuffix());
            Generator genLink = new XItem.FillGenerator(site, templateLink);
            XItem docDetail = new XItem(xPath, genLink);
            //set link
            docTop.setLink(xPath.getTargetURLName() + ".html");
            if(resizeList == null) {
                resizeList = HandlerImage.resizeImages(xPath, 100);
            }
            docDetail.setSrcSets(convert(xPath, site, filesCounter, resizeList));

            Utils.writeHTML(xPath, docDetail, generatedFile2);
            Utils.increase(filesCounter, Utils.listPaths(site, generatedFile2));
            generated.add(generatedFile2);

        }
        cache.setCached(xPath, docTop, generated.toArray(new Path[]{}));
        return docTop;
    }

    private List<SrcSet> convert(XPath xPath, Site site, Map<Path, Integer> filesCounter, List<Pair<Path, String>> resizeList) {
        List<SrcSet> result = new ArrayList<>();
        for(Pair<Path, String> pair:resizeList) {
            Utils.increase(filesCounter, Utils.listPaths(site, pair.element0()));
            result.add(new SrcSet(pair.element0().getFileName().toString(), pair.element1()));
        }
        return result;
    }

    @Override
    public List<String> knownExtensions() {
        return Arrays.asList(new String[]{"png", "PNG", "jpg", "jpeg", "JPG",
            "JPEG", "gif", "GIF"});
    }

    public static String executeGetAspectSize(String image, String aspect) throws IOException, InterruptedException {
        return executeAndOutput(new ProcessBuilder(
                "/usr/bin/convert",
                image,
                "-format",
                "%[fx:w/h>="+aspect+"?h*"+aspect+":w]x%[fx:w/h<="+aspect+"?w/"+aspect+":h]",
                "info:"));
    }

    public static String executeGetSize(String image) throws IOException, InterruptedException {
        return executeAndOutput(new ProcessBuilder(
                "/usr/bin/convert",
                image,
                "-format",
                "%[w]x%[h]",
                "info:"));
    }

    public static String executeCropResize(String image, int w, int h, String outputImageName) throws IOException, InterruptedException {
        return executeAndOutput(new ProcessBuilder(
                "/usr/bin/convert",
                image,
                "-resize", w+"x"+h+"^",
                "-gravity", "center",
                "-crop", w+"x"+h+"+0+0",
                 "+repage",
                outputImageName));
    }

    public static String executeResize(String image, int w, int h, String outputImageName) throws IOException, InterruptedException {
        return executeAndOutput(new ProcessBuilder(
                "/usr/bin/convert",
                image,
                "-resize", w+"x"+h+"^",
                outputImageName));
    }

    public static List<Pair<Path,String>> cropImages(XPath xPath, String aspect, int limit) throws IOException, InterruptedException {
        List<Pair<Path,String>> result = new ArrayList<>();
        String size = HandlerImage.executeGetAspectSize(xPath.path().toString(), aspect);
        float w = Float.parseFloat(size.substring(0, size.indexOf("x")));
        float h = Float.parseFloat(size.substring(size.indexOf("x") + 1));
        do {
            Path dstImage = xPath.resolveTargetFromBasePath(xPath.getTargetURL()
                    + "_crop_"+ Math.round(w) + xPath.extensions());
            Files.createDirectories(dstImage.getParent());
            String tmp = HandlerImage.executeCropResize(xPath.path().toString(), Math.round(w), Math.round(h), dstImage.toString());
            result.add(new Pair<>(dstImage,Math.round(w)+"w"));
            w /= 2;
            h /= 2;
        } while(w > limit && h > limit);
        return result;
    }

    public static List<Pair<Path,String>> resizeImages(XPath xPath, int limit) throws IOException, InterruptedException {
        List<Pair<Path,String>> result = new ArrayList<>();
        String size = HandlerImage.executeGetSize(xPath.path().toString());
        float w = Float.parseFloat(size.substring(0, size.indexOf("x")));
        float h = Float.parseFloat(size.substring(size.indexOf("x") + 1));
        do {
            Path dstImage = xPath.resolveTargetFromBasePath(xPath.getTargetURL()
                    + "_"+ Math.round(w) + xPath.extensions());
            Files.createDirectories(dstImage.getParent());
            String tmp = HandlerImage.executeResize(xPath.path().toString(), Math.round(w), Math.round(h), dstImage.toString());
            result.add(new Pair<>(dstImage,Math.round(w)+"w"));
            w /= 2;
            h /= 2;
        } while(w > limit && h > limit);
        return result;
    }

    private static String executeAndOutput(ProcessBuilder pb) throws IOException,
            InterruptedException {
        pb.redirectErrorStream(true);

        Process p = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                p.getErrorStream()));
        String line = null;
        while ((line = br.readLine()) != null) {
            LOG.error(line);
        }
        br = new BufferedReader(new InputStreamReader(
                p.getInputStream()));
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        p.waitFor();
        return sb.toString().trim();
    }

    private static String stripMod(String size, String... mods) {
        for (String mod : mods) {
            size = size.replace(mod, "");
        }
        return size;
    }

}
