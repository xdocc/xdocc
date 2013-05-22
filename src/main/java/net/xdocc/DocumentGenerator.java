package net.xdocc;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.xdocc.Site.TemplateBean;
import freemarker.template.TemplateException;

public class DocumentGenerator implements Serializable {

	private static final Logger LOG = LoggerFactory
			.getLogger(DocumentGenerator.class);
	private static final long serialVersionUID = -8512427831292951263L;
	final private TemplateBean templateText;
	final private Map<String, Object> model;
	final private Site site;

	public DocumentGenerator(Site site, TemplateBean templateText,
			Map<String, Object> model) {
		this.site = site;
		this.templateText = templateText;
		this.model = model;
	}

	public String generate() {
		try {
			return Utils.applyTemplate(site, templateText, model);
		} catch (TemplateException | IOException e) {
			if (LOG.isWarnEnabled()) {
				LOG.warn("cannot generate document "
						+ templateText.getFile().getFileName() + ". Model is "
						+ model, e);
			}
			return null;
		}
	}

	public TemplateBean getTemplateText() {
		return templateText;
	}

	public Map<String, Object> getModel() {
		return model;
	}
}
