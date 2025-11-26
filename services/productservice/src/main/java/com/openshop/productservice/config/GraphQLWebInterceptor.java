package com.openshop.productservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;


/**
 * Interceptor to extract authentication headers and add them to GraphQL context
 * This allows resolvers to access X-User-Id and X-User-Role headers
 */
@Component
@Slf4j
public class GraphQLWebInterceptor implements WebGraphQlInterceptor {

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        // Extract headers from HTTP request
        String userId = request.getHeaders().getFirst("x-user-id");
        String userRole = request.getHeaders().getFirst("x-user-role");
        
        log.debug("GraphQL Request - User ID: {}, Role: {}", userId, userRole);
        
        // Add headers to GraphQL context so resolvers can access them
        request.configureExecutionInput((executionInput, builder) -> {
            return builder.graphQLContext(contextBuilder -> {
                if (userId != null) {
                    contextBuilder.put("X-User-Id", userId);
                }
                if (userRole != null) {
                    contextBuilder.put("X-User-Role", userRole);
                }
            }).build();
        });
        
        return chain.next(request);
    }
}
