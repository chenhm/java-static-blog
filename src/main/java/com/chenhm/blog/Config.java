package com.chenhm.blog;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;

import com.chenhm.blog.runner.BlogProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import reactor.core.publisher.Mono;

@Configuration
public class Config {
    private Resource location;

    private Cache<String, ServerResponse> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(3, TimeUnit.SECONDS)
            .build();

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
                .build();
    }

    private Mono<ServerResponse> getServerResponseMono(String path) {
        ServerResponse response = cache.getIfPresent(path);
        if (response != null) {
            return Mono.just(response);
        }

        try {
            Resource resource = location.createRelative(path);
            if (resource.exists() && resource.isReadable()) {
                MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.TEXT_HTML);
                return ServerResponse.ok().contentType(mediaType).syncBody(Files.readAllBytes(resource.getFile().toPath()))
                        .doOnNext(resp -> cache.put(path, resp));
            } else {
                return ServerResponse.temporaryRedirect(URI.create("/list/1")).build();
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
