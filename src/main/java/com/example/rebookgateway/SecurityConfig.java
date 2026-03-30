package com.example.rebookgateway;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchange -> exchange
                .pathMatchers(WHITE_LIST).permitAll()
                .pathMatchers(SERVICES).permitAll()
                .anyExchange().denyAll()
            )
            .build();
    }


    private static final String[] SERVICES = {
        "/api/auth/**", // auth
        "/api/users/**", // user
        "/api/books/**", // book
        "/api/tradings/**", // trading
        "/api/chats/**", // chat
        "/api/notifications/**", // noti
        "/api/ws-chat/**" // ws
    };


    private static final String[] WHITE_LIST = {
        "/actuator/**",
        "/eureka/**",
        "/swagger-ui.html",
        "/favicon.ico",
        "/webjars/**"
    };
}

