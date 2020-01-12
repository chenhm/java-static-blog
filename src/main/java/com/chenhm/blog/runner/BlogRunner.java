package com.chenhm.blog.runner;

import static com.google.common.io.RecursiveDeleteOption.ALLOW_INSECURE;
import static java.util.regex.Pattern.MULTILINE;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

import com.chenhm.blog.Args;
import com.chenhm.blog.engine.AsciidoctorEngine;
import com.chenhm.blog.engine.MarkdownEngine;
import com.chenhm.blog.engine.HandlebarsEngine;
import com.chenhm.blog.util.FileUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.MoreFiles;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BlogRunner {

    @Autowired
    BlogProperties properties;

    @Autowired
    AsciidoctorEngine asciidoctorEngine;

    @Autowired
    HandlebarsEngine handlebarsEngine;

    @Autowired
    MarkdownEngine markdownEngine;

    String onlyDate(String fileName) {
        Matcher matcher = Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}").matcher(fileName);
        if (matcher.find())
            return matcher.group();
        return "";
    }

    String onlyId(String fileName) {
        return fileName.replaceAll("\\.(md|adoc)$", "");
    }

    int ceil(int a, int b) {
        return a / b + ((a % b == 0) ? 0 : 1);
    }

    String getTitle(String text) {
        Matcher matcher = Pattern.compile("^#([^#].*)", MULTILINE).matcher(text);
        if (matcher.find())
            return matcher.group(1).trim();
        return "";
    }

    String getThisYear() {
        return String.valueOf(new GregorianCalendar().get(Calendar.YEAR));
    }

    public void run(Args args) throws Exception {

        copyStatic(args.getSource());

        String source = args.getSource() + "/" + properties.getApp().getPostPath();
        String dist = properties.getApp().getDist() + File.separator + properties.getApp().getPostPath();
        String listPath = properties.getApp().getDist() + File.separator + "list";
        File file = new File(dist);
        if (!file.exists())
            file.mkdirs();

        file = new File(listPath);
        if (!file.exists())
            file.mkdirs();

        Queue<Map<String, String>> files = new ConcurrentLinkedQueue<>();
        Files.list(Paths.get(source)).parallel().forEach(p -> {
            if (p.toFile().isDirectory()) {
                try {
                    FileSystemUtils.copyRecursively(p, Paths.get(dist).resolve(p.getFileName()));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return;
            }

            if (!p.toFile().isFile())
                return;
            String fileName = p.getFileName().toString();
            boolean isMD = fileName.endsWith(".md");
            String id = onlyId(fileName);

            try (FileWriter fw = new FileWriter(dist + File.separator + id + ".html")) {
                String post = new String(Files.readAllBytes(p));
                String postTitle;
                String fmTitle = "";
                String html;
                if (isMD) {
                    MarkdownEngine.Markdown md = markdownEngine.render(post);
                    if (md.getMate().get("title") != null && md.getMate().get("title").size() > 0) {
                        fmTitle = md.getMate().get("title").get(0);
                        postTitle = fmTitle;
                    } else {
                        postTitle = getTitle(post);
                    }
                    html = md.getHtml();
                } else {
                    postTitle = getTitle(post);
                    html = asciidoctorEngine.render(post);
                }
                files.add(ImmutableMap.<String, String>builder()
                        .put("id", id)
                        .put("date", onlyDate(fileName))
                        .put("title", postTitle)
                        .build());

                Map scope = ImmutableMap.builder().put("html", html)
                        .put("thisYear", getThisYear())
                        .put("title", properties.getApp().getTitle())
                        .put("fmTitle", fmTitle)
                        .build();
                fw.write(handlebarsEngine.render("post", scope));
                fw.flush();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        });

        List<Map> names = files.stream().sorted((o1, o2) -> -o1.get("date").compareTo(o2.get("date"))).collect(Collectors.toList());
        for (int page = 0, pageSize = properties.getApp().getPageSize(); page * pageSize < names.size(); page++) {
            List<Map> list = names.stream().skip(page * pageSize).limit(pageSize).collect(Collectors.toList());
            int currentPage = page + 1;
            int totalPage = ceil(names.size(), pageSize);
            try (FileWriter fw = new FileWriter(listPath + File.separator + currentPage + ".html")) {
                Map scope = ImmutableMap.builder()
                        .put("list", list)
                        .put("title", properties.getApp().getTitle())
                        .put("postPath", properties.getApp().getPostPath())
                        .put("currentPage", currentPage)
                        .put("previousPage", currentPage > 1 ? currentPage - 1 : 1)
                        .put("nextPage", currentPage < totalPage ? currentPage + 1 : totalPage)
                        .put("totalPage", totalPage)
                        .put("total", names.size())
                        .put("thisYear", getThisYear())
                        .build();
                fw.write(handlebarsEngine.render("list", scope));
                fw.flush();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }

        log.info("Blog generated");
    }

    void copyStatic(String path) {
        try {
            Path dist = Paths.get(properties.getApp().getDist());
            Path staticPath = dist.resolve("static");
            Files.createDirectories(staticPath);

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:/static/*");
            for (Resource r : resources) {
                System.out.println("copy file:" + r.getDescription());
                FileCopyUtils.copy(r.getInputStream(), Files.newOutputStream(staticPath.resolve(r.getFilename())));
            }
            FileUtils.copyRecursively(Paths.get(path), Paths.get(properties.getApp().getDist()),
                    s -> properties.getApp().getPostPath().equals(s)
                            || properties.getApp().getDist().equals(s)
                            || Lists.newArrayList(properties.getApp().getIgnore().split(";")).stream().anyMatch(regex -> s.matches(regex)));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
