/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.xdocc;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import static net.xdocc.XItem.CURRENT_PAGE;
import static net.xdocc.XItem.PAGE_URLS;

/**
 *
 * @author draft
 */
public class XList extends XItem {
    
    public XList(XPath xPath, Generator documentGenerator, String url) throws IOException {
        super(xPath, documentGenerator, url);
    }
    
    // list
    public static final String ITEMS = "items";
    public static final String ITEMS_SIZE = "document_size";
    
    /**
     * @return a list of documents if present in the model or null
     */
    public List<XItem> getItems() {
        @SuppressWarnings("unchecked")
        java.util.List<XItem> documents = (java.util.List<XItem>) documentGenerator()
                .model().get(ITEMS);
        if (documents == null) {
            return Collections.emptyList();
        }
        return documents;
    }

    /**
     * @param documents The list of documents in a collection
     * @return this class
     */
    public XItem setItems(java.util.List<XItem> documents) {
        documentGenerator().model().put(ITEMS, documents);
        documentGenerator().model().put(ITEMS_SIZE, documents.size());
        return this;
    }

    /**
     * Set the data for paging. This is the the URLs for the other pages and the current site
     *
     * @param pageURLs
     * @param current
     */
    public void setPaging(java.util.List<String> pageURLs, Integer current) {
        documentGenerator().model().put(PAGE_URLS, pageURLs);
        documentGenerator().model().put(CURRENT_PAGE, current);

    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> getPageURLs() {
        return (java.util.List<String>) documentGenerator().model().get(PAGE_URLS);
    }

    public Integer getCurrent() {
        return (Integer) documentGenerator().model().get(CURRENT_PAGE);

    }
    
}
