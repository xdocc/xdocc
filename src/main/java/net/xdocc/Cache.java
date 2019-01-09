/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.xdocc;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public boolean isCached(XPath xPath) {
        return isCached(xPath, null);
    }

    private boolean isCached(CacheEntry c, Path generated) {
        if( c == null) {
            return false;
        }

        for(Map.Entry<String, Long> entry: c.sourceDirs.entrySet()) {
            try {
                Path p = Paths.get(entry.getKey());
                if(!Files.exists(p)) {
                    LOG.debug("file {} does not exist", p);
                    return false;
                }
                //don't check directories for timestamp as they change if the content of the file changes
                if(Files.isRegularFile(p)) {
                    if (Files.getLastModifiedTime(p).toMillis() != entry.getValue()) {
                        LOG.debug("time of file {} is {}, stored is {}", p, Files.getLastModifiedTime(p).toMillis(), entry.getValue());
                        return false;
                    }
                } else {

                }

            } catch (IOException e) {
                LOG.error("caching exception",e);
                return false;
            }
        }

        boolean found = false;
        for(String gen:c.generatedFiles()) {
            Path p = Paths.get(gen);
            if(p!=null && !Files.exists(p)) {
                return false;
            }
            if(p.equals(generated)) {
                found = true;
            }
        }
        if(!found && generated != null) {
            return false;
        }
        return true;
    }

    public boolean isCached(XPath xPath, Path generated) {
        String key = xPath.path();
        CacheEntry c = cache.get(key);
        return isCached(c, generated);
    }

    public CacheEntry getCached(Site site, XPath xPath, Path generated) {
        String key = xPath.path();
        CacheEntry c = cache.get(key);

        if(!isCached(c,generated)) {
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

    public Cache setCached(Site site, XPath xPath, Path sourceFile, XItem item,  Path genFile) {
        List<Path> sourceFiles = new ArrayList<>(1);
        if(sourceFile != null) {
            sourceFiles.add(sourceFile);
        }
        List<String> genFiles = new ArrayList<>(1);
        genFiles.add(genFile.toString());
        return setCached(site,xPath, sourceFiles, item, genFiles);
    }
    
    public Cache setCached(Site site, XPath xPath, List<Path> sourceFiles, XItem item,  Path genFile) {
        List<String> genFiles = new ArrayList<>(1);
        genFiles.add(genFile.toString());
        return setCached(site,xPath, sourceFiles, item, genFiles);
    }

    public Cache setCached(Site site, XPath xPath, List<Path> sourceFiles, XItem item,  List<String> genFiles) {
        String key = xPath.path();
        CacheEntry c = cache.get(key);

        if(c == null) {
            Map<String, Long> map = parentMap(site, Paths.get(xPath.path()));
            c  = new CacheEntry().xItem(item).sourceDirs(map).generatedFiles(genFiles);
            cache.put(key, c);
        } else {
            //sanity check
            //if(item != null && !item.equals(c.xItem())) {
            //    throw new RuntimeException("tried to cache different things! "+item+"//"+c.xItem());
            //}
            c.generatedFiles().addAll(genFiles);
        }
        if(sourceFiles != null) {
                for(Path sourceFile : sourceFiles) {
                    try {
                        c.sourceDirs().put(sourceFile.toString(), Files.getLastModifiedTime(sourceFile).toMillis());
                    } catch (IOException e) {
                        LOG.error("cannot cache", e);
                    }
                }
        }
        //put in all templates as sources
        for(Path sourceFile : site.templates()) {
            try {
            	if(Files.exists(sourceFile)) {
            		c.sourceDirs().put(sourceFile.toString(), Files.getLastModifiedTime(sourceFile).toMillis());
            	}
            } catch (IOException e) {
                LOG.error("cannot cache", e);
            }
        }
        //put in all the global navigation sources
        for(Link link: site.globalNavigation().flat()) {
        	try {
        		Path sourceFile = Paths.get(link.getTarget().path());
        		if(Files.exists(sourceFile)) {
                    //LOG.debug("in source set: key={}", sourceFile);
        			c.sourceDirs().put(sourceFile.toString(), Files.getLastModifiedTime(sourceFile).toMillis());
        		}
            } catch (IOException e) {
                LOG.error("cannot cache", e);
            }
        }
        /*if(LOG.isDebugEnabled()) {
            for(Map.Entry<String, Long> m:c.sourceDirs.entrySet()) {
                LOG.debug("in source set: key={}, value={}", m.getKey(), m.getValue());
            }
        }*/
        
        cache.put(key, c); //mapdb specific, values are immutable
        return this;
    }

    private static Map<String, Long> parentMap(Site site, Path xPath) {
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
        private static final long serialVersionUID = -5976223970753740658L;
		@Getter @Setter
        private XItem xItem;
        @Getter @Setter
        private Map<String, Long> sourceDirs;
        @Getter @Setter
        private List<String> generatedFiles;
    }
    
}
