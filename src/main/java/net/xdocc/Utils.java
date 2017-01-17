package net.xdocc;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.xdocc.Site.TemplateBean;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.NullCacheStorage;
import freemarker.template.TemplateException;
import java.nio.file.Paths;

public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    private static XItem adjustPath(XItem doc, String minusPath) {
        adjustPath0(doc, minusPath);
        for(XItem item:doc.getItems()) {
            adjustPath(item, minusPath);
        }
        return doc;
    }
    
    private static XItem adjustPath0(XItem doc, String minusPath) {
        String path = doc.getOriginalPath();
        Path pathRelative = Paths.get(minusPath).relativize(Paths.get(path));
        path = pathRelative.toString();
        //path = path.startsWith(minusPath) ? path.substring(minusPath.length()) : path;
        //path = path.startsWith("/") ? path.substring(1) : path;
        path = path.isEmpty() ? ".":path;
        doc.setPath(path);
        return doc;
    }
    
    private static XItem adjustPathToRoot(XItem doc, String newPathToRoot) {
        adjustPathToRoot0(doc, newPathToRoot);
        for(XItem item:doc.getItems()) {
            adjustPathToRoot(item, newPathToRoot);
        }
        return doc;
    }
    
    private static XItem adjustPathToRoot0(XItem doc, String newPathToRoot) {
        doc.setPathToRoot(newPathToRoot);
        return doc;
    }

    private static XItem adjustPromotedDepth(XItem doc, Integer minusPromoteDepth) {
        Integer calc = null;
        if(doc.getPromoteDepthOriginal()!= null) {
            calc = doc.getPromoteDepthOriginal() - minusPromoteDepth;
        }
        doc.setPromoteDepth(calc);
        for(XItem item:doc.getItems()) {
            adjustPromotedDepth(item, minusPromoteDepth);
        }
        return doc;
    }

    

    

    public static enum OS_TYPE {
        LINUX, WIN, MAC, OTHER
    };

    public static OS_TYPE getOSType() {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            return OS_TYPE.WIN;
        } else if (osName.startsWith("Mac")) {
            return OS_TYPE.MAC;
        } else if (osName.startsWith("Linux")) {
            return OS_TYPE.LINUX;
        } else {
            return OS_TYPE.OTHER;
        }
    }

    public static XPath find(Path resolved, List<Site> sites) {
        for (Site site : sites) {
            if (isChild(resolved, site.source())) {
                return new XPath(site, resolved);
            }
        }
        return null;
    }
    
    public static boolean isChild(Link parent, Link maybeChild) {
        if(parent.equals(maybeChild)) {
            return true;
        }
        for(Link child:parent.getChildren()) {
            if(isChild(child, maybeChild)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isChild(Path maybeChild, Path possibleParent) {
        URI parentURI = possibleParent.toUri(), childURI = maybeChild.toUri();
        return !parentURI.relativize(childURI).isAbsolute();
    }

    /**
     * Creates a relative path to the source. If the path is not a chiled of the source, then null is
     * returned. If source is /tmp/ and path is /tmp/test, then the relative path to root is ../
     *
     * @param root The root
     * @param path The path
     * @return The relative path to root
     */
    public static String relativePathToRoot(Path root, Path path) {
        return relativePathToRoot(root, path, false);
    }

    public static String relativePathToRoot(Path root, Path path,
            boolean includeSelf) {
        if (!isChild(path, root)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        Path parent;
        if (Files.isDirectory(path) && !includeSelf) {
            parent = path;
        } else {
            parent = path.getParent();
        }
        while (parent != null && !parent.equals(root)) {
            sb.append("../");
            parent = parent.getParent();
        }
        return sb.toString();
    }
    
    public static void deleteDirectories(Path... paths) throws IOException {
        for(Path path:paths) {
            deleteDirectory(path);
        }
    }

    public static void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new FileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                if (exc == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw exc;
                }
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc)
                    throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static String[] createURLSplit(Path source, XPath xPath) {
        List<String> paths = new ArrayList<>();
        while (!source.equals(xPath.path())) {
            paths.add(0, xPath.url());
            xPath = new XPath(xPath.site(), xPath.path().getParent());
        }
        return paths.toArray(new String[0]);
    }

    public static String createURL(String[] paths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.length; i++) {
            if (i > 0) {
                sb.append("/");
            }
            sb.append(paths[i]);
        }
        return sb.toString();
    }

    public static List<XPath> getNonHiddenChildren(final Site site,
            final Path siteToCompile) throws IOException {
        final List<XPath> result = new ArrayList<>();
        Files.walkFileTree(siteToCompile,
                EnumSet.noneOf(FileVisitOption.class), 1,
                new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) throws IOException {
                // do not include ourself
                if (!siteToCompile.equals(dir)) {
                    XPath xPath = new XPath(site, dir);
                    if (!xPath.isHidden()) {
                        result.add(xPath);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                // do not include ourself
                if (!siteToCompile.equals(file)) {
                    XPath xPath = new XPath(site, file);
                    if (!xPath.isHidden()) {
                        result.add(xPath);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file,
                    IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir,
                    IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    public static List<XPath> getDownDependencies(final Site site,
            final Path siteToCompile) throws IOException {
        final List<XPath> result = new ArrayList<>();
        Files.walkFileTree(siteToCompile,
                EnumSet.noneOf(FileVisitOption.class), 1,
                new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) throws IOException {
                // do not include ourself
                if (!siteToCompile.equals(dir)) {
                    XPath xPath = new XPath(site, dir);
                    if (!xPath.isHidden() && xPath.isVisible() && !xPath.isNavigation()) {
                        result.add(xPath);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                // do not include ourself
                if (!siteToCompile.equals(file)) {
                    XPath xPath = new XPath(site, file);
                    if (!xPath.isHidden() && xPath.isVisible() && !xPath.isNavigation()) {
                        result.add(xPath);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file,
                    IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir,
                    IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    public static List<Path> getChildren(final Site site,
            final Path siteToCompile) throws IOException {
        final List<Path> result = new ArrayList<>();
        Files.walkFileTree(siteToCompile,
                EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE,
                new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) throws IOException {
                // do not include ourself
                if (!siteToCompile.equals(dir)) {
                    result.add(dir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                // do not include ourself
                if (!siteToCompile.equals(file)) {
                    result.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file,
                    IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir,
                    IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    public static List<String> splitExtensions(String extensions) {
        List<String> retVal = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(extensions, ".");
        while (st.hasMoreTokens()) {
            String extension = st.nextToken();
            if (!extension.equals("")) {
                retVal.add(extension);
            }
        }
        return retVal;
    }

    /**
     * Search for the highlighted document. May return null if no document was found. If non document is
     * tagged, the first one will be used
     *
     * @param documents The list of document found in the folder
     * @return The highlighted document or null.
     */
    /*public static XItem searchHighlight(List<XItem> documents) {
        for (XItem document : documents) {
            if (document.getHighlight()) {
                return document;
            }
        }
        if (documents.size() > 0) {
            return documents.get(0);
        }
        return null;
    }*/

    /**
     * Sort the documents according to its number. If a date was provided, the date will be converted to a
     * long. If no number is provided the sort will be by name.
     *
     * @param documents The documents to sort
     * @param inverted A flag to invert the sort order
     */
    public static void sort2(List<XPath> children, final boolean inverted) {
        Collections.sort(children, new Comparator<XPath>() {
            @Override
            public int compare(XPath o1, XPath o2) {
                int compare = o1.compareTo(o2);
                return inverted ? compare : compare * -1;
            }
        });

    }
    
    public static void sort3(List<XItem> results, final boolean inverted) {
        Collections.sort(results, new Comparator<XItem>() {
            @Override
            public int compare(XItem o1, XItem o2) {
                int compare = o1.xPath().compareTo(o2.xPath());
                return inverted ? compare : compare * -1;
            }
        });
    }

    public static void write(String html, XPath xPath, Path generatedFile)
            throws TemplateException, IOException {
        // not in use yet
        Map<Path, Path> created = new HashMap<Path, Path>();

        Path alreadyGeneratedSource = created.get(generatedFile);
        if (alreadyGeneratedSource == null) {
            created.put(generatedFile, xPath.path());
        } else if (alreadyGeneratedSource.equals(xPath.path())) {
            throw new IOException("create " + generatedFile
                    + ", but it was already created by "
                    + alreadyGeneratedSource + ". Anyway we will overwrite");
        }

        try (FileWriter fw = new FileWriter(generatedFile.toFile())) {
            fw.write(html);
        }
    }

    /**
     * Escapes for inclusion as an attribute -> http://stackoverflow.com/questions
     * /8909613/html-entity-escaping-to-prevent-xss.
     *
     * @param s1 string.
     * @return The string that is safe to place as an attribute
     */
    public static String escapeAttribute(String s1) {
        return s1.replace("&", "&amp;").replace("\"", "&quot;");
    }

    /**
     * Prints the complete model as a debug table and adds it to the model.
     *
     * @param model The model for the output.
     * @return A HTML formated string
     */
    public static String getDebug(final Map<String, Object> model) {
        final int previewSize = 50;
        final StringBuilder sb = new StringBuilder(
                "<table border=1><th colspan=2>this document contains</th>\n");
        for (Map.Entry<String, Object> entry : model.entrySet()) {
            if ("debug".equals(entry.getKey())) {
                continue;
            }
            sb.append("<tr><td>");
            sb.append(escapeHtml(entry.getKey()));
            sb.append("</td><td title=\"");
            String value = entry.getValue() == null ? "null" : entry.getValue()
                    .toString();
            sb.append(escapeAttribute(value));
            sb.append("\">");
            value = escapeHtml(value);
            sb.append(value.length() > previewSize ? value.substring(0,
                    previewSize) : value);
            sb.append("</td></tr>\n");
        }
        return sb.append("</table>").toString();
    }

    public static Object lock = new Object();

    public static String applyTemplate(Site site, TemplateBean templateText,
            Map<String, Object> model) throws TemplateException, IOException {
        model.put(XItem.DEBUG, getDebug(model));
        StringWriter sw = new StringWriter();
        synchronized (lock) {

            templateText
                    .template()
                    .getConfiguration()
                    .setDirectoryForTemplateLoading(site.templatePath().toFile());
            templateText.template().getConfiguration()
                    .setCacheStorage(new NullCacheStorage());
            templateText
                    .template()
                    .getConfiguration()
                    .setTemplateLoader(
                            new Custom2FileTemplateLoader(site
                                    .templatePath().toFile(), site,
                                    templateText));
            try {
                templateText.template().process(model, sw);
            } catch (Exception e) {
                e.printStackTrace();
                LOG.debug("available data:");
                for (Map.Entry<String, Object> entry : model.entrySet()) {
                    LOG.debug("key:[" + entry.getKey() + "]=["
                            + entry.getValue() + "]");
                }
                LOG.debug("Template is: " + templateText.file());
            }

            sw.flush();
            return sw.getBuffer().toString();
        }

    }

    final static class Custom2FileTemplateLoader extends FileTemplateLoader {

        final private TemplateBean parentTemplateBean;
        final private Site site;

        public Custom2FileTemplateLoader(File baseDir, Site site,
                TemplateBean templateBean) throws IOException {
            super(baseDir);
            this.site = site;
            this.parentTemplateBean = templateBean;
        }

        @Override
        public Object findTemplateSource(String name) throws IOException {

            File source = (File) super.findTemplateSource(name);
            if (source == null) {
                return null;
            }

            //TemplateBean templateBean = site.service().getTemplateBeans(site).get(name);
            //templateBean.addDependencies(parentTemplateBean);

            return source;
        }

    }

    /*public static List<XItem> filter(List<XItem> documents) {
        List<XItem> retVal = new ArrayList<>();
        List<XItem> toPreview = new ArrayList<>();
        if (toPreview.size() > 0) {
            XItem document = searchHighlight(toPreview);
            retVal.add(document);
        }
        return retVal;
    }*/

    public static void createFile(Path source, String path, String content)
            throws IOException {
        Path file = source.resolve(path);
        Files.createDirectories(file.getParent());
        Files.createFile(file);
        Files.write(file, content.getBytes());
    }

    public static Link find(XPath xPath, Link navigation) {
        // we pass xPath as null if it is root
        if (xPath == null) {
            return navigation;
        }
        if (navigation.getTarget().path().equals(xPath.path())) {
            return navigation;
        }
        for (Link link : navigation.getChildren()) {
            Link found = find(xPath, link);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public static void createDirectory(Site site) throws IOException {
        Files.createDirectories(site.generated());
    }

    public static String searchXPath(Path parent, String xPath)
            throws IOException {
        if (!Files.exists(parent)) {
            return null;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(parent)) {
            for (Path p : ds) {
                if (Files.isDirectory(p)) {
                    if (p.getFileName().toString().indexOf("|" + xPath) > 0) {
                        return p.toString();
                    } else if (p.getFileName().toString().indexOf(xPath) == 0) {
                        return p.toString();
                    }
                }
            }

        }
        return null;
    }

    /**
     * Performs a wildcard matching for the text and pattern provided.
     *
     * @param text the text to be tested for matches.
     *
     * @param pattern the pattern to be matched for. This can contain the wildcard character '*' (asterisk).
     *
     * @return <tt>true</tt> if a match is found, <tt>false</tt> otherwise.
     */
    public static boolean wildCardMatch(String text, String pattern) {
        // Create the cards by splitting using a RegEx. If more speed
        // is desired, a simpler character based splitting can be done.
        String[] cards = pattern.split("\\*");
        // Iterate over the cards.
        for (String card : cards) {
            int idx = text.indexOf(card);
            // Card not detected in the text.
            if (idx == -1) {
                return false;
            }
            // Move ahead, towards the right of the text.
            text = text.substring(idx + card.length());
        }
        return true;
    }

    public static List<XPath> findChildURL(Site site, XPath current,
            String url, String extension) throws IOException {
        List<XPath> children = Utils.getNonHiddenChildren(site,
                current.path());
        List<XPath> result = new ArrayList<>();
        for (XPath child : children) {
            if (wildCardMatch(child.url(), url)) {
                if (extension == null
                        || extension.equals(child.extensions())) {
                    result.add(child);
                }
            }
        }
        return result;
    }
    
     public static List<XPath> findURL(Site site, XPath current, String url)
            throws IOException {
        
        boolean root = url.startsWith("/");
        XPath rootPath;
        if (root) {
            rootPath = new XPath(site, site.source());
            url = url.substring(1);
        } else if (current.isDirectory()) {
            rootPath = current;
        } else {
            rootPath = current.getParent();
        }
        String[] parsedURL = url.split("/");
        return findURL(site, rootPath, parsedURL, 0);
    }

    public static List<XPath> findURL(Site site, XPath current, String[] url, int i)
            throws IOException {
        
                
        List<XPath> matches = new ArrayList<>();
        List<XPath> results = new ArrayList<>();
        
        if(url[i].equals("..")) {
            current = current.getParent();
            matches.add(current);
        }
        else {
            List<XPath> children = Utils.getNonHiddenChildren(site, current.path());
            if(url[i].equals("*")) {
                matches.addAll(children);
            } else {
                for(XPath child:children) {
                    if(child.url().equals(url[i])) {
                        matches.add(child);
                    }
                }
            }
        }
        if(url.length == (i + 1)) {
            return matches;
        } else {
            for(XPath match: matches) {
                if(match.isDirectory()) {
                    results.addAll(findURL(site, match, url, i + 1));
                } else {
                    //no match
                }
            }
        }
        return results;
    }

    /**
     * Returns a list of links that goes to the root. This is typically used for breadcrumbs.
     *
     * @param current The current location
     * @return the list of links
     */
    public static List<Link> linkToRoot(Path root, XPath xPath) {
        // if we are root, return an empty list
        if (xPath == null) {
            return Collections.emptyList();
        }
        List<XPath> xPaths = new ArrayList<>();
        while (!root.equals(xPath.path())) {
            xPaths.add(0, xPath);
            xPath = new XPath(xPath.site(), xPath.path().getParent());
        }

        List<Link> retVal = new ArrayList<>();
        Link old = null;
        for (XPath xPath2 : xPaths) {
            Link link = new Link(xPath2, old);
            retVal.add(link);
            old = link;
        }
        return retVal;
    }

    public static Link setSelected(List<Link> pathToRoot, Link root) {
        Link copy = root.copy();
        List<Link> pathToRootCopy = new ArrayList<>();
        pathToRootCopy.addAll(pathToRoot);
        setSelectedRec(pathToRootCopy, copy);
        return copy;
    }

    public static void setSelectedRec(List<Link> pathToRoot, Link root) {
        if (pathToRoot.size() == 0) {
            return;
        }

        Link current = pathToRoot.remove(0);
        for (Link children : root.getChildren()) {
            if (children.equals(current)) {
                children.setSelected(true);
            }
            setSelectedRec(pathToRoot, children);
        }
    }

    public static String postApplyTemplate(String html,
            Map<String, Object> model, String... string) {
        for (String key : string) {
            if (!model.containsKey(key) || model.get(key) == null) {
                LOG.info("cannot find key {} in html{}", key, html);
                continue;
            }
            // TODO: regexp would be better
            html = html.replace("${" + key + "}", model.get(key).toString());
        }
        return html;
    }

    /**
     * The automatic sort detection does detect if its sorting with number up to 1000 (e.g. 1,2,3), which is
     * sorted ascending. If its a date, the number is much higher and the sorting is descending (newest
     * first). If there is a mix of number (e.g., 1,3,234534547545) it will be sorted descending.
     *
     * @param children The files in that directory. Only visible files are considered.
     * @return True if all numbers of visible files are smaller or equal than 1000, false otherwise.
     */
    public static boolean guessAutoSort(List<XPath> children) {
        for (XPath xpath : children) {
            if (xpath.nr() > 1000) {
                return false;
            }
        }
        return true;
    }
    
    static boolean guessAutoSort1(List<XItem> results) {
        for (XItem xItem : results) {
            if (xItem.xPath().nr() > 1000) {
                return false;
            }
        }
        return true;
    }

    /**
     * Counts all visible documents in a list. The visible documents start with either a number or a date ->
     * 1|bla... or 2013-01-01|bla...
     *
     * @param children The list of all files to consider
     * @return The number of visible elements.
     */
    public static int countVisibleDocmuntes(List<XPath> children) {
        int counter = 0;
        for (XPath xpath : children) {
            if (xpath.isVisible()) {
                counter++;
            }
        }
        return counter;
    }

    public static void copyModelValues(Map<String, Object> modelDestination,
            Map<String, Object> modelSource, String... keys) {
        for (String key : keys) {
            modelDestination.put(key, modelSource.get(key));
        }
    }

    public static XItem createDocument(Site site, XPath xPath,
            String htmlContent, String template) throws IOException {
        TemplateBean templateText = site.getTemplate(template, xPath.getLayoutSuffix());
        // create the document
        XItem.Generator documentGenerator = new XItem.Generator(site,
                templateText);
        String documentURL = xPath.getTargetURL() + ".html";
        XItem doc = new XItem(xPath, documentGenerator, documentURL);
        doc.setHTML(htmlContent);
        doc.setTemplate(template);
        return doc;
    }
    
    public static void writeListHTML(Site site, XPath xPath, XItem doc, Path generatedFile) 
            throws IOException, TemplateException {
        
        //adjust path
        String minusPath = xPath.getTargetURL();
        doc = Utils.adjustPath(doc, minusPath);
        String minusPathToRoot = xPath.originalPathToRoot();
        doc = Utils.adjustPathToRoot(doc, minusPathToRoot);
        doc = Utils.adjustPromotedDepth(doc, doc.getPromoteDepthOriginal());
        
        String htmlSite = doc.getContent();
        Files.createDirectories(generatedFile.getParent());
        Utils.write(htmlSite, xPath, generatedFile);
    }

    public static void writeHTML(Site site, XPath xPath, XItem doc, Path generatedFile) 
            throws IOException, TemplateException {
        
         //adjust path
        String minusPath = xPath.getTargetURLPath();
        doc = Utils.adjustPath(doc, minusPath);
        String minusPathToRoot = xPath.originalPathToRoot();
        doc = Utils.adjustPathToRoot(doc, minusPathToRoot);
        
        String htmlSite = doc.getContent();
        Files.createDirectories(generatedFile.getParent());
        Utils.write(htmlSite, xPath, generatedFile);
    }

    public static String[] paging(XPath xPath, int pages) {
        String[] pagesURLs = new String[pages + 1];
        for (int i = 0; i <= pages; i++) {
            // first is special, no _ in the URL
            if (i == 0) {
                pagesURLs[i] = xPath.resolveTargetURL("index.html");
            } else {
                pagesURLs[i] = xPath.resolveTargetURL("index_" + i + ".html");
            }
        }
        return pagesURLs;
    }

    public static List<List<XItem>> split(List<XItem> documents,
            int pages, int pageSize) {
        List<List<XItem>> result = new ArrayList<>();
        if (pages == 0) {
            result.add(documents);
            return result;
        } else {
            for (int i = 0; i <= pages; i++) {
                int start = i * pageSize;
                int stop = start + pageSize;
                result.add(documents.subList(start,
                        documents.size() < stop ? documents.size() : stop));
            }
        }
        return result;
    }

    

}
