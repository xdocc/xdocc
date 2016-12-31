package net.xdocc.handlers;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.xdocc.XItem;
import net.xdocc.Site;
import net.xdocc.Utils;
import net.xdocc.XPath;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

public class HandlerLink implements Handler {

    @Override
    public boolean canHandle(Site site, XPath xPath) {
        return xPath.isCompile() && !xPath.isDirectory()
                && HandlerUtils.knowsExtension(knownExtensions(), xPath);
    }

    @Override
    public List<String> knownExtensions() {
        return Arrays.asList(new String[]{"link", "Link", "LINK"});
    }

    @Override
    public XItem compile(Site site, XPath xPath, Map<String, Object> model, String relativePathToRoot)
            throws Exception {

        Configuration config = new PropertiesConfiguration(xPath.path().toFile());

        List<Object> urls = config.getList("url", new ArrayList<>());
        int limit = config.getInt("limit", -1);

        List<XPath> founds = new ArrayList<>();

        for (Object url : urls) {
            founds.addAll(Utils.findURL(site,
                    xPath, (String) url));
        }
        if (founds.size() == 0 || (founds.size() > 0 && !founds.get(0).isVisible())) {
            return null;
        } else {
            List<XItem> documents = new ArrayList<>();

            final boolean ascending;
            if (xPath.isAutoSort()) {
                ascending = Utils.guessAutoSort(founds);
            } else {
                ascending = xPath.isAscending();
            }
            Utils.sort2(founds, ascending);

            for (XPath found : founds) {
                documents.addAll(site.service().compile(site, found.path(), new HashMap<String, Object>()).
                        get());
            }

            if (limit >= 0) {
                documents = documents.subList(0,
                        documents.size() < limit ? documents.size() : limit);
            }

            XItem doc = Utils.createDocument(site, xPath, null, "link");
            doc.setItems(documents);
            // always create a single page for that
            if (xPath.getParent().isItemWritten()) {
                Path generatedFile = xPath
                        .resolveTargetFromBasePath(xPath.getTargetURL() + ".html");
                Utils.writeHTML(site, xPath, "", doc, generatedFile);
            }
            return doc;
        }
    }
}
