package net.xdocc;

import java.io.*;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

@Accessors(chain = true, fluent = true)
final public class XPath implements Comparable<XPath>, Serializable {

	private static final long serialVersionUID = -3757002496981209774L;

	private static final Logger LOG = LoggerFactory.getLogger(XPath.class);

    private final static Pattern PATTERN_NUMBER = Pattern.compile("^([0-9]+)");

    private final static Pattern PATTERN_DATE = Pattern
            .compile("^([0-9]{4}-[0-9]{2}-[0-9]{2})");

    private final static Pattern PATTERN_DATETIME = Pattern
            .compile("^([0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}:[0-9]{2}:[0-9]{2})");

    private final static Pattern PATTERN_URL = Pattern
            .compile("([^/|.]*)([.]|[|]|$)");
    
    private final static List<String> KNOWN_EXTENSIONS = new ArrayList<>();

    @Getter
    private final String path;

    @Getter
    private final Site site; 
    
    @Getter
    private final List<String> extensionList = new ArrayList<>(2);
    public static final String EXTENSION_LIST = "extensionlist";
    
    @Getter
    private final Map<String, String> properties = new HashMap<>();
    public static final String PROPERTIES = "properties";

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
    public static final String EXTENSIONS = "extensions";

    @Getter
    @Setter
    private String name = "";
    public static final String NAME = "name";

    @Getter
    @Setter
    private String url;
    public static final String URL = "url";

    
    private boolean visible;

    /**
     * Creates a xPath object from a path. The path will be parsed and information will be extracted.
     *
     * @param site The site where information about the source or web folder is stored
     * @param path The path to parse
     * @throws IllegalArgumentException if the path is not inside the context of site
     */
    public XPath(Site site, Path path) {
        if (!Utils.isChild(path, Paths.get(site.source()))) {
            throw new IllegalArgumentException(path + " is not a child of "
                    + site.source());
        }
        this.path = path.toString();
        this.site = site;
        
        String extensionFilteredFileName = findKnownExtensions(site, fileName());
        if (extensionList.size() > 0 || Files.isDirectory(path)) {
            this.visible = parse(extensionFilteredFileName, site.source().equals(path));
        } else {
            this.visible = false;
        }
        
        if(!this.visible) {
             this.url = extensionFilteredFileName;
             extractName();
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
        Path frontmatter = Paths.get(path).resolve(".xdocc");
        if (Files.exists(frontmatter)) {
            try {
                String content = FileUtils.readFileToString(frontmatter
                        .toFile());
                try {
                    Yaml yaml = new Yaml();
                    Object obj = yaml.load(content);
                    if(obj == null) {
                        LOG.debug("frontmatter is empty");
                    } else if(!(obj instanceof Map)) {
                        LOG.debug("frontmatter is not a map");
                    } else {
                        @SuppressWarnings("unchecked")
						Map<String, Object> map = (Map<String, Object>) obj;
                        for (Map.Entry<String, Object> entry : map.entrySet()) {
                            properties.put(entry.getKey(), entry.getValue().toString());
                        }
                        return;
                    }

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
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
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
            @SuppressWarnings("unchecked")
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
        int len1 = site.handlers().size();
        
        for (int i = 0; i < len1; i++) {
            int len2 = site.handlers().get(i).knownExtensions().size();
            for (int j = 0; j < len2; j++) {
                String extension = site.handlers().get(i).knownExtensions().get(j);
                if (tmpFilename.endsWith("." + extension)) {
                    tmpFilename = tmpFilename
                            .substring(0, tmpFilename.length() - (extension.length() + 1));

                    extensionList.add(extension);
                    extensions = "." + extension + extensions;
                    i = j = 0;
                    break;
                }
            }
        }
        int len3 = KNOWN_EXTENSIONS.size();
        for (int i = 0; i < len3; i++) {
            if (tmpFilename.endsWith("." + KNOWN_EXTENSIONS.get(i))) {
                tmpFilename = tmpFilename
                            .substring(0, tmpFilename.length() - (KNOWN_EXTENSIONS.get(i).length() + 1));

                    extensionList.add(KNOWN_EXTENSIONS.get(i));
                    extensions = "." + KNOWN_EXTENSIONS.get(i) + extensions;
                i = 0;
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
                    extractName();
                    offset = matcher4.end(1);
                } else {
                    this.url = "";
                }
            } else {
                this.url = mandatory;
                extractName();
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
    
    public String fileName() {
        Path p = Paths.get(path);
        if (p.getFileName() == null) {
            LOG.error("[" + path + "], cannot deal with an empty path");
            throw new RuntimeException("cannot deal with an empty path");
        }
        return p.getFileName().toString();
    }
    public static final String FILENAME = "filename";

    public String getTargetURLName() {
        String[] paths = Utils.createURLSplit(Paths.get(site.source()), this);
        return paths[paths.length - 1];
    }
    
    public String getTargetURLFilename() {
        String[] paths = Utils.createURLSplit(Paths.get(site.source()), this);
        paths[paths.length - 1] = fileName();
        String url = Utils.createURL(paths);
        return url;
    }

    /**
     * @return The url of the file that have been compiled
     */
    public String getTargetURL() {
        String[] paths = Utils.createURLSplit(Paths.get(site.source()), this);
        String url = Utils.createURL(paths);
        return url;
    }
    
    public int getTargetDepth() {
        String[] paths = Utils.createURLSplit(Paths.get(site.source()), this);
        return paths.length - (isDirectory() ? 0 : 1);
    }

    public String getTargetURLPath() {
        String[] paths = Utils.createURLSplit(Paths.get(site.source()), this);

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
    public Path resolveTargetFromBasePath(String url) {
        Path gen = Paths.get(site.generated());
        Path tmp = gen.resolve(url);
        return tmp;
    }
    
    public Path resolveTargetFromPath(String url) {
        Path p = Paths.get(path);
        Path target = Paths.get(getTargetURL());
        Path gen = Paths.get(site.generated());
        if(Files.isDirectory(p)) {
            return gen.resolve(target.resolve(url));
        } else {
            return gen.resolve(target.getParent().resolve(url));
        }
    }
    
    public XPath resolveSource(String url) {
        Path p = Paths.get(path);
        return new XPath(site, p.resolve(url));
    }

    /**
     * @param extension The extension to check
     * @return True if the exension is present in this filename
     */
    public boolean containsExtension(String extension) {
        return extensionList != null && extensionList.contains(extension);
    }
    
    public XPath getParent() {
        Path p = Paths.get(path);
        if (!Utils.isChild(p.getParent(), Paths.get(site.source()))) {
            return null;
        }
        return new XPath(site, p.getParent());
    }
    
    /**
     * @return True if the file should been compiled, e.g. compile from textile to html
     */
    public boolean isCompile() {
        return !isHidden() && isVisible();
    }
    public static final String IS_COMPILE = "iscompile";

    
    /**
     * @return True if a file is hidden and only visible in the source folder
     */
    public boolean isHidden() {
        if (fileName().endsWith("~")) {
            return true;
        } else if (fileName().startsWith(".")) {
            return true;
        }
        return hasRecursiveProperty("hidden") || hasRecursiveExtension("hide");
    }
    public static final String IS_HIDDEN = "ishidden";
    static {KNOWN_EXTENSIONS.add("hide");}

    
    public boolean isDirectory() {
        Path p = Paths.get(path);
        return Files.isDirectory(p);
    }
    public static final String IS_DIRECTORY = "isdirectory";

    
    public boolean isVisible() {
        // first check if property "copy" is somewhere
        if (isCopy()) {
            return false;
        }
        //all non hidden files are visible (also those without 1-blabla) and compiled
        if (hasRecursiveProperty("visible","vis") || hasRecursiveExtension("visible","vis")) {
            return true;
        }
        
        return visible;
    }
    public static final String IS_VISIBLE = "isvisible";
    static {KNOWN_EXTENSIONS.add("visible");KNOWN_EXTENSIONS.add("vis");}

    public boolean isAscending() {
        return properties != null
                && (properties.containsKey("ascending") || properties
                .containsKey("asc"));
    }
    public static final String IS_ASCENDING = "isascending";

    
    public boolean isDescending() {
        return properties != null
                && (properties.containsKey("descending") || properties
                .containsKey("desc"));
    }
    public static final String IS_DESCENDING = "isdescending";
    

    public boolean isAutoSort() {
        return !isDescending() && !isAscending();
    }
    public static final String IS_AUTOSORT = "isautosort";
    

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
                    depth = 0;
                } else {
                    depth = Integer.parseInt(layoutSuffix[1]);
                }
                if (depth >= level) {
                    if (!StringUtils.isEmpty(layoutSuffix[0])) {
                        return layoutSuffix[0];
                    } else {
                        return "";
                    }
                }
            }
            parent = parent.getParent();
            level++;
        }
        return "";
    }
    public static final String LAYOUT = "layout";

    private boolean isPropertyTrue(String... tagNames) {
        if (properties != null) {
            for (String tag : tagNames) {
                if (properties.containsKey(tag)) {
                    String value = properties.get(tag);
                    return !"false".equalsIgnoreCase(value);
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

    public String getPostProcessing() {
        return getProperty("post-processing", "pp");
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
    /**
     * 
     * root/1-dir/test.txt
     * root/1-dir/two.txt
     * 
     * if page is set, it will produce
     * root/dir/index.html (contains test, two)
     * 
     * if page is not set, it will produce
     * root/dir/index.html (contains test, two)
     * root/dir/test.html (only test)
     * root/dir/two.html (only two)
     */
    public boolean isPage() {
        return containsExtension("page") || isPropertyTrue("page"); 
        //items not rendered, only directory page, no link
    }
    public static final String IS_PAGE = "ispage";
    static {KNOWN_EXTENSIONS.add("page");}
    
    /**
     * 
     * root/1-dir/test.txt
     * root/1-dir/two.txt
     * 
     * if no index is set, it will produce
     * root/dir/test.html (only test)
     * root/dir/two.html (only two)
     * 
     * if no index is not set, it will produce
     * root/dir/index.html (contains test, two)
     * root/dir/test.html (only test)
     * root/dir/two.html (only two)
     */
    public boolean isNoIndex() {
        return containsExtension("noindex") || containsExtension("noidx") || isPropertyTrue("noindex") || isPropertyTrue("noidx"); 
        //items not rendered, only directory page, no link
    }
    public static final String IS_NOINDEX = "isnoindex";
    static {KNOWN_EXTENSIONS.add("noindex");KNOWN_EXTENSIONS.add("noidx");}
    
    //dealing with recursion: a directory that is promoted, will be a like a content page for the parent
    public boolean isPromoted() {
        return isPropertyTrue("promote") || isPropertyTrue("prm") || 
                containsExtension("promote") || containsExtension("prm");
    }
    public static final String IS_PROMOTED = "ispromoted";
    static {KNOWN_EXTENSIONS.add("promote");KNOWN_EXTENSIONS.add("prm");}
    
    
    
    
    public boolean isItemWritten() {
        return !isPage();
    }
    public static final String IS_WRITE = "iswrite";
    

    //ordering extensions, can be combined with the rendering or with other ordering extensions
    //from above -> sum_nav, s_n, list_high_nav
    public boolean isNavigation() {
        return containsExtension("nav") || isPropertyTrue("nav");
    }
    public static final String IS_NAVIGATION = "isnavigation";
    static {KNOWN_EXTENSIONS.add("nav");}
    

    public boolean isHighlight() {
        return isPropertyTrue("highlight") || isPropertyTrue("high") || 
                containsExtension("highlight") || containsExtension("high");
    }
    public static final String IS_HIGHLIGHT = "ishighlight";    
    static {KNOWN_EXTENSIONS.add("highlight");KNOWN_EXTENSIONS.add("high");}
    
    public boolean isCopy() { //nothing is visible (not hidden), everything is copied (not compiled)
        return hasRecursiveProperty("copy") || hasRecursiveExtension("copy");
    }
    public static final String IS_COPY = "iscopy";
    static {KNOWN_EXTENSIONS.add("copy");}
    
    
    public boolean isKeep() { //nothing is visible (not hidden), everything is copied (not compiled)
        return hasRecursiveProperty("keep", "keep_orig") || hasRecursiveExtension("keep", "keep_orig");
    }
    public static final String IS_KEEP = "iskeep";
    static {KNOWN_EXTENSIONS.add("keep");KNOWN_EXTENSIONS.add("keep_orig");}
    static {KNOWN_EXTENSIONS.add("docbook");KNOWN_EXTENSIONS.add("Docbook");KNOWN_EXTENSIONS.add("DOCBOOK");}

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
        if (fileName() != null && o2.fileName() != null) {
            diff = fileName().compareTo(o2.fileName());
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

    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj == null) {
            return false;
        }
        if(!(obj instanceof XPath)) {
            return false;
        }
        
        XPath other = (XPath) obj;
        
        return compareTo(other) == 0;
    }
    
    

    public boolean isRoot() {
        return path().equals(site.source());
    }
    public static final String IS_ROOT = "isroot";
    

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
    public static final String PAGES = "pages";

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
            if (current.isPropertyTrue(names)) {
                return true;
            }
        } while ((current = current.getParent()) != null);

        return false;
    }
    
     public boolean hasRecursiveExtension(String... names) {
        XPath current = this;
        do {
            for(String name:names) {
                if (current.extensionList != null && current.extensionList.contains(name)) {
                    return true;
                }
            }
        } while ((current = current.getParent()) != null);

        return false;
    }

    public long fileSize() {
        Path p = Paths.get(path);
        try {
            if (!Files.isDirectory(p)) {
                return Files.size(p);
            } else {
                return 0;
            }
        } catch (IOException ex) {
            LOG.error("[" + path + "], file size not able to determine");
            return -1;
        }
    }
    public static final String FILESIZE = "filesize";
    
    public long filesCount() {
        Path p = Paths.get(path);
        try {
            if (Files.isDirectory(p)) {
                return Files.walk(p).count();
            } else {
                return 1;
            }
        } catch (IOException ex) {
            LOG.error("[" + path + "], file size not able to determine");
            return -1;
        }
    }
    public static final String FILESCOUNT = "filescount";
    
    

    public String originalPath() {
        return isDirectory() ? getTargetURL() : getTargetURLPath();
    }
    public static final String ORIGINAL_PATH = "originalpath";
    
    public String originalRoot() {
        Path p = Paths.get(path);
        return Utils.relativePathToRoot(Paths.get(site.source()), p);
    }
    public static final String ORIGINAL_ROOT = "originalroot";

    private void extractName() {
        if(url.contains(":")) {
            String[] tmp = url.split(":", 2);
            this.url = tmp[0];
            this.name = tmp[1];
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(nr);
        sb.append("-");
        sb.append(name);
        sb.append("/u:");
        sb.append(url);
        return sb.toString();
    }
}
