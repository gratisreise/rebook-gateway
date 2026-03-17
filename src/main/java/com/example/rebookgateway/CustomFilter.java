package com.example.rebookgateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomFilter implements GlobalFilter, Ordered {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PASSPORT_HEADER = "X-Passport";

    private final JwtUtil jwtUtil;
    private final WebClient.Builder lbWebClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String uri = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();
        log.info("Request: {} {}", method, uri);

        // 1. 인증 불필요 경로
        if (isPublicPath(uri)) {
            return chain.filter(exchange);
        }

        // 2. WebSocket 연결
        if (isWebSocketRequest(uri)) {
            return handleWebSocket(exchange, chain);
        }

        // 3. SSE 연결
        if (isSSERequest(uri)) {
            return handleSSE(exchange, chain);
        }

        // 4. REST API
        return handleRestApi(exchange, chain);
    }

    private boolean isPublicPath(String uri) {
        return uri.startsWith("/api/auth")
            || uri.startsWith("/swagger-ui")
            || uri.startsWith("/v3/api-docs")
            || uri.startsWith("/swagger-resources")
            || uri.equals("/favicon.ico");
    }

    private boolean isWebSocketRequest(String uri) {
        return uri.startsWith("/api/ws-chat");
    }

    private boolean isSSERequest(String uri) {
        return uri.startsWith("/api/notifications/sse");
    }

    private Mono<Void> handleWebSocket(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest().getQueryParams().getFirst("token");

        if (token == null || !jwtUtil.validateToken(token)) {
            log.error("WebSocket connection rejected: invalid or missing token");
            return onError(exchange);
        }

        return getPassport(exchange, chain, token);
    }

    private Mono<Void> handleSSE(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest().getQueryParams().getFirst("token");

        if (token == null || !jwtUtil.validateToken(token)) {
            log.error("SSE connection rejected: invalid or missing token");
            return onError(exchange);
        }

        return getPassport(exchange, chain, token);
    }

    private Mono<Void> handleRestApi(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = getToken(exchange);

        if (token.isBlank() || !jwtUtil.validateToken(token)) {
            log.error("REST API rejected: missing or invalid Authorization header");
            return onError(exchange);
        }

        return getPassport(exchange, chain, token);
    }

    private Mono<Void> getPassport(ServerWebExchange exchange, GatewayFilterChain chain, String token) {
        return lbWebClient
            .build()
            .post()
            .uri(uriBuilder -> uriBuilder
                .scheme("lb")
                .host("AUTH-SERVICE")
                .path("/passports")
                .queryParam("jwt", token)
                .build()
            )
            .retrieve()
            .bodyToMono(String.class)
            .flatMap(passport -> {
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(PASSPORT_HEADER, passport)
                    .build();
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            });
    }

    private String getToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return "";
        }
        return authHeader.substring(BEARER_PREFIX.length());
    }

    private Mono<Void> onError(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -10;
    }
}
