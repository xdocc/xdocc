/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.xdocc;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private int hits = 0;
    
    public Cache(Map<String, CacheEntry> cache) {
        this.cache = cache;
    }

    private static final Logger LOG = LoggerFactory.getLogger(Cache.class);
    
    public CacheEntry getCached(Site site, XPath xPath) {
        return getCached(site, xPath, null);
    }

    public CacheEntry getCached(Site site, XPath xPath, Path generated) {
        String key = xPath.getTargetURL();
        CacheEntry c = cache.get(key);
        if( c == null) {
            return null;
        }
        //check if sources have same modification time
        for(Map.Entry<String, Long> entry: c.sourceDirs.entrySet()) {
            try {
                Path p = Paths.get(entry.getKey());
                if(!Files.exists(p) ||
                        Files.getLastModifiedTime(p).toMillis() != entry.getValue()) {
                    return null;
                }
            } catch (IOException e) {
                LOG.error("caching exception",e);
                return null;
            }
        }

        
        //check if generated files are there, don't care about the time
        boolean found = false;
        for(String gen:c.generatedFiles()) {
            Path p = Paths.get(gen);
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
        if(c.xItem() !=null) {
            c.xItem().init(site);
        }
        hits++;
        return c;
    }
    
    public int hits() {
        return hits;
    }

    public Cache setCached(Site site, XPath xPath, Path sourceFile, XItem item, Path... generatedFile) {
        String key = xPath.getTargetURL();
        CacheEntry c = cache.get(key);

        List<String> genFiles = new ArrayList<>();
        for(Path p:generatedFile) {
            if(p!=null) {
                genFiles.add(p.toString());
            }
        }

        if(c == null) {
            Map<String, Long> map = parentMap(site, Paths.get(xPath.path()));
            c  = new CacheEntry().xItem(item).sourceDirs(map).generatedFiles(genFiles);
            cache.put(key, c);
        } else {
            c.generatedFiles().addAll(genFiles);
        }
        if(sourceFile != null) {
            try {
                c.sourceDirs.put(sourceFile.toString(), Files.getLastModifiedTime(sourceFile).toMillis());
            } catch (IOException e) {
                LOG.error("cannot cache", e);
            }
        }
        return this;
    }

    private static Map<String, Long> parentMap(Site site, Path xPath) {


        Path current = xPath;
        Map<String, Long> map = new HashMap<>();
        for(Path p:Utils.listPathsSrc(site, xPath)) {
            try {
                map.put(p.toString(), Files.getLastModifiedTime(p).toMillis());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return map;
    }
    
    @Accessors(chain = true, fluent = true)
    public static class CacheEntry implements Serializable {
        @Getter @Setter
        private XItem xItem;
        @Getter @Setter
        private Map<String, Long> sourceDirs;
        @Getter @Setter
        private List<String> generatedFiles;
    }
    
}
