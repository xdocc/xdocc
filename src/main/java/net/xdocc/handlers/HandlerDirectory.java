package net.xdocc.handlers;

import freemarker.template.TemplateException;
import net.xdocc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class HandlerDirectory implements Handler {

	private static final Logger LOG = LoggerFactory.getLogger(HandlerDirectory.class);

    public static final Map<String, String> MAP = new HashMap<String, String>();
    static{
        MAP.put("list.ftl", "<#list items as item>${item.content}</#list>");
        MAP.put("page.ftl", "${content}");
    }

	@Override
	public boolean canHandle(Site site, XPath xPath) {return xPath.isDirectory() && xPath.isCopy();}

	@Override
	public XItem compile(Site site, XPath xPath, Map<String, Integer> filesCounter, Cache cache) throws Exception {

        final XItem doc;
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

        Cache.CacheEntry cached = cache.getCached(site, xPath);
        if (cached != null) {
            doc = cached.xItem();
            Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
        } else {
            Files.createDirectories(generatedFile.getParent());
            Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
            LOG.debug("copy {} to {}", xPath.path(), generatedFile);
            doc = HandlerCopy.createDocumentBrowse(site, xPath, "");
            Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
            cache.setCached(site, xPath, null, doc, generatedFile);
        }
        return doc;
	}

	public static XItem compileList(Site site, final Path path, final List<XItem> results,
                             Map<String, Integer> filesCounter, Cache cache, final int depth, final int promoteDepth)
            throws IOException, TemplateException {
        XPath xPath = new XPath(site, path);

        final XItem doc;
        final Path generatedFile = xPath.resolveTargetFromPath("index.html");
        Cache.CacheEntry cached = cache.getCached(site, xPath);
        if (cached != null) {
            doc = cached.xItem();
            if (!xPath.isNoIndex()) {
                Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
            }
        } else {

            final boolean ascending;
            if (xPath.isAutoSort()) {
                ascending = Utils.guessAutoSort1(results);
            } else {
                ascending = xPath.isAscending();
            }
            Utils.sort3(results, ascending);

            doc = Utils.createDocument(site, xPath, null, "list");
            doc.setItems(results);
            doc.setDepth(depth, promoteDepth);

            if (!xPath.isNoIndex() && xPath.isVisible()) {
                Utils.writeListHTML(xPath, doc, generatedFile);
                Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
            }
        }
        cache.setCached(site, xPath, null, doc, generatedFile);
        return doc;

    }

    @Override
    public List<String> knownExtensions() {
        return Arrays.asList(new String[0]);
    }
}