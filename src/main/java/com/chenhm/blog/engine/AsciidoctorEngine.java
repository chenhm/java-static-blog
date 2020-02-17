package com.chenhm.blog.engine;

import static org.asciidoctor.AttributesBuilder.attributes;
import static org.asciidoctor.OptionsBuilder.options;
import static org.asciidoctor.Placement.RIGHT;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.chenhm.blog.runner.BlogProperties;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AsciidoctorEngine {

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
            engine.javaExtensionRegistry().block(new PlantumlProcessor(properties.getAsciidoctor().isPlantumlAsImg()));
            log.info("Asciidoctor engine initialized.");
        }
    }

    public String render(String adoc) {
        Options options = new Options();
        options.setAttributes(attributes().tableOfContents(RIGHT).backend("xhtml5").sourceHighlighter("prismjs")
                .showTitle(true).imagesDir(properties.getAsciidoctor().getImagesdir()).get());
        return engine.convert(adoc, options);
    }

    /**
     * Asciidoctor need custom font and theme to render CJK characters, please refer asciidoctorj command parameters, e.g:
     * ```
     * asciidoctorj -b pdf -a pdf-theme=basic-theme.yml -a pdf-fontsdir="uri:classloader:/path/to/fonts;GEM_FONTS_DIR" document.adoc
     * ```
     * Recommended using this project: https://github.com/chloerei/asciidoctor-pdf-cjk-kai_gen_gothic
     *
     * @param adoc
     * @param out
     */
    public void renderPDF(String adoc, Path out) {
        Options options = options().backend("pdf").toFile(out.toFile()).attributes(attributes()
                .showTitle(true)
                .docType("book")
                .sourceHighlighter("rouge")
                .imagesDir(Paths.get(properties.getApp().getDist()).resolve(properties.getApp().getPostPath()).toString())
                .attribute("pdf-fontsdir", properties.getAsciidoctor().getPdfFontsdir())
                .attribute("pdf-stylesdir", properties.getAsciidoctor().getPdfStylesdir())
                .attribute("pdf-theme", properties.getAsciidoctor().getPdfTheme())
                .get()).get();
        engine.convert(adoc, options);
    }
}
