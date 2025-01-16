package vstu.isd.notebin.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import vstu.isd.notebin.controller.filter.JwtAuthFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf().disable() // TODO fix this
                .authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                        .requestMatchers(HttpMethod.POST, "/api/v1/note").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/note/{url}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/analytics/view-notes").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, BasicAuthenticationFilter.class);

        return http.build();
    }
}
