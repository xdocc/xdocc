package net.xdocc.handlers;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.xdocc.Cache;

import net.xdocc.XItem;
import net.xdocc.XItem.Generator;
import net.xdocc.Site;
import net.xdocc.TemplateBean;
import net.xdocc.Utils;
import net.xdocc.XPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlerCopy implements Handler {
    
    public static final Map<String, String> MAP = new HashMap<String, String>();
    static {
    	MAP.put("file.ftl", "<a href=\"${url}\">${name}</a>");
    }
    

    private static final Logger LOG = LoggerFactory.getLogger(HandlerCopy.class);

    @Override
    public boolean canHandle(Site site, XPath xPath) {
        return !xPath.isDirectory();
    }

    @Override
    public XItem compile(Site site, XPath xPath, Map<String, Integer> filesCounter, Cache cache) {
        final Path generatedFile;
        if (xPath.isVisible()) {
            String filename = xPath.fileName();
            String extension = filename.substring(filename.lastIndexOf("."));
            generatedFile = xPath.resolveTargetFromBasePath(
                    xPath.getTargetURL() + extension);
        } else {
            generatedFile = xPath.resolveTargetFromBasePath(
                    xPath.getTargetURLFilename());
        }

        try {
            if(cache.isCached(site, xPath)) {
                Cache.CacheEntry cached = cache.getCached(site, xPath);
                if (cached != null) {
                    System.out.println("CACHED: " + generatedFile);
                    XItem doc = cached.xItem();
                    Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
                    return doc;
                } else {
                    return null;
                }
            }
            else {
                //copy ignores the page/isItemWritten flag
                if(!Files.exists(generatedFile.getParent())) {
                    Files.createDirectories(generatedFile.getParent());
                }
                if(!xPath.isDirectory()) {
                    Files.copy(Paths.get(xPath.path()), generatedFile,
                            StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS);
                }
                Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
                LOG.debug("copy {} to {}", xPath.path(), generatedFile);

                if (xPath.isCopy() || xPath.isVisible()) {
                    XItem item = createDocumentBrowse(site, xPath, "");
                    cache.setCached(site, xPath, (Path)null, item, generatedFile);
                    return item;
                } else {
                    cache.setCached(site, xPath, (Path)null, null, generatedFile);
                }
            }
        } catch (IOException e) {
            LOG.error("Copy handler failed, cannot copy from {} to {}", xPath.path(), generatedFile, e);
        }

        return null;
    }

    @Override
    public List<String> knownExtensions() {
        return Arrays.asList(new String[0]);
    }

    public static XItem createDocumentBrowse(Site site, XPath xPath,
            String path) throws IOException {
        TemplateBean templateText = site.getTemplate("file");
        Generator documentGenerator = new XItem.FillGenerator(site,
                templateText);
        XItem document = new XItem(xPath, documentGenerator);
        Date lastModified = new Date(Files.getLastModifiedTime(Paths.get(xPath.path()))
                .toMillis());
        document.setDate(lastModified);
        document.setName(xPath.fileName());
        document.setTemplate("file");
        document.setLayout(xPath.getLayoutSuffix());
        return document;
    }

}
