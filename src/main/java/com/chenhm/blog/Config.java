package com.chenhm.blog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.EntityResponse;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;

import com.chenhm.blog.runner.BlogProperties;

import reactor.core.publisher.Mono;

@Configuration
public class Config {
    private Resource location;

    public Config(BlogProperties properties) {
        location = new FileSystemResource(properties.getApp().getDist() + "/");
    }

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
                    String path = request.pathContainer().subPath(1).value();
                    return getServerResponseMono(path);
                })
                .after((serverRequest, serverResponse) -> {
                    if (serverResponse instanceof EntityResponse) {
                        EntityResponse response = (EntityResponse) serverResponse;
                        EntityResponse.Builder<Object> builder = EntityResponse.fromObject(response.entity());
//                        MediaType mediaType = MediaTypeFactory.getMediaType(serverRequest.path()).orElse(TEXT_HTML);
                        if (serverRequest.path().endsWith(".css")) {
                            builder.header(HttpHeaders.CONTENT_TYPE, "text/css");
                        } else if (serverRequest.path().endsWith(".js")) {
                            builder.header(HttpHeaders.CONTENT_TYPE, "application/javascript");
                        } else if (serverRequest.path().endsWith(".json")) {
                            builder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                        } else if (serverRequest.path().endsWith(".svg")) {
                            builder.header(HttpHeaders.CONTENT_TYPE, "image/svg+xml");
                        } else {
                            builder.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
                        }

                        return builder.build().block();
                    }
                    return serverResponse;
                })
                .build();
    }

    private Mono<ServerResponse> getServerResponseMono(String path) {
        try {
            Resource resource = location.createRelative(path);
            if (resource.exists() && resource.isReadable()) {
                return EntityResponse.fromObject(resource).build().map(resp -> resp);
            } else {
                return ServerResponse.temporaryRedirect(URI.create("/list/1")).build();
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
