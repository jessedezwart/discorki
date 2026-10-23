package com.alistats.discorki.service;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;

import org.springframework.stereotype.Component;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;

@Component
public class TemplatingService {
    final PebbleEngine engine = new PebbleEngine.Builder()
            .autoEscaping(false)
            .build();

    public String renderTemplate(String templatePath, HashMap<String, Object> context) throws IOException {
        PebbleTemplate compiledTemplate = engine.getTemplate(templatePath);

        Writer writer = new StringWriter();

        compiledTemplate.evaluate(writer, context);

        return writer.toString();
    }

    public String getNotificationTemplate(String name, HashMap<String, Object> context) throws IOException {
        return renderTemplate("templates/notifications/" + name + ".md.pebble", context);
    }
}
