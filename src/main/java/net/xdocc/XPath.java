package net.xdocc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import net.xdocc.handlers.Handler;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class XPath implements Comparable<XPath> {
	private static final Logger LOG = LoggerFactory.getLogger(XPath.class);

	private final static Pattern PATTERN_NUMBER = Pattern.compile("^([0-9]+)");

	private final static Pattern PATTERN_DATE = Pattern
			.compile("^([0-9]{4}-[0-9]{2}-[0-9]{2})");

	private final static Pattern PATTERN_DATETIME = Pattern
			.compile("^([0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{2}:[0-9]{2}:[0-9]{2})");

	private final static Pattern PATTERN_URL = Pattern
			.compile("([^/|.]*)([.]|[|]|$)");

	private final Path path;

	private final Site site;

	private final String filename;

	private Date date;

	private long nr;

	private String extensions;

	private List<String> extensionList;

	private String name = "";

	private String url;

	private Map<String, String> properties = new HashMap<>();
	
	private boolean visible;

	/**
	 * Creates a xPath object from a path. The path will be parsed and
	 * information will be extracted.
	 * 
	 * @param site
	 *            The site where information about the source or web folder is
	 *            stored
	 * @param path
	 *            The path to parse
	 * @throws IllegalArgumentException
	 *             if the path is not inside the context of site
	 */
	public XPath(Site site, Path path) {
		if (!Utils.isChild(path, site.getSource())) {
			throw new IllegalArgumentException(path + "is not a child of "
					+ site.getSource());
		}
		this.path = path;
		this.site = site;
		this.filename = getFileName();
		String extensionFilteredFileName = findKnownExtensions(site, filename);
		this.visible = parse(extensionFilteredFileName, site.getSource().equals(path));
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
				Yaml yaml = new Yaml();
				Map<String, Object> map = (Map<String, Object>) yaml
						.load(content);
				for (Map.Entry<String, Object> entry : map.entrySet()) {
					properties.put(entry.getKey(), entry.getValue().toString());
				}
			} catch (IOException e) {
				LOG.error("cannot parse frontmatter", e);
			}
		}
	}

	/**
	 * As seen in
	 * http://stackoverflow.com/questions/11770077/parsing-yaml-front-
	 * matter-in-java
	 */
	private void parseFrontmatter() {
		try (BufferedReader br = new BufferedReader(new FileReader(
				path.toFile()))) {
			String line = br.readLine();
			while (line != null && line.isEmpty())
				line = br.readLine();
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
		if (site.getHandlers() != null) {
			for (Handler handler : site.getHandlers()) {
				for (String extension : handler.knownExtensions()) {
					if (tmpFilename.endsWith("." + extension)) {
						tmpFilename = tmpFilename
								.substring(
										0,
										tmpFilename.length()
												- (extension.length() + 1));
						if (extensionList == null) {
							// we expect 1 or 2 entries
							extensionList = new ArrayList<>(2);
						}
						if (extensions == null) {
							extensions = "";
						}
						extensionList.add(extension);
						extensions = "." + extension + extensions;
					}
				}
			}
		}
		return tmpFilename;
	}

	/**
	 * Parses the filename and extracts information such as name, url, etc.
	 * 
	 * @param name
	 *            The filename
	 * @return True if the file is visible
	 */
	private boolean parse(String name, boolean root) {
		if (isHidden()) {
			return false;
		}

		try {
			int firstPipeIndex = name.indexOf('|');
			String url = "";
			String order = "";
			String mandatory = "";
			int lastDelimiterIndex = 0;
			int nextPipeIndex = 0;
			int offset = 0;
			// first mandatory patterns: order, url
			if (firstPipeIndex == -1) {
				mandatory = name;
				if (!isRoot()) {
					lastDelimiterIndex = mandatory.lastIndexOf('-');
					if (lastDelimiterIndex != -1) {
						url = mandatory.substring(lastDelimiterIndex + 1);
						order = mandatory.substring(0, lastDelimiterIndex);
					} else {
//						order = mandatory;
						return false;
					}
				} else {
					url = mandatory;
				}
			} else {
				mandatory = name.substring(0, firstPipeIndex);
				if (!isRoot()) {
					lastDelimiterIndex = mandatory.lastIndexOf('-');
					if (lastDelimiterIndex != -1) {
						order = mandatory.substring(0, lastDelimiterIndex);
						url = mandatory.substring(lastDelimiterIndex + 1);
					} else {
//						order = mandatory;
						return false;
					}
				} else {
					url = mandatory;
				}
			}
			Matcher matcher1 = PATTERN_DATETIME.matcher(order);
			Matcher matcher2 = PATTERN_DATE.matcher(order);
			Matcher matcher3 = PATTERN_NUMBER.matcher(order);

			// order
			if (root) {
				nr = 1;
			} else {
				if (matcher1.find()) {
					SimpleDateFormat parserSDF = new SimpleDateFormat(
							"yyyy-MM-dd_HH:mm:ss");
					try {
						date = parserSDF.parse(matcher1.group(1));
						nr = date.getTime();
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
					} catch (ParseException e) {
						LOG.error("Cannot parse date: ", e);
						return false;
					}
				} else if (matcher3.find()) {
					try {
						nr = Long.parseLong(matcher3.group(1));
					} catch (NumberFormatException e) {
						LOG.error("Cannot parse number: ", e);
						return false;
					}
				} else {
					return false;
				}
			}
			// url
			Matcher matcher4 = PATTERN_URL.matcher(url);
			if (matcher4.find()) {
				this.url = matcher4.group(1);
			}

			//  tags
			if (firstPipeIndex != -1) {
				offset = firstPipeIndex;
				nextPipeIndex = name.indexOf('|', offset + 1);
			} else {
				offset = name.length();
			}
			while (name.length() > offset && offset >= 0) {
				// check if already at end (extension)
				if (name.length() > offset + 1
						&& name.charAt(offset + 1) == '.') {
					// leading dot, we got an extension
					extensions = name.substring(offset + 1);
					extensionList = Utils.splitExtensions(extensions);
					return true;
				}
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
				} else {
					if (!key.equalsIgnoreCase("")) {
						// tag [b] is the same as [l99=browse,c]
						if (key.equals("b") || key.equals("browse")) {
							properties.put("l99", "browse");
							properties.put("c", null);
						} else {
							properties.put(key, null);
						}
					}
				}
				if (nextPipeIndex >= 0) {
					offset = nextPipeIndex;
					nextPipeIndex = name.indexOf('|', offset + 1);
				} else {
					offset = name.length();
				}
			}

			// lets go for the extension
			if (name.length() > offset && offset >= 0) {
				if (name.charAt(offset) == '.') {
					// leading dot, we got an extension
					extensions = name.substring(offset);
					extensionList = Utils.splitExtensions(extensions);
					return true;
				}
				int index = name.indexOf(".", offset);
				if (index > 0) {
					LOG.warn("There seems to be an extension, but it was not found properly: "
							+ name);
					extensions = name.substring(index);
					extensionList = Utils.splitExtensions(extensions);
					return true;
				}
			}
			return true;
		} catch (Exception e) {
			LOG.info("name: " + name + " not valid, returning false parse()");
			return false;
		}
	}

	private String parseValue(String key, String value) {
		if (StringUtils.indexOfIgnoreCase("file", key) >= 0) {
			return value.replace(">", "/");
		}
		return value;
	}

	public Path getPath() {
		return path;
	}

	public Site getSite() {
		return site;
	}

	public long getNr() {
		return nr;
	}

	public List<String> getExtensionList() {
		return extensionList;
	}

	@Override
	public String toString() {
		return path.toString();
	}

	/**
	 * @return True if the file should been compiled, e.g. compile from textile
	 *         to html
	 */
	public boolean isCompile() {
		return !isHidden() && isVisible() && !isCopyAll();
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
	public String getFileName() {
		if (path.getFileName() == null) {
			LOG.error("[" + path + "], cannot deal with an empty path");
			throw new RuntimeException("cannot deal with an empty path");
		}
		return path.getFileName().toString();
	}

	/**
	 * @return The path of file in the web folder for files that have been
	 *         copied
	 */
	public Path getTargetPath() {
		Path relative = site.getSource().relativize(path);
		return site.getGenerated().resolve(relative);
	}

	public String getTargetURLFilename() {
		String[] paths = Utils.createURLSplit(site.getSource(), this);
		paths[paths.length - 1] = getName();
		String url = Utils.createURL(paths);
		/*
		 * if ( url.indexOf( "/" ) == 0 ) { url = url.substring( 1 ); }
		 */
		return url;
	}

	/**
	 * @return The url of the file that have been compiled
	 */
	public String getTargetURL() {
		String[] paths = Utils.createURLSplit(site.getSource(), this);
		String url = Utils.createURL(paths);
		/*
		 * if ( url.indexOf( "/" ) == 0 ) { url = url.substring( 1 ); }
		 */
		return url;

	}

	public String getTargetURLPath() {
		String[] paths = Utils.createURLSplit(site.getSource(), this);

		String[] tmp = new String[paths.length - 1];
		for (int i = 0; i < tmp.length; i++) {
			tmp[i] = paths[i];
		}
		String url = Utils.createURL(tmp);
		return url;

	}

	/**
	 * @return The path of the file in the web folder for files that have been
	 *         compiled
	 */
	public Path getTargetPath(String url) {
		Path tmp = site.getGenerated().resolve(url);
		return tmp;
	}

	/**
	 * @param extension
	 *            The extension to check
	 * @return True if the exension is present in this filename
	 */
	public boolean containsExtension(String extension) {
		return extensionList != null && extensionList.contains(extension);
	}

	public boolean isDirectory() {
		return Files.isDirectory(path);
	}

	public XPath getParent() {
		if (!Utils.isChild(path.getParent(), site.getSource())) {
			return null;
		}
		return new XPath(site, path.getParent());
	}

	public boolean isVisible() {
		// first check if property "copy" is somewhere
		if(isCopyAllInherited()) {
			return false;
		}
		// second check if property "all" is somewhere
		if(isAllInherited()) {
			return true;
		}
		// third if the parent is "none"
		if(getParent().isNone()) {
			return false;
		}
		// third if no property found we return parse result
		return visible;
	}

	public String getName() {
		return name;
	}

	public String getExtensions() {
		return extensions;
	}

	/**
	 * @return The url of this file, if no url was provided, then the crc32 of
	 *         the filename is used
	 */
	public String getUrl() {
		// non-visible files -> url = filename
		if (!isVisible()) {
			return getFileName();
		}
		if(isAllInherited() && (url == null || url.equals(""))) {
			return getFileName().replace(extensions, "");
		}
		if (url == null || url.equals("")) {
			byte bytes[] = getFileName().getBytes();
			Checksum checksum = new CRC32();

			/*
			 * To compute the CRC32 checksum for byte array, use void
			 * update(bytes[] b, int start, int length) method of CRC32 class.
			 */

			checksum.update(bytes, 0, bytes.length);

			/*
			 * Get the generated checksum using getValue method of CRC32 class.
			 */
			long lngChecksum = checksum.getValue();
			String tmp = new BigInteger(String.valueOf(lngChecksum))
					.toString(Character.MAX_RADIX);
			return tmp;
		} else {
			return url;
		}
	}

	public Map<String, String> getProperties() {
		return properties;
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

	public boolean isPreview() {
		return containsExtension("pre") || containsExtension("p");
	}

	public boolean isFull() {
		return containsExtension("full") || containsExtension("f");
	}

	public boolean isNavigation() {
		return containsExtension("nav") || containsExtension("n");
	}

	public boolean isNone() {
		return containsExtension("none") || containsExtension("x");
	}

	public boolean isHighlight() {
		return hasProperty("highlight") || hasProperty("h");
	}
	
	public boolean isAll(XPath parent) {
		while (parent != null) {
			if (parent.hasProperty("a", "all")) {
				String value = parent.getProperty("a", "all");
				if (value == null
						|| StringUtils.equalsIgnoreCase(value, "true")) {
					return true;
				}
				if (parent.extensionList != null
						&& extensionList.contains("raw")) {
					return true;
				}
			}
			parent = parent.getParent();
		}
		return false;
	}

	public boolean isAllInherited() {
		XPath parent = getParent();
		if (parent != null) {
			return isAll(parent);
		} else {
			return false;
		}
	}

	public Date getDate() {
		return date;
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
		long diff = getNr() - o2.getNr();
		if (diff != 0) {
			return diff > 0 ? 1 : -1;
		}
		if (getName() != null && o2.getName() != null) {
			diff = getName().compareTo(o2.getName());
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
		return getPath().equals(site.getSource());
	}

	public boolean isCopyAllInherited() {
		XPath parent = getParent();
		if (parent != null) {
			return isCopyAll(parent);
		} else {
			return false;
		}
	}

	public boolean isCopyAll() {
		return isCopyAll(this);
	}

	public boolean isCopyAll(XPath parent) {
		while (parent != null) {
			if (parent.hasProperty("c", "copy", "copy-all")) {
				String value = parent.getProperty("c", "copy", "copy-all");
				if (value == null
						|| StringUtils.equalsIgnoreCase(value, "true")) {
					return true;
				}
				if (parent.extensionList != null
						&& extensionList.contains("raw")) {
					return true;
				}
			}
			parent = parent.getParent();
		}
		return false;
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
	public String searchProperty(String...names) {
		XPath current = this;
		do {
			String property = current.getProperty(names);
			if (property != null) {
				return property;
			}
		} while ((current = current.getParent()) != null);
		
		for(String name : names) {
			if(name.equals("sn") || name.equals("size_normal")) {
				return "800x600^";
			}
			if(name.equals("si") || name.equals("size_icon")) {
				return "250x250^c";
			}
		}
		return null;
	}
	
}
