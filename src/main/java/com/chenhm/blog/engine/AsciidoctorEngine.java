package com.chenhm.blog.engine;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.chenhm.blog.runner.BlogProperties;
import com.google.common.collect.ImmutableMap;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AsciidoctorEngine {
    private static final String OPT_REQUIRES = "requires";

    private Asciidoctor engine;

    @Autowired
    BlogProperties properties;

    @PostConstruct
    public void init() {
        if (engine == null) {
            log.info("Initializing Asciidoctor engine...");
            engine = Asciidoctor.Factory.create();
            if (!StringUtils.isEmpty(properties.getAsciidoctor().getRequests())) {
                String[] requires = properties.getAsciidoctor().getRequests().split(",");
                engine.requireLibrary(requires);
            }
            log.info("Asciidoctor engine initialized.");
        }
    }

    public String render(String adoc){
        Options options = new Options();
        Map attributes = ImmutableMap.builder()
                .put("showtitle","true")
                .put("toc","right")
                .put("backend","xhtml5")
                .put("source-highlighter","prismjs")
                .put("imagesdir", properties.getAsciidoctor().getImagesdir())
                .build();
        options.setAttributes(attributes);
        return engine.convert(adoc,options);
    }
}
