package net.xdocc.handlers;

import freemarker.template.TemplateException;
import net.xdocc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class HandlerDirectory implements Handler {

	private static final Logger LOG = LoggerFactory.getLogger(HandlerDirectory.class);

    public static final Map<String, String> MAP = new HashMap<String, String>();
    static{
        MAP.put("list.ftl", "<#list items as item>${item.content}</#list>");
        MAP.put("page.ftl", "<!DOCTYPE HTML><html><head><meta charset=\"UTF-8\"></head><body>${content}</body></html>");
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
            Utils.createDirectories(generatedFile);
            Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
            LOG.debug("copy {} to {}", xPath.path(), generatedFile);
            doc = HandlerCopy.createDocumentBrowse(site, xPath, "");
            Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
            cache.setCached(site, xPath, (Path)null, doc, generatedFile);
        }
        return doc;
	}

	public static XItem compileList(Site site, final Path path, final List<XItem> results,
                             Map<String, Integer> filesCounter, Cache cache, final int depth)
            throws IOException, TemplateException {
        XPath xPath = XPath.get(site, path);

        final XItem doc;
        final Path generatedFile = xPath.resolveTargetFromPath("index.html");
        Cache.CacheEntry cached = cache.getCached(site, xPath);
        if (cached != null  && HandlerUtils.childCached(cache, cached.xItem())) {
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

            List<XItem> visible = new ArrayList<>(results.size());
            for(XItem item: results) {
                if(item.xPath().isVisible() && !item.xPath().isDirectory()) {
                    visible.add(item);
                }
                //iterate over item and check if that item is promoted
                //if yes, add it to top according if exposed or promoted
                visible.addAll(promoteItems(item));
            }
            doc.setItems(visible);

            doc.setDepth(depth, depth);

            if (!xPath.isNoIndex() && xPath.isVisible()) {
                Utils.writeListHTML(xPath, doc, generatedFile);
                Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
            }

        }
        cache.setCached(site, xPath, fromXPathList(results), doc, generatedFile);
        return doc;

    }

    private static List<XItem> promoteItems(XItem item) {
        if(item.getTemplate() != null && item.getTemplate().equals("list") && !item.getItems().isEmpty()) {
            if(item.getPromoted() && !item.getItemsPromoted().isEmpty()) {
                List<XItem> all = new ArrayList<>(item.getItems().size());
                for(XItem tmp:item.getItemsPromoted()) {
                    int promoteDepth = tmp.getPromoteDepth();
                    all.add(new XItem(tmp).setPromoteDepth(promoteDepth - 1));
                }
                return all; //get all promoted
            } else if(item.getPromoted() && item.getItemsPromoted().isEmpty()) {
                List<XItem> one = new ArrayList<>(1);
                int promoteDepth = item.getItems().get(0).getPromoteDepth();
                one.add(new XItem(item.getItems().get(0)).setPromoteDepth(promoteDepth - 1));
                return one;
            } else if(item.getExposed()) {
                List<XItem> all = new ArrayList<>(item.getItems().size());
                for(XItem tmp:item.getItems()) {
                    int promoteDepth = tmp.getPromoteDepth();
                    all.add(new XItem(tmp).setPromoteDepth(promoteDepth - 1));
                }
                return all; //get all
            }
        }
        return Collections.emptyList();
    }

    private static List<Path> fromXPathList(final List<XItem> results) {
		List<Path> retVal = new ArrayList<>();
		for(XItem item:results) {
			retVal.add(Paths.get(item.xPath().path()));
		}
		return retVal;
	}

    @Override
    public List<String> knownExtensions() {
        return Arrays.asList(new String[0]);
    }
}