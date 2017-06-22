/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.xdocc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 *
 * @author draft
 */
public class Cache {
    
    final private Map<String, CacheEntry> cache;
    final private Map<String, Integer> hits = new HashMap<>();
    
    public Cache(Map<String, CacheEntry> cache) {
        this.cache = cache;
    }
    
    public CacheEntry getCached(XPath xPath) {
        return getCached(xPath, null);
    }

    public CacheEntry getCached(XPath xPath, Path generated) {
        String key = xPath.getTargetURL();
        CacheEntry c = cache.get(key);
        if( c == null) {
            return null;
        }
        //check if sources have same modification time
        Map<XPath, Long> map = parentMap(xPath);
        if(!map.equals(c.sourceDirs)) {
            return null;
        }
        
        //check if generated files are there, don't care about the time
        boolean found = false;
        for(Path p:c.generatedFiles()) {
            if(p!=null && !Files.exists(p)) {
                return null;
            }
            if(p.equals(generated)) {
                found = true;
            }
        }
        if(!found && generated != null) {
            return null;
        }
        
        Integer counter = hits.get(key);
        if(counter == null) {
            hits.put(key, 1);
        } else {
            hits.put(key, counter + 1);
        }
        
        return c;
    }
    
    public int hits() {
        int total = 0;
        for (Integer counter:hits.values()) {
            total += counter;
        }
        return total;
    }

    public Cache setCached(XPath xPath, XItem item, Path... generatedFile) {
        String key = xPath.getTargetURL();
        CacheEntry c = cache.get(key);
        List<Path> genFiles = new ArrayList<>(Arrays.asList(generatedFile));
        genFiles.removeIf(Objects::isNull);
        if(c == null) {
            Map<XPath, Long> map = parentMap(xPath);
            c  = new CacheEntry().xItem(item).sourceDirs(map).generatedFiles(genFiles);
            cache.put(key, c);
        } else {
            c.generatedFiles().addAll(genFiles);  
        }
        return this;
    }

    private static Map<XPath, Long> parentMap(XPath xPath) {
        XPath current = xPath;
        Map<XPath, Long> map = new HashMap<>();
        while(current != null) {
            try {
                map.put(current, Files.getLastModifiedTime(current.path()).toMillis());
                current = current.getParent();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return map;
    }
    
    @Accessors(chain = true, fluent = true)
    public static class CacheEntry {
        @Getter @Setter
        private XItem xItem;
        @Getter @Setter
        private Map<XPath, Long> sourceDirs;
        @Getter @Setter
        private List<Path> generatedFiles;
    }
    
}
