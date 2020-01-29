package com.chenhm.blog.engine;

import static com.chenhm.blog.engine.PlantumlProcessor.TAG;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Contexts;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.Reader;
import org.springframework.util.Base64Utils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.HtmlUtils;

import com.chenhm.blog.util.Maps;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Contexts({Contexts.LITERAL, Contexts.LISTING, Contexts.OPEN})
@ContentModel(ContentModel.RAW)
@Name(TAG)
@Slf4j
public class PlantumlProcessor extends BlockProcessor {
    public static final String TAG = "plantuml";
    private boolean plantumlAsImg = true;

    public PlantumlProcessor(boolean plantumlAsImg) {
        this.plantumlAsImg = plantumlAsImg;
    }

    ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("{}:{}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        String url = "https://kroki.io/plantuml/svg/";
        String uml = reader.read();
        String title = (String) attributes.get("title");

        ClientResponse response = WebClient.builder().filters(filter -> filter.add(logRequest())).baseUrl(url).build()
                .get().uri(compress(uml)).exchange().doOnError(throwable -> throwable.printStackTrace()).block();

        if (plantumlAsImg) {
            String svg = response.bodyToMono(byte[].class).defaultIfEmpty("".getBytes()).map(Base64Utils::encodeToString).block();
            Block block = createBlock(parent, "image", "",
                    Maps.<String, Object>builder().put("target", "data:image/svg+xml;base64," + svg).build());
            block.setCaption("Figure " + parent.getDocument().getAndIncrementCounter("figure") + ". ");
            block.setTitle(title);
            return block;
        } else {
            String svg = response.bodyToMono(String.class).defaultIfEmpty("").map(s -> String.format("<div class=\"imageblock\">\n" +
                    "<div class=\"content\">%s</div>\n" + (
                    StringUtils.isEmpty(title) ? "" : String.format("<div class=\"title\">%s. %s</div>\n",
                            "Figure " + parent.getDocument().getAndIncrementCounter("figure"),
                            HtmlUtils.htmlEscape(title))) +
                    "</div>", s)).block();
            return createBlock(parent, "pass", Arrays.asList(svg));
        }
    }

    public static String compress(String raw) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (OutputStream out = new DeflaterOutputStream(bos)) {
            StreamUtils.copy(raw.getBytes(), out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return Base64Utils.encodeToUrlSafeString(bos.toByteArray());
    }

    public static String decompress(String raw) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (OutputStream out = new InflaterOutputStream(bos)) {
            StreamUtils.copy(Base64Utils.decodeFromUrlSafeString(raw), out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bos.toString();
    }
}
