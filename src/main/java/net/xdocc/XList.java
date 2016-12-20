/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.xdocc;

import java.util.Collections;
import java.util.List;
import static net.xdocc.Document.CURRENT_PAGE;
import static net.xdocc.Document.PAGE_URLS;

/**
 *
 * @author draft
 */
public class XList extends Document {
    
    public XList(XPath xPath, DocumentGenerator documentGenerator, String url) {
        super(xPath, documentGenerator, url);
    }
    
    // list
    public static final String LIST = "list";
    public static final String DOCUMENT_SIZE = "document_size";
    
    /**
     * @return a list of documents if present in the model or null
     */
    public List<Document> getList() {
        @SuppressWarnings("unchecked")
        java.util.List<Document> documents = (java.util.List<Document>) documentGenerator()
                .model().get(LIST);
        if (documents == null) {
            return Collections.emptyList();
        }
        return documents;
    }

    /**
     * @param documents The list of documents in a collection
     * @return this class
     */
    public Document setList(java.util.List<Document> documents) {
        documentGenerator().model().put(LIST, documents);
        documentGenerator().model().put(DOCUMENT_SIZE, documents.size());
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
