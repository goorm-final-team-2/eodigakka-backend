package com.eodigakka.global.config;

import com.eodigakka.global.security.CustomAccessDeniedHandler;
import com.eodigakka.global.security.CustomAuthenticationEntryPoint;
import com.eodigakka.global.security.GuestAuthenticationFilter;
import com.eodigakka.global.security.JwtAuthenticationFilter;
import com.eodigakka.global.security.SecurityAuthority;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final GuestAuthenticationFilter guestAuthenticationFilter;
  private final CustomAuthenticationEntryPoint authenticationEntryPoint;
  private final CustomAccessDeniedHandler accessDeniedHandler;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      GuestAuthenticationFilter guestAuthenticationFilter,
      CustomAuthenticationEntryPoint authenticationEntryPoint,
      CustomAccessDeniedHandler accessDeniedHandler) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.guestAuthenticationFilter = guestAuthenticationFilter;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.accessDeniedHandler = accessDeniedHandler;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exception ->
                exception
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(
                        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health")
                    .permitAll()
                    .requestMatchers("/ws/**")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST, "/api/auth/kakao", "/api/auth/refresh", "/api/auth/logout")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/dev/auth/login")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/appointments/invite/*")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/appointments/guests")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/appointments/*/members")
                    .hasAnyAuthority(SecurityAuthority.USER, SecurityAuthority.GUEST)
                    .requestMatchers("/api/appointments/*/place-candidates/**")
                    .hasAnyAuthority(SecurityAuthority.USER, SecurityAuthority.GUEST)
                    .requestMatchers(HttpMethod.PUT, "/api/appointments/*/votes")
                    .hasAnyAuthority(SecurityAuthority.USER, SecurityAuthority.GUEST)
                    .requestMatchers(HttpMethod.GET, "/api/appointments/*/votes/results")
                    .hasAnyAuthority(SecurityAuthority.USER, SecurityAuthority.GUEST)
                    .requestMatchers(HttpMethod.GET, "/api/appointments/*/confirmed-place")
                    .hasAnyAuthority(SecurityAuthority.USER, SecurityAuthority.GUEST)
                    .requestMatchers("/api/appointments/*/locations/**")
                    .hasAnyAuthority(SecurityAuthority.USER, SecurityAuthority.GUEST)
                    .anyRequest()
                    .hasAuthority(SecurityAuthority.USER))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(guestAuthenticationFilter, JwtAuthenticationFilter.class)
        .build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins}") String allowedOrigins) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .toList());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of("Location"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
