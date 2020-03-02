package com.chenhm.blog.engine;

import static com.chenhm.blog.engine.DitaaProcessor.TAG;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Contexts;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.Reader;
import org.springframework.util.Base64Utils;

import com.chenhm.blog.util.Maps;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

@Contexts({Contexts.LITERAL, Contexts.LISTING, Contexts.OPEN})
@ContentModel(ContentModel.RAW)
@Name(TAG)
@Slf4j
public class DitaaProcessor extends BlockProcessor {
    public static final String TAG = "ditaa";

    @Override
    public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
        String uml = reader.read();
        uml = "@startditaa\n" + uml + "\n@endditaa";
        String title = (String) attributes.get("title");

        return process(parent, uml, title);
    }

    private String getBase64Img(String uml) {
        return "data:image/png;base64," + Base64Utils.encodeToString(getPNGImg(uml));
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

    private Object process(StructuralNode parent, String uml, String title) {
        String png = getBase64Img(uml);
        Block block = createBlock(parent, "image", "",
                Maps.<String, Object>builder().put("target", png).build());
        block.setCaption("Figure " + parent.getDocument().getAndIncrementCounter("figure") + ". ");
        block.setTitle(title);
        return block;
    }
}
