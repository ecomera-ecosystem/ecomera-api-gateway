package com.ecomera.gateway.filter;

import com.ecomera.gateway.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthGlobalFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private JwtAuthGlobalFilter filter;

    @Captor
    private ArgumentCaptor<ServerWebExchange> exchangeCaptor;

    private final UUID userId = UUID.randomUUID();
    private final String email = "test@example.com";
    private final String validToken = "valid.jwt.token";
    private final String authHeader = "Bearer " + validToken;

    static Stream<String> whitelistedPaths() {
        return Stream.of(
                "/api/v1/auth/register",
                "/actuator/health",
                "/swagger-ui/index.html",
                "/v3/api-docs"
        );
    }

    static Stream<String> publicGetPaths() {
        return Stream.of(
                "/api/v1/products",
                "/api/v1/categories/electronics"
    );
    }

    @ParameterizedTest
    @MethodSource("whitelistedPaths")
    void filter_shouldAllowWhitelistedPaths(String path) {
        given(chain.filter(any())).willReturn(Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(path).build()
        );

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @ParameterizedTest
    @MethodSource("publicGetPaths")
    void filter_shouldAllowPublicGetPaths(String path) {
        given(chain.filter(any())).willReturn(Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(path).build()
        );

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    void filter_shouldReturn401_whenNoAuthHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/protected").build()
        );

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_shouldReturn401_whenAuthHeaderIsNotBearer() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/protected")
                        .header("Authorization", "Basic token").build()
        );

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_shouldReturn401_whenTokenInvalid() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/protected")
                        .header("Authorization", authHeader).build()
        );

        given(jwtUtil.validateToken(validToken)).willReturn(false);

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_shouldForwardRequestWithHeaders_whenTokenValid() {
        given(chain.filter(any())).willReturn(Mono.empty());
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/protected")
                .header("Authorization", authHeader).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        given(jwtUtil.validateToken(validToken)).willReturn(true);
        given(jwtUtil.extractUserId(validToken)).willReturn(userId);
        given(jwtUtil.extractEmail(validToken)).willReturn(email);
        given(jwtUtil.extractRoles(validToken)).willReturn("USER");

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchangeCaptor.capture());
        ServerWebExchange captured = exchangeCaptor.getValue();
        assertThat(captured.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo(userId.toString());
        assertThat(captured.getRequest().getHeaders().getFirst("X-User-Email")).isEqualTo(email);
        assertThat(captured.getRequest().getHeaders().getFirst("X-User-Roles")).isEqualTo("USER");
    }

    @Test
    void filter_shouldForwardRequestWithEmptyRoles_whenRolesNull() {
        given(chain.filter(any())).willReturn(Mono.empty());
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/protected")
                .header("Authorization", authHeader).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        given(jwtUtil.validateToken(validToken)).willReturn(true);
        given(jwtUtil.extractUserId(validToken)).willReturn(userId);
        given(jwtUtil.extractEmail(validToken)).willReturn(email);
        given(jwtUtil.extractRoles(validToken)).willReturn(null);

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchangeCaptor.capture());
        ServerWebExchange captured = exchangeCaptor.getValue();
        assertThat(captured.getRequest().getHeaders().getFirst("X-User-Roles")).isEmpty();
    }

    @Test
    void filter_shouldReturn401_whenJwtProcessingFails() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/protected")
                .header("Authorization", authHeader).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        given(jwtUtil.validateToken(validToken)).willReturn(true);
        given(jwtUtil.extractUserId(validToken)).willThrow(new RuntimeException("Unexpected error"));

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
