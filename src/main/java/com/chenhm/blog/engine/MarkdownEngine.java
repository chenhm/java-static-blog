package com.chenhm.blog.engine;

import static com.vladsch.flexmark.parser.PegdownExtensions.ANCHORLINKS;
import static com.vladsch.flexmark.parser.PegdownExtensions.GITHUB_DOCUMENT_COMPATIBLE;
import static com.vladsch.flexmark.parser.PegdownExtensions.TOC;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.profiles.pegdown.PegdownOptionsAdapter;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.DataHolder;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MarkdownEngine {
    static final DataHolder OPTIONS = PegdownOptionsAdapter.flexmarkOptions(TOC
                    | GITHUB_DOCUMENT_COMPATIBLE
                    & (~ANCHORLINKS)
            , YamlFrontMatterExtension.create()
            , HeadingsAttributeProvider.HeadingsExtension.create())
            .toMutable()
            .set(HtmlRenderer.GENERATE_HEADER_ID, true)
            .set(TocExtension.TITLE, "Table of Contents")
            .set(TocExtension.LEVELS, 4 | 8 | 16)
            .set(TocExtension.LIST_CLASS, "toc-list")
            .set(TocExtension.DIV_CLASS, "toc md");

    static final Parser PARSER = Parser.builder(OPTIONS).build();
    static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();

    public Markdown render(String md) {
        AbstractYamlFrontMatterVisitor visitor = new AbstractYamlFrontMatterVisitor();
        Document document = PARSER.parse(md);
        visitor.visit(document);
        Markdown markdown = new Markdown();
        markdown.setHtml(RENDERER.render(document));
        visitor.getData().replaceAll((s, strings) ->
                strings.stream().map(str -> str.replaceAll("^\"|\"$", "")).collect(Collectors.toList())
        );
        markdown.setMate(visitor.getData());
        return markdown;
    }

    @Data
    public static class Markdown {
        String html;
        Map<String, List<String>> mate;
    }
}
