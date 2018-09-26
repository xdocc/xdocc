package net.xdocc;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.TemplateException;
import javassist.compiler.SyntaxError;

import java.nio.file.Paths;
import java.util.regex.Pattern;

public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
    private static Map<Path, Path> created = new HashMap<Path, Path>();

    private static XItem adjustPath(XItem doc, String minusPath) {
        adjustPath0(doc, minusPath);
        for (XItem item : doc.getItems().values()) {
            adjustPath(item, minusPath);
        }
        return doc;
    }

    private static XItem adjustPath0(XItem doc, String minusPath) {
        String path = doc.getOriginalPath();
        Path pathRelative = Paths.get(minusPath).relativize(Paths.get(path));
        path = pathRelative.toString();
        path = path.isEmpty() ? "." : path;
        doc.setPath(path);

        String link = doc.getOriginalLink();
        if (link != null) {
            Path pathRelativeLink = Paths.get(minusPath).relativize(Paths.get(link));
            link = pathRelativeLink.toString();
            link = link.isEmpty() ? "." : link;
            doc.setLink(link);
        }

        return doc;
    }

    private static XItem adjustPathToRoot(XItem doc, String newPathToRoot) {
        adjustPathToRoot0(doc, newPathToRoot);
        for (XItem item : doc.getItems().values()) {
            adjustPathToRoot(item, newPathToRoot);
        }
        return doc;
    }

    private static XItem adjustPathToRoot0(XItem doc, String newPathToRoot) {
        newPathToRoot = newPathToRoot.isEmpty() ? "." : newPathToRoot;
        doc.setPathToRoot(newPathToRoot);
        return doc;
    }

    public static Collection<Path> listPathsGen(Site site, Path generatedFile) {

        generatedFile = generatedFile.normalize();

        if (!isChild(generatedFile, Paths.get(site.generated()))) {
            return null;
        }

        Collection<Path> retVal = new ArrayList<>();
        while (!generatedFile.equals(Paths.get(site.generated()))) {
            retVal.add(generatedFile);
            generatedFile = generatedFile.getParent();
        }
        retVal.add(generatedFile);
        return retVal;
    }

    public static Collection<Path> listPathsSrc(Site site, Path srcFile) {

        srcFile = srcFile.normalize();

        if (!isChild(srcFile, Paths.get(site.source()))) {
            return null;
        }

        Collection<Path> retVal = new ArrayList<>();
        while (!srcFile.equals(Paths.get(site.source()))) {
            retVal.add(srcFile);
            srcFile = srcFile.getParent();
        }
        retVal.add(srcFile);
        return retVal;
    }

    public static void increase(Map<String, Integer> filesCounter, Collection<Path> listPaths) {
        for (Path path : listPaths) {
            synchronized (filesCounter) {
                Integer i = filesCounter.get(path.toString());
                if (i == null) {
                    i = 1;
                } else {
                    i++;
                }

                filesCounter.put(path.toString(), i);
            }
        }
    }

    public static void decrease(Map<Path, Integer> filesCounter, Collection<Path> listPaths) {
        for (Path path : listPaths) {
            synchronized (filesCounter) {
                Integer i = filesCounter.get(path);
                if (i == null) {
                    i = 0;
                } else {
                    i--;
                }

                filesCounter.put(path, i);
            }
        }
    }

    public static enum OS_TYPE {
        LINUX, WIN, MAC, OTHER
    }

    ;

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

    public static boolean isChild(Link parent, Link maybeChild) {
        if (parent.equals(maybeChild)) {
            return true;
        }
        for (Link child : parent.getChildren()) {
            if (isChild(child, maybeChild)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isChild(Path maybeChild, Path possibleParent) {
        return maybeChild.normalize().startsWith(possibleParent.normalize());
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

    public static String[] createURLSplit(Path source, XPath xPath) {
        List<String> paths = new ArrayList<>();
        while (!source.equals(Paths.get(xPath.path()))) {
            paths.add(0, xPath.url());
            xPath = XPath.get(xPath.site(), Paths.get(xPath.path()).getParent());
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
                            XPath xPath = XPath.get(site, dir);
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
                            XPath xPath = XPath.get(site, file);
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

    /**
     * Sort the documents according to its number. If a date was provided, the date will be converted to a
     * long. If no number is provided the sort will be by name.
     *
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
        //create generated file and paths if not done yet
        createDirectories(generatedFile);
        Path alreadyGeneratedSource = created.get(generatedFile);
        if (alreadyGeneratedSource == null) {
            created.put(generatedFile, Paths.get(xPath.path()));
        } else if (!alreadyGeneratedSource.equals(Paths.get(xPath.path()))) {
            LOG.warn("create " + generatedFile
                    + ", but it was already created by "
                    + alreadyGeneratedSource + ". Anyway we will overwrite");
        } else {
            LOG.debug("overwriting with a new version for {}", generatedFile);
        }

        System.err.println("writing " + xPath + " for " + generatedFile);

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
        final int previewSize = 100;
        final StringBuilder sb = new StringBuilder(
                "<table id=\"debug\" border=\"1\">" +
                        "<th colspan=\"2\">Templates can access the following properties:</th>\n");
        SortedMap<String, Object> map = new TreeMap<>();
        for (Map.Entry<String, Object> entry : model.entrySet()) {
            //if ("debug".equals(entry.getKey())) {
            // /   continue;
            //}
            map.put(entry.getKey(), entry.getValue());
        }
        for(Map.Entry<String, Object> entry:map.entrySet())
        {
            sb.append("<tr><td>");
            sb.append(escapeHtml4(entry.getKey()));
            sb.append("</td><td title=\"");
            String value = entry.getValue() == null ? "null" : entry.getValue()
                    .toString();
            sb.append(escapeAttribute(value));
            sb.append("\">");
            value = escapeHtml4(value);
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

            /*templateText
                    .template()
                    .getConfiguration()
                    .setDirectoryForTemplateLoading(site.templatePath().toFile());
            templateText.template().getConfiguration()
                    .setCacheStorage(new NullCacheStorage());
            templateText
                    .template()
                    .getConfiguration()
                    .setTemplateLoader(
                            new FileTemplateLoader(site.templatePath().toFile()));*/
            try {
                templateText.template().process(model, sw);
            } catch (Throwable e) {
                LOG.debug("available data for template {}:", templateText.file(), e);
                for (Map.Entry<String, Object> entry : model.entrySet()) {
                    if(entry.getValue() != null) {
                        String val = entry.getValue().toString();
                        if(val.length() > 60) {
                            val = val.substring(0, 60) + "...";
                        }
                        val = val.replace("\n", "");
                        LOG.debug("key:[{}]=[{}]", entry.getKey(), val);
                    } else {
                        LOG.debug("key:[{}]=null", entry.getKey());
                    }
                }
            }

            sw.flush();
            return sw.getBuffer().toString();
        }

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

    /**
     * Performs a wildcard matching for the text and pattern provided.
     *
     * @param text    the text to be tested for matches.
     * @param pattern the pattern to be matched for. This can contain the wildcard character '*' (asterisk).
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

    public static List<XPath> findURL(Site site, XPath current, String url)
            throws IOException {

        boolean root = url.startsWith("/");
        XPath rootPath;
        if (root) {
            rootPath = XPath.get(site, Paths.get(site.source()));
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

        if (url[i].equals("..")) {
            current = current.getParent();
            matches.add(current);
        } else {
            List<XPath> children = Utils.getNonHiddenChildren(site, Paths.get(current.path()));
            if (url[i].equals("*")) {
                matches.addAll(children);
            } else {
                for (XPath child : children) {
                    if (child.url().equals(url[i])) {
                        matches.add(child);
                    }
                }
            }
        }
        if (url.length == (i + 1)) {
            return matches;
        } else {
            for (XPath match : matches) {
                if (match.isDirectory()) {
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
     * @return the list of links
     */
    public static List<Link> linkToRoot(Path root, XPath xPath) {
        // if we are root, return an empty list
        if (xPath == null) {
            return Collections.emptyList();
        }
        if (!xPath.isDirectory()) {
            xPath = xPath.getParent();
        }
        List<XPath> xPaths = new ArrayList<>();
        while (!root.equals(Paths.get(xPath.path()))) {
            xPaths.add(0, xPath);
            xPath = XPath.get(xPath.site(), Paths.get(xPath.path()).getParent());
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

    public static String postApplyTemplate(String html,
                                           Map<String, Object> model, String... string) {
        for (String key : string) {
            if (!model.containsKey(key) || model.get(key) == null) {
                //LOG.debug("cannot find key {} in html{}", key, html);
                continue;
            }
            // TODO: regexp would be better
            if(model.get(key) instanceof Date) {
                SimpleDateFormat dt1 = new SimpleDateFormat("dd.MM.yyyy");
                html = html.replace("${" + key + "}", dt1.format((Date) model.get(key)));
            } else {
                html = html.replace("${" + key + "}", model.get(key).toString());
            }
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

    public static boolean guessAutoSort1(List<XItem> results) {
        for (XItem xItem : results) {
            if (xItem.xPath().nr() > 1000) {
                return false;
            }
        }
        return true;
    }

    public static XItem createDocument(Site site, XPath xPath,
                                       String htmlContent, String template) throws IOException {
        TemplateBean templateText = site.getTemplate(template);
        // create the document
        XItem.Generator documentGenerator = new XItem.FillGenerator(site, templateText);

        XItem doc = new XItem(xPath, documentGenerator);
        doc.setHTML(htmlContent);
        doc.setTemplate(template);
        doc.setLayout(xPath.getLayoutSuffix());
        System.out.println("layout is "+xPath.getLayoutSuffix()+" xpath: "+xPath);
        return doc;
    }

    public static void writeListHTML(XPath xPath, XItem doc, Path generatedFile)
            throws IOException, TemplateException {

        //adjust path in current item
        String minusPath = xPath.getTargetURL();
        doc = Utils.adjustPath(doc, minusPath);
        String minusPathToRoot = xPath.originalRoot();
        doc = Utils.adjustPathToRoot(doc, minusPathToRoot);

        //adjust path in page item
        String htmlSite = doc.getContent();
        XItem page = Utils.createDocument(xPath.site(), xPath, htmlSite, "page");

        page.setDepth(doc.getDepth());
        page = Utils.adjustPath(page, minusPath);
        page = Utils.adjustPathToRoot(page, minusPathToRoot);

        Utils.write(page.getContent(), xPath, generatedFile);
    }

    public static void createDirectories(Path path) throws IOException {
        if(Files.isDirectory(path)) {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } else {
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
        }
    }

    public static XItem adjust(XPath xPath, XItem doc) {
        String minusPath = xPath.getTargetURLPath();
        doc = Utils.adjustPath(doc, minusPath);
        String minusPathToRoot = xPath.originalRoot();
        doc = Utils.adjustPathToRoot(doc, minusPathToRoot);
        return doc;
    }

    public static void writeHTML(XPath xPath, XItem doc, Path generatedFile)
            throws IOException, TemplateException {

        //adjust path in current item
        String minusPath = xPath.getTargetURLPath();
        doc = Utils.adjustPath(doc, minusPath);
        String minusPathToRoot = xPath.originalRoot();
        doc = Utils.adjustPathToRoot(doc, minusPathToRoot);


        String path = generatedFile.getFileName().toString();
        path = path.isEmpty() ? "." : path;
        doc.setUrl(path);


        //adjust path in page item
        String htmlSite = doc.getContent();
        XItem page = Utils.createDocument(xPath.site(), xPath, htmlSite, "page");
        page.setDepth(doc.getDepth());
        page.setUrl(path);
        page = Utils.adjustPath(page, minusPath);
        page = Utils.adjustPathToRoot(page, minusPathToRoot);

        Utils.write(page.getContent(), xPath, generatedFile);
    }

    public static String executeAndOutput(ProcessBuilder pb) throws IOException,
            InterruptedException {
        return executeAndOutput(pb, null);
    }

    public static String executeAndOutput(ProcessBuilder pb, String workingDirectory) throws InterruptedException {
        pb.redirectErrorStream(true);
        if (workingDirectory != null) {
            pb.directory(new File(workingDirectory));
        }

        try {
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
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            LOG.warn(sw.toString());
            return sw.toString(); // stack trace as a string
        }
    }


}
