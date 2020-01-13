package com.chenhm.blog.runner;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@Data
@ConfigurationProperties
public class BlogProperties {

    Asciidoctor asciidoctor;

    App app;

    @Data
    public static class Asciidoctor{
        private String requests;
        private String imagesdir;
    }

    @Data
    public static class App{
        private String postPath;
        private String dist;
        private int pageSize;
        private String title;
        private String ignore;
    }

}
