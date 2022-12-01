package com.chenhm.blog.engine;

import org.jetbrains.annotations.NotNull;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.AttributeProviderFactory;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.html.MutableAttributes;

public class HeadingsAttributeProvider {
    static class HeadingsExtension implements HtmlRenderer.HtmlRendererExtension {
        @Override
        public void rendererOptions(final MutableDataHolder options) {
            // add any configuration settings to options you want to apply to everything, here
        }

        @Override
        public void extend(final HtmlRenderer.Builder rendererBuilder, final String rendererType) {
            rendererBuilder.attributeProviderFactory(SampleAttributeProvider.Factory());
        }

        static HeadingsExtension create() {
            return new HeadingsExtension();
        }
    }

    static class SampleAttributeProvider implements AttributeProvider {

        @Override
        public void setAttributes(@NotNull Node node, @NotNull AttributablePart part, @NotNull MutableAttributes attributes) {
            if (node instanceof Heading) {
                attributes.addValue("class", "md");
            }
        }

        static AttributeProviderFactory Factory() {
            return new IndependentAttributeProviderFactory() {
                @Override
                public AttributeProvider apply(LinkResolverContext context) {
                    return new SampleAttributeProvider();
                }
            };
        }
    }

}
