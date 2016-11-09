package net.xdocc;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.xdocc.Site.TemplateBean;
import freemarker.template.TemplateException;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(chain = true, fluent = true)
public class DocumentGenerator implements Serializable {

	private static final Logger LOG = LoggerFactory
			.getLogger(DocumentGenerator.class);
	private static final long serialVersionUID = -8512427831292951263L;
        
        @Getter
	final private TemplateBean templateBean;
        
        @Getter
        final private Map<String, Object> model;
	final private Site site;

	public DocumentGenerator(Site site, TemplateBean templateBean) {
		this.site = site;
		this.templateBean = templateBean;
		this.model = new HashMap<String, Object>();
	}
        
        public DocumentGenerator(Site site, TemplateBean templateBean, Map<String, Object> model) {
		this(site, templateBean);
                this.model.putAll(model);
	}

	public String generate() {
		try {
			return Utils.applyTemplate(site, templateBean, model);
		} catch (TemplateException | IOException e) {
				LOG.warn("cannot generate document {}. Model is {}", 
                                        templateBean.file().getFileName(), model, e);
			return null;
		}
	}	

	public DocumentGenerator copy() {
		DocumentGenerator documentGenerator = new DocumentGenerator(site, templateBean, model);
		return documentGenerator;
	}

}
