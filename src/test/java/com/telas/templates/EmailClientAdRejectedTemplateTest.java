package com.telas.templates;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailClientAdRejectedTemplateTest {

    @Test
    void processesWithEmptyOptionalFields() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "templates");
        cfg.setDefaultEncoding("UTF-8");
        Template template = cfg.getTemplate("email_client_ad_rejected.ftlh");
        Map<String, Object> model = new HashMap<>();
        model.put("clientName", "ACME");
        model.put("adName", "banner.png");
        model.put("link", "https://example.com/messages");
        StringWriter out = new StringWriter();
        template.process(model, out);
        assertThat(out.toString()).contains("Customer rejected an ad");
    }

    @Test
    void processesWithDescriptionOnly() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "templates");
        cfg.setDefaultEncoding("UTF-8");
        Template template = cfg.getTemplate("email_client_ad_rejected.ftlh");
        Map<String, Object> model = new HashMap<>();
        model.put("clientName", "ACME");
        model.put("adName", "banner.png");
        model.put("description", "Line one\nLine two");
        model.put("link", "");
        StringWriter out = new StringWriter();
        template.process(model, out);
        assertThat(out.toString()).contains("Details:");
        assertThat(out.toString()).contains("Line one");
    }

    @Test
    void processesWithJustificationOnly() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "templates");
        cfg.setDefaultEncoding("UTF-8");
        Template template = cfg.getTemplate("email_client_ad_rejected.ftlh");
        Map<String, Object> model = new HashMap<>();
        model.put("clientName", "ACME");
        model.put("adName", "banner.png");
        model.put("justification", "Not on brand");
        StringWriter out = new StringWriter();
        template.process(model, out);
        assertThat(out.toString()).contains("Summary:");
        assertThat(out.toString()).contains("Not on brand");
    }
}
