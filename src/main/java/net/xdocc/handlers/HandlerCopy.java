package net.xdocc.handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.xdocc.Cache;

import net.xdocc.XItem;
import net.xdocc.XItem.Generator;
import net.xdocc.Site;
import net.xdocc.Site.TemplateBean;
import net.xdocc.Utils;
import net.xdocc.XPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlerCopy implements Handler {
    
    public static final Map<String, String> MAP = new HashMap<String, String>() {{
        put("file.ftl", "<a href=\"${url}\">${name}</a>");
    }};

    private static final Logger LOG = LoggerFactory.getLogger(HandlerCopy.class);

    @Override
    public boolean canHandle(Site site, XPath xPath) {
        return true;
    }

    @Override
    public XItem compile(Site site, XPath xPath, Map<Path, Integer> filesCounter, Cache cache) {
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
            Cache.CacheEntry cached = cache.getCached(xPath);
            if (cached != null) {
                XItem doc = cached.xItem();
                Utils.increase(filesCounter, Utils.listPaths(site, generatedFile));                
                return doc;
                
            } else {
                //copy ignores the page/isItemWritten flag
                Files.createDirectories(generatedFile.getParent());
                Files.copy(xPath.path(), generatedFile,
                        StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS);
                Utils.increase(filesCounter, Utils.listPaths(site, generatedFile));
                LOG.debug("copy {} to {}", xPath.path(), generatedFile);

                if (xPath.isCopy() || xPath.isVisible()) {
                    XItem item = createDocumentBrowse(site, xPath, "");
                    cache.setCached(xPath, item, generatedFile);
                    return item;

                } else {
                    cache.setCached(xPath, null, generatedFile);
                }
            }
        } catch (IOException e) {
            LOG.error("Copy handler faild, cannot copy from {} to {}", xPath.path(), generatedFile, e);
        }

        return null;
    }

    @Override
    public List<String> knownExtensions() {
        return Arrays.asList(new String[0]);
    }

    public static XItem createDocumentBrowse(Site site, XPath xPath,
            String path) throws IOException {
        TemplateBean templateText = site.getTemplate("file", xPath.getLayoutSuffix());
        Generator documentGenerator = new XItem.FillGenerator(site,
                templateText);
        XItem document = new XItem(xPath, documentGenerator);
        Date lastModified = new Date(Files.getLastModifiedTime(xPath.path())
                .toMillis());
        document.setDate(lastModified);
        document.setName(xPath.fileName());
        document.setTemplate("file");
        return document;
    }

}
