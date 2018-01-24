package net.xdocc;

import freemarker.template.Template;
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
import java.nio.file.attribute.FileTime;

@Accessors(chain = true, fluent = true)
public class TemplateBean implements Serializable {
	
	private static final long serialVersionUID = 2193521832344660637L;
	private static final Logger LOG = LoggerFactory.getLogger(TemplateBean.class);

    @Getter @Setter
    private long timestamp;
    @Getter @Setter
    private long filesize;
    @Getter @Setter
    private String file;
    @Getter @Setter
    private boolean internal;

    final private Site site;

    public TemplateBean(Site site) {
        this.site = site;
    }

    public boolean isDirty() {
        if(internal) {
            //its loaded from memory, so never refresh
            return false;
        }
        try {
            final FileTime fileTime = Files.getLastModifiedTime(Paths.get(this.file));
            final long filesize = Files.size(Paths.get(this.file));
            boolean dirty = this.timestamp != fileTime.toMillis()
                    || this.filesize != filesize;
            return dirty;
        } catch (IOException e) {
            LOG.info("file removed: {}", file);
            return true;
        }
    }

    public Template template() {
        final Template template;
        synchronized (Utils.lock) {
            try {
                Path p = Paths.get(file);
                template = site.freemakerEngine().getTemplate(p.getFileName().toString());
                return template;
            } catch (Throwable e) {
                e.printStackTrace();
                Path p = Paths.get(file);
                try {
                    site.freemakerEngine().getTemplate(p.getFileName().toString());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }
        }
        return null;
    }

}