/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2018  Alexander Ben Nasrallah.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.rest.security;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.veo.persistence.CurrentUserProvider;
import org.veo.persistence.LenientCurrentUserProviderImpl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

/**
 * This class bundles custom API security configurations.
 */
@EnableWebSecurity
@Slf4j
public class WebSecurity extends WebSecurityConfigurerAdapter {

    @Value("${veo.cors.origins}")
    private String[] origins;

    @Value("${veo.cors.headers}")
    private String[] allowedHeaders;

    @SuppressFBWarnings("SPRING_CSRF_PROTECTION_DISABLED")
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf()
            .disable();
        http.cors();
        http.headers()
            .cacheControl()
            .disable();

        // Anonymous access (a user with role "ROLE_ANONYMOUS" must be enabled for
        // swagger-ui). We cannot disable it.
        // Make sure that no critical API can be accessed by an anonymous user!
        // .anonymous().disable()

        http.authorizeRequests()
            .antMatchers("/actuator/**")
            .permitAll();

        http.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http.authorizeRequests()
            .antMatchers(HttpMethod.GET, "/", "/v2/api-docs/**", "/v3/api-docs/**", "/swagger.json",
                         "/swagger-ui.html", "/swagger-resources/**", "/webjars/**",
                         "/swagger-ui/**")
            .permitAll()

            .antMatchers(HttpMethod.POST, "/domains/**", "/domaintemplates/")
            .hasRole("veo-content-creator")

            .antMatchers("/units/**", "/assets/**", "/controls/**", "/scopes/**", "/persons/**",
                         "/processes/**", "/schemas/**", "/translations/**", "/domains/**")
            .hasRole("veo-user")

            .antMatchers("/admin/**", "/domaintemplates/*/createdomains")
            .hasRole("veo-admin")

            .anyRequest()
            .authenticated(); // CAUTION:
                              // this includes anonymous users,
                              // see above

        http.oauth2ResourceServer()
            .jwt()
            .jwtAuthenticationConverter(jwtAuthenticationConverter());
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(inMemoryUserDetailsManager());
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Some basic headers are always needed, additional headers are
        // configurable:
        corsConfig.addAllowedHeader(HttpHeaders.AUTHORIZATION);
        corsConfig.addAllowedHeader(HttpHeaders.CONTENT_TYPE);
        corsConfig.addAllowedHeader(HttpHeaders.IF_MATCH);
        corsConfig.addAllowedHeader(HttpHeaders.IF_NONE_MATCH);
        Arrays.stream(allowedHeaders)
              .peek(s -> log.debug("Added CORS allowed header: {}", s))
              .forEach(corsConfig::addAllowedHeader);
        Arrays.stream(origins)
              .peek(s -> log.debug("Added CORS origin pattern: {}", s))
              .forEach(corsConfig::addAllowedOriginPattern);
        corsConfig.setMaxAge(Duration.ofMinutes(30));
        corsConfig.addExposedHeader(HttpHeaders.ETAG);
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }

    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager() {
        final String nilUUID = "00000000-0000-0000-0000-000000000000";

        ApplicationUser basicUser = ApplicationUser.authenticatedUser("user", nilUUID, "veo-user",
                                                                      Collections.emptyList());
        basicUser.setAuthorities(List.of(new SimpleGrantedAuthority("SCOPE_veo-user")));

        ApplicationUser adminUser = ApplicationUser.authenticatedUser("admin", nilUUID, "veo-admin",
                                                                      Collections.emptyList());
        adminUser.setAuthorities(List.of(new SimpleGrantedAuthority("SCOPE_veo-admin")));

        return new CustomUserDetailsManager(List.of(basicUser, adminUser));
    }

    @Bean
    @Primary
    public CurrentUserProvider testCurrentUserProvider(AuditorAware<String> auditorAware) {
        return new LenientCurrentUserProviderImpl(auditorAware);
    }
}
