package com.chenhm.blog.engine;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.chenhm.blog.util.TimeAgo;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HandlebarsEngine {

    @Autowired
    MarkdownEngine markdownEngine;

    Handlebars handlebars;

    public HandlebarsEngine() {
        TemplateLoader classPathTemplateLoader = new ClassPathTemplateLoader();
        classPathTemplateLoader.setSuffix(".mustache");
        new FileTemplateLoader("static");

        TemplateLoader loader = classPathTemplateLoader;
        handlebars = new Handlebars(loader);

        handlebars.registerHelper("times", (Helper<Integer>) (context, options) -> {
            StringBuilder ret = new StringBuilder();
            for (int i = 1; i <= context; i++) {
                ret.append(options.fn(i));
            }
            return ret.toString();
        });

        handlebars.registerHelper("timeago", (Helper<String>) (context, options) -> {
            if (StringUtils.isEmpty(context))
                return "";
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            long postTime;
            try {
                postTime = format.parse(context).getTime();
            } catch (ParseException e) {
                log.info(e.getMessage());
                postTime = Instant.now().toEpochMilli();
            }
            return TimeAgo.toDuration(Instant.now().toEpochMilli() - postTime);
        });

        handlebars.registerHelper("md", (Helper<String>) (context, options) -> {
            return markdownEngine.render(options.context.toString());
        });

        handlebars.registerHelper("eq", ConditionalHelpers.eq);
    }

    public String render(String name, Map<String, Object> scopes) throws IOException {
        Template template = handlebars.compile(name);
        return template.apply(scopes);
    }
}
