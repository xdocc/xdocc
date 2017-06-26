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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(Cache.class);
    
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
        for(Map.Entry<Path, Long> entry: c.sourceDirs.entrySet()) {
            try {
                if(!Files.exists(entry.getKey()) ||
                        Files.getLastModifiedTime(entry.getKey()).toMillis() != entry.getValue()) {
                    return null;
                }
            } catch (IOException e) {
                LOG.error("caching exception",e);
                return null;
            }
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

    public Cache setCached(Site site, XPath xPath, Path sourceFile, XItem item, Path... generatedFile) {
        String key = xPath.getTargetURL();
        CacheEntry c = cache.get(key);

        List<Path> genFiles = new ArrayList<>(Arrays.asList(generatedFile));
        genFiles.removeIf(Objects::isNull);

        if(c == null) {
            Map<Path, Long> map = parentMap(site, xPath.path());
            c  = new CacheEntry().xItem(item).sourceDirs(map).generatedFiles(genFiles);
            cache.put(key, c);
        } else {
            c.generatedFiles().addAll(genFiles);
        }
        if(sourceFile != null) {
            try {
                c.sourceDirs.put(sourceFile, Files.getLastModifiedTime(sourceFile).toMillis());
            } catch (IOException e) {
                LOG.error("cannot cache", e);
            }
        }
        return this;
    }

    private static Map<Path, Long> parentMap(Site site, Path xPath) {


        Path current = xPath;
        Map<Path, Long> map = new HashMap<>();
        for(Path p:Utils.listPathsSrc(site, xPath)) {
            try {
                map.put(p, Files.getLastModifiedTime(p).toMillis());
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
        private Map<Path, Long> sourceDirs;
        @Getter @Setter
        private List<Path> generatedFiles;
    }
    
}
