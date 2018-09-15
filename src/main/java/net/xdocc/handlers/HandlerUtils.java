package net.xdocc.handlers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.xdocc.Cache;
import net.xdocc.XItem;
import net.xdocc.XPath;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import org.apache.commons.lang3.StringUtils;

public class HandlerUtils {
    
    final private static Object lock = new Object();

    public static boolean knowsExtension(List<String> knownExtensions,
            XPath xPath) {
        for (String extension : knownExtensions) {
            if (xPath.containsExtension(extension)) {
                return true;
            }
        }
        return false;
    }

    public static Charset detectCharset(Path path) throws IOException {
        Charset charset = detectCharset0(path);
        if (charset == null) {
            // set this as a default
            charset = Charset.forName("UTF-8");
        }
        return charset;
    }

    private static Charset detectCharset0(Path path) throws IOException {
        // the detection library is not threadsafe
        synchronized (lock) {
            try (@SuppressWarnings("resource") BufferedInputStream bis = new BufferedInputStream(
                    Files.newInputStream(path))) {
                CharsetDetector cd = new CharsetDetector();
                cd.setText(bis);
                CharsetMatch[] cm = cd.detectAll();
                if (cm != null) {
                    for (int i = 0; i < cm.length; i++) {
                        //preference goes always to UTF-8
                        if ("UTF-8".equals(cm[i].getName())) {
                            return Charset.forName(cm[i].getName());
                        }
                    }
                    if (cm.length > 0) {
                        return Charset.forName(cm[0].getName());
                    }
                }
                return null;
            }
        }
    }

    public static Map<String, Object> fillModel(String documentName,
            String documentURL, Date documentDate, long documentNr,
            String documentFilename, String htmlContent, Map<String, Object> model) {
        model.put(XPath.NAME, documentName);
        model.put(XPath.URL, documentURL);
        model.put(XItem.CONTENT, htmlContent);
        model.put(XPath.DATE, documentDate);
        model.put(XPath.NR, documentNr);
        model.put(XPath.FILENAME, documentFilename);
        return model;
    }

    public static boolean childCached(Cache cache, XItem xItem) {
        if(!cache.isCached(xItem.xPath())) {
            return false;
        }
        for(XItem item:xItem.getItems()) {
            if(!childCached(cache, item)) {
                return false;
            }
        }
        return true;
    }

    public static String readFile(Path path, Charset charset) throws IOException {
        byte[] encoded = Files.readAllBytes(path);
        String tmp = new String(encoded, charset);
        if(tmp.startsWith("---")) {
            int stop = tmp.indexOf("\n---", 3);
            if(stop <= 0) {
                //no frontmatter
                return tmp;
            }
            return StringUtils.stripStart(tmp.substring(stop+4), null);

        } else {
            //no frontmatter
            return tmp;
        }
    }

    public static String readFile(String path) throws IOException {
        return readFile(Paths.get(path), Charset.defaultCharset());
    }

    public static List<String> readAllLines(Path path, Charset charset) throws IOException {
        List<String> lines = Files.readAllLines(path, charset);
        if(lines.isEmpty()) {
           return lines;
        }
        if(lines.get(0).startsWith("---")) {
            for(int i=1;i<lines.size();i++) {
                if(lines.get(i).startsWith("---")) {
                    return lines.subList(i, lines.size());
                }
            }
            //no frontmatter
            return lines;
        } else {
            //no frontmatter
            return lines;
        }
    }
}
