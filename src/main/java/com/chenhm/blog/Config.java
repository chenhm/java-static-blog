package com.chenhm.blog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.server.EntityResponse;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;

import com.chenhm.blog.runner.BlogProperties;
import com.google.common.net.MediaType;

@Configuration
public class Config {
    @Autowired
    BlogProperties properties;

    @Bean
    public RouterFunction<ServerResponse> htmlRouter() {
        return RouterFunctions.route()
                .before(serverRequest -> {
                    if (!serverRequest.uri().getPath().contains(".")) {
                        URI uri = UriComponentsBuilder.fromUri(serverRequest.uri()).path(".html").build().toUri();
                        return ServerRequest.from(serverRequest).uri(uri).build();
                    }
                    return serverRequest;
                })
                .GET("/**", request -> {
                    Resource location = new FileSystemResource(properties.getApp().getDist() + "/");
                    String path = request.pathContainer().subPath(1).value();
                    try {
                        Resource resource = location.createRelative(path);
                        if (resource.exists() && resource.isReadable()) {
                            return EntityResponse.fromObject(resource).build().cast(ServerResponse.class);
                        } else {
                            return ServerResponse.temporaryRedirect(URI.create("/list/1")).build();
                        }
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                })
//                .resources("/**", new FileSystemResource(properties.getApp().getDist() + "/"))
                .after((serverRequest, serverResponse) -> {
                    if (serverResponse instanceof EntityResponse) {
                        EntityResponse response = (EntityResponse) serverResponse;
                        EntityResponse.Builder<Object> builder = EntityResponse.fromObject(response.entity());

                        if (serverRequest.path().endsWith(".css"))
                            builder.header(HttpHeaders.CONTENT_TYPE, MediaType.CSS_UTF_8.toString());
                        else if (serverRequest.path().endsWith(".js"))
                            builder.header(HttpHeaders.CONTENT_TYPE, MediaType.JAVASCRIPT_UTF_8.toString());
                        else if (serverRequest.path().endsWith(".json"))
                            builder.header(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
                        else if (serverRequest.path().endsWith(".svg"))
                            builder.header(HttpHeaders.CONTENT_TYPE, MediaType.SVG_UTF_8.toString());
                        else
                            builder.header(HttpHeaders.CONTENT_TYPE, MediaType.HTML_UTF_8.toString());

                        return builder.build().block();
                    }
                    return serverResponse;
                })
                .build();
    }
}
