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

import org.asciidoctor.Attributes;
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
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
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

    public PlantumlProcessor() {
    }

    ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("{}:{}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(resp -> {
            log.info("{}:{}", resp.statusCode(), resp.headers().contentType().get());
            return Mono.just(resp);
        });
    }


    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        String backend = (String) parent.getDocument().getAttribute(Attributes.BACKEND);
        String uml = reader.read().trim();
        if (!uml.startsWith("@start"))
            uml = "@startuml\n" + uml + "\n@enduml";
        String title = (String) attributes.get("title");

        return "pdf".equals(backend) ? processPDF(parent, uml, title) : processHTML(parent, uml, title);
    }

    private String getBase64Img(String uml) {
        if (uml.startsWith("@startditaa"))
            return "data:image/png;base64," + Base64Utils.encodeToString(getPNGImg(uml));
        else
            return "data:image/svg+xml;base64," + Base64Utils.encodeToString(getSVGImg(uml).getBytes());
    }

    private String svgCss = "<style type=\"text/css\"><![CDATA[" +
            "text{font-family:KaiGen Gothic CN,Microsoft YaHei,Arial,sans-serif}" +
            "]]></style>";

    private String getSVGImg(String uml) {
        SourceStringReader reader = new SourceStringReader(uml);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            reader.outputImage(os, new FileFormatOption(FileFormat.SVG, false));
            return insertString(os.toString(), svgCss, "<defs/>", "<defs>")
                    .replaceAll(" font-family=\"sans-serif\"", "");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private byte[] getPNGImg(String uml) {
        SourceStringReader reader = new SourceStringReader(uml);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            reader.outputImage(os, new FileFormatOption(FileFormat.PNG, false));
            return os.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String insertString(String originalString, String toBeInserted, String... before) {
        for (String s : before) {
            int pos = originalString.indexOf(s);
            if (pos >= 0)
                return new StringBuffer(originalString).insert(pos, toBeInserted).toString();
        }
        return originalString;
    }

    private String getSVGImgRemote(String uml) {
        String url = "https://kroki.io/plantuml/svg/";
        ClientResponse response = WebClient.builder().filter(logResponse()).filters(filter -> filter.add(logRequest())).baseUrl(url).build()
                .get().uri(compress(uml)).exchange().doOnError(throwable -> throwable.printStackTrace()).block();
        return response.bodyToMono(byte[].class).defaultIfEmpty("".getBytes()).map(Base64Utils::encodeToString).block();
    }

    private Object processHTML(StructuralNode parent, String uml, String title) {
        if (plantumlAsImg) {
            String img = getBase64Img(uml);
            Block block = createBlock(parent, "image", "",
                    Maps.<String, Object>builder().put("target", img).build());
            block.setCaption("Figure " + parent.getDocument().getAndIncrementCounter("figure") + ". ");
            block.setTitle(title);
            return block;
        } else {
            String svg = String.format("<div class=\"imageblock\">\n" +
                    "<div class=\"content\">%s</div>\n" + (
                    StringUtils.isEmpty(title) ? "" : String.format("<div class=\"title\">%s. %s</div>\n",
                            "Figure " + parent.getDocument().getAndIncrementCounter("figure"),
                            HtmlUtils.htmlEscape(title))) +
                    "</div>", getSVGImg(uml));
            return createBlock(parent, "pass", Arrays.asList(svg));
        }
    }

    private Object processPDF(StructuralNode parent, String uml, String title) {
        String img = getBase64Img(uml);
        Block block = createBlock(parent, "image", "",
                Maps.<String, Object>builder().put("target", img).build());
        block.setCaption("Figure " + parent.getDocument().getAndIncrementCounter("figure") + ". ");
        block.setTitle(title);
        return block;
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
