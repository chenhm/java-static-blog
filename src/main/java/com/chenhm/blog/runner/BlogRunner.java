package com.chenhm.blog.runner;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.regex.Pattern.MULTILINE;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

import com.chenhm.blog.Args;
import com.chenhm.blog.engine.AsciidoctorEngine;
import com.chenhm.blog.engine.MarkdownEngine;
import com.chenhm.blog.engine.HandlebarsEngine;
import com.chenhm.blog.util.FileUtils;
import com.chenhm.blog.util.Maps;

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

        Path source = Paths.get(args.getSource()).resolve(properties.getApp().getPostPath());
        Path postDist = Paths.get(properties.getApp().getDist()).resolve(properties.getApp().getPostPath());
        Path listPath = Paths.get(properties.getApp().getDist()).resolve("list");

        Files.createDirectories(postDist);
        Files.createDirectories(listPath);

        Map<String, Map<String, String>> files = new ConcurrentHashMap<>();
        Files.list(source).parallel().forEach(p -> renderPost(postDist, files, p));

        renderList(listPath, files);
        log.info("Blog generated");

        if (args.isDev()) {
            watchFiles(source, postDist, listPath, files);
        }
    }

    private void renderList(Path listPath, Map<String, Map<String, String>> files) {
        List<Map> names = files.values().stream().sorted((o1, o2) -> -o1.get("date").compareTo(o2.get("date"))).collect(Collectors.toList());
        for (int page = 0, pageSize = properties.getApp().getPageSize(); page * pageSize < names.size(); page++) {
            List<Map> list = names.stream().skip(page * pageSize).limit(pageSize).collect(Collectors.toList());
            int currentPage = page + 1;
            int totalPage = ceil(names.size(), pageSize);
            try (FileWriter fw = new FileWriter(listPath.resolve(currentPage + ".html").toFile())) {
                Map scope = Maps.builder()
                        .put("list", list)
                        .put("title", properties.getApp().getTitle())
                        .put("postTitle", properties.getApp().getTitle())
                        .put("postPath", properties.getApp().getPostPath())
                        .put("currentPage", currentPage)
                        .put("previousPage", currentPage > 1 ? currentPage - 1 : 1)
                        .put("nextPage", currentPage < totalPage ? currentPage + 1 : totalPage)
                        .put("totalPage", totalPage)
                        .put("total", names.size())
                        .put("thisYear", getThisYear())
                        .put("gaId", properties.getApp().getGoogleTrackingId())
                        .build();
                fw.write(handlebarsEngine.render("list", scope));
                fw.flush();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void renderPost(Path postDist, Map<String, Map<String, String>> files, Path p) {
        if (p.toFile().isDirectory()) {
            try {
                FileSystemUtils.copyRecursively(p, postDist.resolve(p.getFileName()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return;
        }

        if (!p.toFile().isFile())
            return;
        String fileName = p.getFileName().toString();
        boolean isMD = fileName.endsWith(".md");
        boolean isAdoc = fileName.endsWith(".adoc") || fileName.endsWith(".asciidoc");
        String id = onlyId(fileName);

        try (FileWriter fw = new FileWriter(postDist.resolve(id + ".html").toFile())) {
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
            } else if (isAdoc) {
                postTitle = getTitle(post);
                html = asciidoctorEngine.render(post);
                if(properties.getAsciidoctor().isPdfRender()){
                    asciidoctorEngine.renderPDF(post, postDist.resolve(id + ".pdf"));
                }
            } else {
                log.info("can't process file: " + fileName);
                return;
            }
            files.put(id, Maps.<String, String>builder()
                    .put("id", id)
                    .put("date", onlyDate(fileName))
                    .put("title", postTitle)
                    .build());

            Map scope = Maps.builder().put("html", html)
                    .put("thisYear", getThisYear())
                    .put("title", properties.getApp().getTitle())
                    .put("fmTitle", fmTitle)
                    .put("postTitle", StringUtils.isEmpty(fmTitle) ? postTitle : fmTitle)
                    .put("gaId", properties.getApp().getGoogleTrackingId())
                    .build();
            fw.write(handlebarsEngine.render("post", scope));
            fw.flush();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

    }

    void watchFiles(final Path source, Path dist, Path list, Map<String, Map<String, String>> files) {
        log.info("Watching: " + source);
        try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
            final WatchKey watchKey = source.register(watchService, ENTRY_MODIFY, ENTRY_DELETE);
            while (true) {
                final WatchKey wk = watchService.take();
                for (WatchEvent<?> event : wk.pollEvents()) {
                    final Path changed = (Path) event.context();
                    WatchEvent.Kind kind = event.kind();
                    log.info(kind.name() + ": " + changed);
                    if (kind == ENTRY_DELETE) {
                        files.remove(onlyId(changed.getFileName().toString()));
                    } else {
                        renderPost(dist, files, source.resolve(changed));
                    }
                    renderList(list, files);
                }

                // reset the key
                boolean valid = wk.reset();
                if (!valid) {
                    log.warn("Key has been unregistered");
                }
            }
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    void copyStatic(String path) {
        try {
            Path dist = Paths.get(properties.getApp().getDist());
            Path staticPath = dist.resolve("static");
            Files.createDirectories(staticPath);

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:/static/*");
            for (Resource r : resources) {
                log.info("Copy file:" + r.getDescription());
                FileCopyUtils.copy(r.getInputStream(), Files.newOutputStream(staticPath.resolve(r.getFilename())));
            }
            FileUtils.copyRecursively(Paths.get(path), Paths.get(properties.getApp().getDist()),
                    s -> properties.getApp().getPostPath().equals(s)
                            || properties.getApp().getDist().equals(s)
                            || Arrays.asList(properties.getApp().getCopyIgnore().split(";")).stream().anyMatch(regex -> s.matches(regex)));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
