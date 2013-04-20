package net.xdocc;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import net.xdocc.Site.TemplateBean;
import freemarker.template.TemplateException;

public class DocumentGenerator implements Serializable {

	private static final long serialVersionUID = -8512427831292951263L;
	final private TemplateBean templateText;
	final private Map<String, Object> model;
	final private Site site;

	public DocumentGenerator(Site site, TemplateBean templateText, Map<String, Object> model) {
		this.site = site;
		this.templateText = templateText;
		this.model = model;
	}

	public String generate() throws TemplateException, IOException {
		try {
		return Utils.applyTemplate(site, templateText, model);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public TemplateBean getTemplateText() {
		return templateText;
	}

	public Map<String, Object> getModel() {
		return model;
	}

	public DocumentGenerator copy() {
		return new DocumentGenerator(site, templateText, new HashMap<>(model));
	}
}
