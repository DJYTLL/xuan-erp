package com.xuan.erp.gateway.infrastructure.security;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.xuan.erp.common.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Sentinel Gateway 统一限流响应处理器。
 */
public class GatewaySentinelBlockRequestHandler implements BlockRequestHandler {

    public static final String ERROR_CODE = "GATEWAY_RATE_LIMITED";
    public static final String ERROR_MESSAGE = "请求过于频繁，请稍后再试";

    @Override
    public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable throwable) {
        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(ApiResponse.failure(ERROR_CODE, ERROR_MESSAGE));
    }
}
