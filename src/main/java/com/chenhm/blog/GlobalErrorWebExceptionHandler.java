//package com.chenhm.blog;
//
//import java.net.URI;
//import java.util.Map;
//
//import org.springframework.boot.autoconfigure.web.ResourceProperties;
//import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
//import org.springframework.boot.web.reactive.error.ErrorAttributes;
//import org.springframework.context.ApplicationContext;
//import org.springframework.core.annotation.Order;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.codec.ServerCodecConfigurer;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.function.server.RequestPredicates;
//import org.springframework.web.reactive.function.server.RouterFunction;
//import org.springframework.web.reactive.function.server.RouterFunctions;
//import org.springframework.web.reactive.function.server.ServerRequest;
//import org.springframework.web.reactive.function.server.ServerResponse;
//
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//@Component
//@Order(-2)
//public class GlobalErrorWebExceptionHandler extends
//        AbstractErrorWebExceptionHandler {
//
//    public GlobalErrorWebExceptionHandler(ErrorAttributes errorAttributes, ResourceProperties resourceProperties,
//                                          ApplicationContext applicationContext, ServerCodecConfigurer serverCodecConfigurer){
//        super(errorAttributes, resourceProperties, applicationContext);
//        setMessageWriters(serverCodecConfigurer.getWriters());
//    }
//
//    @Override
//    protected RouterFunction<ServerResponse> getRoutingFunction(
//            ErrorAttributes errorAttributes) {
//
//        return RouterFunctions.route(
//                RequestPredicates.all(), this::renderErrorResponse);
//    }
//
//    private Mono<ServerResponse> renderErrorResponse(
//            ServerRequest request) {
//
//        Map<String, Object> errorPropertiesMap = getErrorAttributes(request, false);
//        if(errorPropertiesMap.get("status").equals(404)){
//            return ServerResponse.temporaryRedirect(URI.create("/list/1")).build();
//        }
//        HttpStatus errorStatus = HttpStatus.valueOf(errorPropertiesMap.get("status").toString());
//        ServerResponse.BodyBuilder responseBody = ServerResponse.status(errorStatus).contentType(MediaType.TEXT_HTML);
//        return renderDefaultErrorView(responseBody, errorPropertiesMap);
//    }
//}
