package net.xdocc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import net.xdocc.handlers.Handler;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

@Accessors(chain = true, fluent = true)
public class XPath implements Comparable<XPath> {

    private static final Logger LOG = LoggerFactory.getLogger(XPath.class);

    private final static Pattern PATTERN_NUMBER = Pattern.compile("^([0-9]+)");

    private final static Pattern PATTERN_DATE = Pattern
            .compile("^([0-9]{4}-[0-9]{2}-[0-9]{2})");

    private final static Pattern PATTERN_DATETIME = Pattern
            .compile("^([0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}:[0-9]{2}:[0-9]{2})");

    private final static Pattern PATTERN_URL = Pattern
            .compile("([^/|.]*)([.]|[|]|$)");

    @Getter
    private final Path path;
    //relative path!
    public static final String PATH = "path";

    @Getter
    private final Site site;

    @Getter
    private final String filename;
    public static final String FILENAME = "filename";
    
    @Getter
    private final long filesize;
    public static final String FILESIZE = "filesize";
    
    @Getter
    private final long filescount;
    public static final String FILESCOUNT = "filescount";
    
    @Getter
    private final List<String> extensionList = new ArrayList<>(2);
    
    @Getter
    private final Map<String, String> properties = new HashMap<>();
    

    @Getter
    @Setter
    private Date date;
    public static final String DATE = "date";

    @Getter
    @Setter
    private long nr;
    public static final String NR = "nr";

    @Getter
    @Setter
    private String extensions;

    @Getter
    @Setter
    private String name = "";
    public static final String NAME = "name";

    @Getter
    @Setter
    private String url;
    public static final String URL = "url";

    @Getter
    @Setter
    private boolean visible;
    public static final String VISIBLE = "visible";

    /**
     * Creates a xPath object from a path. The path will be parsed and information will be extracted.
     *
     * @param site The site where information about the source or web folder is stored
     * @param path The path to parse
     * @throws IllegalArgumentException if the path is not inside the context of site
     */
    public XPath(Site site, Path path) {
        if (!Utils.isChild(path, site.source())) {
            throw new IllegalArgumentException(path + "is not a child of "
                    + site.source());
        }
        this.path = path;
        this.site = site;
        
        this.filesize = getFileSize();
        this.filename = getFileName();
        this.filescount = getFilesCount();
        String extensionFilteredFileName = findKnownExtensions(site, filename);
        if (extensionList.size() > 0 || Files.isDirectory(path)) {
            this.visible = parse(extensionFilteredFileName, site.source().equals(path));
        } else {
            this.url = filename;
            this.visible = false;
        }
        if (Files.isRegularFile(path)) {
            parseFrontmatter();
        } else if (Files.isDirectory(path)) {
            readFrontmatter();
        } else {
            LOG.debug("The path [" + path + "] is not considered!");
        }
        LOG.debug("The path [" + path + "] was parsed to: nr=" + nr + ",name="
                + name + ",url=" + url);
    }

    private void readFrontmatter() {
        Path frontmatter = path.resolve(".xdocc");
        if (Files.exists(frontmatter)) {
            try {
                String content = FileUtils.readFileToString(frontmatter
                        .toFile());
                try {
                    Yaml yaml = new Yaml();
                    Map<String, Object> map = (Map<String, Object>) yaml
                        .load(content);
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        properties.put(entry.getKey(), entry.getValue().toString());
                    }
                    return;
                } catch (Exception e) {
                    LOG.debug("cannot parse frontmatter", e);
                }
                //try a regular property file
                try {
                    final Properties p = new Properties();
                    p.load(new StringReader(content));
                    for (final String name: p.stringPropertyNames()) {
                        properties.put(name, p.getProperty(name));
                    }
                } catch (Exception e) {
                    LOG.debug("cannot parse property file", e);
                }
            } catch (IOException e) {
                LOG.debug("cannot parse frontmatter", e);
            }
        }
    }

    /**
     * As seen in http://stackoverflow.com/questions/11770077/parsing-yaml-front- matter-in-java
     */
    private void parseFrontmatter() {
        try (BufferedReader br = new BufferedReader(new FileReader(
                path.toFile()))) {
            String line = br.readLine();
            while (line != null && line.isEmpty()) {
                line = br.readLine();
            }
            if (line == null) {
                return;
            }
            if (!line.matches("[-]{3,}")) { // use at least three dashes
                LOG.debug("No YAML Front Matter");
                return;
            }
            final String delimiter = line;
            StringBuilder sb = new StringBuilder();
            line = br.readLine();
            while (line != null && !line.equals(delimiter)) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            if (line == null) {
                return;
            }
            Yaml yaml = new Yaml();
            Map<String, Object> map = (Map<String, Object>) yaml.load(sb
                    .toString());
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                properties.put(entry.getKey(), entry.getValue().toString());
            }

        } catch (IOException e) {
            LOG.error("cannot parse frontmatter", e);
        }

    }

    private String findKnownExtensions(Site site, String tmpFilename) {
        extensionList.clear();
        extensions = "";
        for (Handler handler : site.handlers()) {
            for (String extension : handler.knownExtensions()) {
                if (tmpFilename.endsWith("." + extension)) {
                    tmpFilename = tmpFilename
                            .substring(0, tmpFilename.length() - (extension.length() + 1));

                    extensionList.add(extension);
                    extensions = "." + extension + extensions;
                }
            }
        }

        return tmpFilename;
    }

    /**
     * Parses the filename and extracts information such as name, url, etc.
     *
     * @param name The filename
     * @return True if the file is visible
     */
    private boolean parse(String name, boolean root) {
        if (isHidden()) {
            return false;
        }

        try {
            int firstPipeIndex = name.indexOf('|');
            String mandatory = "";
            int nextPipeIndex = 0;
            int offset = 0;

            // first mandatory patterns
            if (firstPipeIndex == -1) {
                mandatory = name;
            } else {
                mandatory = name.substring(0, firstPipeIndex);
            }

            Matcher matcher1 = PATTERN_DATETIME.matcher(mandatory);
            Matcher matcher2 = PATTERN_DATE.matcher(mandatory);
            Matcher matcher3 = PATTERN_NUMBER.matcher(mandatory);

            // order
            if (isRoot()) {
                nr = 1;
            } else if (matcher1.find()) {
                SimpleDateFormat parserSDF = new SimpleDateFormat(
                        "yyyy-MM-dd_HH:mm:ss");
                try {
                    date = parserSDF.parse(matcher1.group(1));
                    nr = date.getTime();
                    offset = matcher1.end(1);
                } catch (ParseException e) {
                    LOG.error("Cannot parse date time: ", e);
                    return false;
                }
            } else if (matcher2.find()) {
                SimpleDateFormat parserSDF = new SimpleDateFormat(
                        "yyyy-MM-dd");
                try {
                    date = parserSDF.parse(matcher2.group(1));
                    nr = date.getTime();
                    offset = matcher2.end(1);
                } catch (ParseException e) {
                    LOG.error("Cannot parse date: ", e);
                    return false;
                }
            } else if (matcher3.find()) {
                try {
                    nr = Long.parseLong(matcher3.group(1));
                    offset = matcher3.end(1);
                } catch (NumberFormatException e) {
                    LOG.error("Cannot parse number: ", e);
                    return false;
                }
            } else {
                return false;
            }
            // mandatory '-'
            if (offset >= mandatory.length()
                    || (mandatory.charAt(offset) != '-' && !isRoot())) {
                return false;
            }
            // url
            if (!isRoot()) {
                Matcher matcher4 = PATTERN_URL.matcher(mandatory);
                if (matcher4.find(offset + 1)) {
                    this.url = matcher4.group(1);
                    offset = matcher4.end(1);
                } else {
                    this.url = "";
                }
            } else {
                this.url = mandatory;
            }
            //  tags
            if (firstPipeIndex != -1) {
                offset = firstPipeIndex;
                nextPipeIndex = name.indexOf('|', offset + 1);
            } else {
                offset = name.length();
            }

            while (name.length() > offset && offset >= 0) {
                // scan tag key & value
                String tagString = "";
                if (nextPipeIndex >= 0) {
                    tagString = name.substring(offset + 1, nextPipeIndex);
                } else {
                    tagString = name.substring(offset + 1, name.length());
                }
                String key = "";
                String value = "";
                StringTokenizer tokenizer = new StringTokenizer(tagString, "=");
                if (tokenizer.hasMoreTokens()) {
                    key = tokenizer.nextToken();
                }
                if (tokenizer.hasMoreTokens()) {
                    value = tokenizer.nextToken();
                    value = parseValue(key, value);
                    if (!key.equalsIgnoreCase("")
                            && !value.equalsIgnoreCase("")) {
                        if (key.equalsIgnoreCase("name")
                                || key.equalsIgnoreCase("n")) {
                            this.name = value;
                        } else {
                            properties.put(key, value);
                        }
                    }
                } else if (!key.equalsIgnoreCase("")) {
                    // tag [b] is the same as [l99=browse,c]
                    properties.put(key, null);
                }
                if (nextPipeIndex >= 0) {
                    offset = nextPipeIndex;
                    nextPipeIndex = name.indexOf('|', offset + 1);
                } else {
                    offset = name.length();
                }
            }

            return true;
        } catch (Exception e) {
            LOG.info("name: {} not valid, returning false parse()", name, e);
            return false;
        }
    }

    private String parseValue(String key, String value) {
        if (StringUtils.indexOfIgnoreCase("file", key) >= 0) {
            return value.replace(">", "/");
        }
        return value;
    }

    public List<String> getExtensionList() {
        return extensionList;
    }

    @Override
    public String toString() {
        return path.toString();
    }

    /**
     * @return True if the file should been compiled, e.g. compile from textile to html
     */
    public boolean isCompile() {
        return !isHidden() && isVisible() && !isRaw();
    }

    /**
     * @return True if a file is hidden and only visible in the source folder
     */
    public boolean isHidden() {
        if (getFileName().endsWith("~")) {
            return true;
        } else if (getFileName().startsWith(".")) {
            return true;
        }
        return false;
    }

    /**
     * @return The file name
     */
    

    public String getTargetURLFilename() {
        String[] paths = Utils.createURLSplit(site.source(), this);
        paths[paths.length - 1] = getFileName();
        String url = Utils.createURL(paths);
        return url;
    }

    /**
     * @return The url of the file that have been compiled
     */
    public String getTargetURL() {
        String[] paths = Utils.createURLSplit(site.source(), this);
        String url = Utils.createURL(paths);
        return url;
    }

    public String getTargetURLPath() {
        String[] paths = Utils.createURLSplit(site.source(), this);

        String[] tmp = new String[paths.length - 1];
        for (int i = 0; i < tmp.length; i++) {
            tmp[i] = paths[i];
        }
        String url = Utils.createURL(tmp);
        return url;

    }

    /**
     * @return The path of the file in the web folder for files that have been compiled
     */
    public Path getTargetPath(String url) {
        Path tmp = site.generated().resolve(url);
        return tmp;
    }

    /**
     * @param extension The extension to check
     * @return True if the exension is present in this filename
     */
    public boolean containsExtension(String extension) {
        return extensionList != null && extensionList.contains(extension);
    }

    public boolean isDirectory() {
        return Files.isDirectory(path);
    }

    public XPath getParent() {
        if (!Utils.isChild(path.getParent(), site.source())) {
            return null;
        }
        return new XPath(site, path.getParent());
    }

    public boolean isVisible() {
        // first check if property "copy" is somewhere
        if (isRaw()) {
            return false;
        }

        return visible;
    }

    public boolean isAscending() {
        return properties != null
                && (properties.containsKey("ascending") || properties
                .containsKey("asc"));
    }

    public boolean isDescending() {
        return properties != null
                && (properties.containsKey("descending") || properties
                .containsKey("desc"));
    }

    public boolean isAutoSort() {
        return !isDescending() && !isAscending();
    }

    public String getLayoutSuffix() {
        XPath parent = this;

        // maybe we are a file and the directory has a layout
        int level = 0;
        while (parent != null) {
            String[] layoutSuffix = parent.getPropertyRegexp("layout_([0-9]*)",
                    "l([0-9]*)");

            if (layoutSuffix != null) {
                final int depth;
                if (StringUtils.isEmpty(layoutSuffix[1])) {
                    depth = 1;
                } else {
                    depth = Integer.parseInt(layoutSuffix[1]);
                }
                if (depth >= level) {
                    if (!StringUtils.isEmpty(layoutSuffix[0])) {
                        return "_" + layoutSuffix[0];
                    } else {
                        return "";
                    }
                }
            }
            XPath old = parent;
            parent = parent.getParent();
            if (old.isDirectory()) {
                level++;
            }
        }
        return "";
    }

    public String getSizeIcon() {
        return getProperty("si", "size_icon");
    }

    public String getSizeNormal() {
        return getProperty("sn", "size_normal");
    }

    private boolean hasProperty(String... tagNames) {
        if (properties != null) {
            for (String tag : tagNames) {
                if (properties.containsKey(tag)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getProperty(String... tagNames) {
        if (properties != null) {
            for (String tag : tagNames) {
                if (properties.containsKey(tag)) {
                    return properties.get(tag);
                }
            }
        }
        return null;
    }

    private String[] getPropertyRegexp(String... kepRegexps) {
        String[] resultArray = null;
        String key = null;
        if (properties != null) {
            for (String kepRegexp : kepRegexps) {
                Pattern pattern = Pattern.compile(kepRegexp);
                for (String keyTest : properties.keySet()) {
                    Matcher m = pattern.matcher(keyTest);
                    if (m.matches()) {
                        key = keyTest;
                        resultArray = new String[1 + m.groupCount()];
                        for (int i = 0; i < m.groupCount(); i++) {
                            resultArray[1 + i] = m.group(1 + i);
                        }
                        break;
                    }
                }
            }
        }

        if (key != null && resultArray != null) {
            resultArray[0] = properties.get(key);
        }
        return resultArray;
    }

    //rendering of directory by extension
    public boolean isSummary() {
        return containsExtension("sum") || hasProperty("sum"); 
        //item always rendered, shows a short summary, title + abstract up to n words, or marker, 
        // or highlight and link
    }

    public boolean isPage() {
        return containsExtension("page") || hasProperty("page"); 
        //items not rendered, only directory page, no link
    }
    
    public boolean isLinkPage() { // default
        return containsExtension("linkpage") || containsExtension("link") ||
                hasProperty("linkpage") || hasProperty("link");
        //no short form as this is default
        //item always rendered, including directory page, and link
    }
    
    public boolean isList() {
        return containsExtension("list") || hasProperty("list");
        //item always rendered, shows a short summary, title and link
    }
    
    
    public boolean isRaw() { //all files are visible, but not compiled, layout set to browse
        return hasRecursiveProperty("raw") || hasRecursiveExtension("raw");
        //items not rendered, but link to the real file
        //special handling as its recursive
    }
    
    public boolean isItemWritten() {
        return !isRaw() && !isPage();
    }

    //ordering extensions, can be combined with the rendering or with other ordering extensions
    //from above -> sum_nav, s_n, list_high_nav
    public boolean isNavigation() {
        return containsExtension("nav") || hasProperty("nav");
    }

    public boolean isHighlight() {
        return hasProperty("highlight") || hasProperty("high") || 
                containsExtension("highlight") || containsExtension("high");
    }
    
    //visibility is recursive
    public boolean isAllVisible() { //all files are visible
        return hasRecursiveProperty("visible","vis") || hasRecursiveExtension("visible","vis");
    }
    
    public boolean isNoneVisible() { //nothing is visible
        return hasRecursiveProperty("none") || hasRecursiveExtension("none");
    }
    
    public boolean isRegularVisible() { //default 1-test -> is visible, rest not
        return hasRecursiveProperty("regular", "reg") || hasRecursiveExtension("regular", "reg");
    }
    
      

    public String resolveTargetURL(String string) {
        if (getTargetURL().isEmpty()) {
            return string;
        } else {
            return getTargetURL() + "/" + string;
        }
    }

    @Override
    public int compareTo(XPath o2) {
        long diff = nr() - o2.nr();
        if (diff != 0) {
            return diff > 0 ? 1 : -1;
        }
        if (name() != null && o2.name() != null) {
            diff = name().compareTo(o2.name());
            if (diff != 0) {
                return diff > 0 ? 1 : -1;
            }
        }
        if (getFileName() != null && o2.getFileName() != null) {
            diff = getFileName().compareTo(o2.getFileName());
            if (diff != 0) {
                return diff > 0 ? 1 : -1;
            }
        }
        return path.compareTo(o2.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    public boolean isRoot() {
        return path().equals(site.source());
    }

    public int getPageSize() {
        String pagesString = getProperty("p", "pages");
        if (pagesString == null) {
            return 0;
        }
        try {
            return Integer.parseInt(pagesString.trim());
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    /**
     * Search a property the hierarchy up, starting at "this"
     *
     * @param names
     * @return
     */
    public String getRecursiveProperty(String... names) {
        XPath current = this;
        do {
            String property = current.getProperty(names);
            if (property != null) {
                return property;
            }
        } while ((current = current.getParent()) != null);

        
        return null;
    }
    
     public boolean hasRecursiveProperty(String... names) {
        XPath current = this;
        do {
            String property = current.getProperty(names);
            if (current.hasProperty(names)) {
                return true;
            }
        } while ((current = current.getParent()) != null);

        return false;
    }
    
     public boolean hasRecursiveExtension(String... names) {
        XPath parent = getParent();
        for(String name:names) {
            if (extensionList != null && extensionList.contains(name)) {
                return true;
            }
        }
        for(String name:names) {
            while (parent != null) {
                if (parent.extensionList != null && parent.extensionList.contains(name)) {
                    return true;
                }
                parent = parent.getParent();
            }
        }
        return false;
    }

    private long getFileSize() {
        try {
            if (!Files.isDirectory(path)) {
                return Files.size(path);
            } else {
                return 0;
            }
        } catch (IOException ex) {
            LOG.error("[" + path + "], file size not able to determine");
            return -1;
        }
    }
    
    private long getFilesCount() {
        try {
            if (Files.isDirectory(path)) {
                return Files.walk(path).count();
            } else {
                return 1;
            }
        } catch (IOException ex) {
            LOG.error("[" + path + "], file size not able to determine");
            return -1;
        }
    }
    
    public String getFileName() {
        if (path.getFileName() == null) {
            LOG.error("[" + path + "], cannot deal with an empty path");
            throw new RuntimeException("cannot deal with an empty path");
        }
        return path.getFileName().toString();
    }

    public String relativePath(Site site) {
        return Utils.relativePathToRoot(site.source(), path);
    }
}
