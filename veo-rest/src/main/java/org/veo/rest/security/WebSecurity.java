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

import static java.util.function.Function.identity;
import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.veo.core.entity.EntityType;
import org.veo.rest.CatalogController;
import org.veo.rest.DomainController;
import org.veo.rest.TypeDefinitionsController;
import org.veo.rest.UnitController;
import org.veo.rest.schemas.resource.EntitySchemaResource;
import org.veo.rest.schemas.resource.TranslationsResource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

/** This class bundles custom API security configurations. */
@Configuration
@Slf4j
public class WebSecurity {

  private static final String ROOT_PATH = "/";

  private static final String[] DOMAINTEMPLATE_PATHS = {
    "/domaintemplates/**",
  };

  private static final String[] CONTENT_CREATION_PATHS = {
    "/content-creation/**",
  };

  private static final String ZERO_OR_MORE_DIRECTORIES = "/**";

  private static final String[] DOMAIN_PATHS = {
    DomainController.URL_BASE_PATH + ZERO_OR_MORE_DIRECTORIES
  };

  // Paths to domain specifications and resources that are part of the domain aggregate:
  private static final String[] DOMAIN_RESOURCE_PATHS = {
    EntitySchemaResource.URL_BASE_PATH + ZERO_OR_MORE_DIRECTORIES,
    TranslationsResource.URL_BASE_PATH + ZERO_OR_MORE_DIRECTORIES,
    CatalogController.URL_BASE_PATH + ZERO_OR_MORE_DIRECTORIES,
    TypeDefinitionsController.URL_BASE_PATH + ZERO_OR_MORE_DIRECTORIES
  };

  // Paths to domain elements:
  private static final Stream<String> ELEMENT_PATHS =
      EntityType.ELEMENT_PLURAL_TERMS.stream().map("/%s/**"::formatted);

  // Resources that are not domain elements (see above) but should be protected by the same
  // policies:
  private static final Stream<String> NON_ELEMENT_PATHS =
      Stream.of(UnitController.URL_BASE_PATH + ZERO_OR_MORE_DIRECTORIES);

  // Paths that should be writable by regular users (users that do not have a special role):
  private static final String[] USER_EDITABLE_PATHS =
      Stream.of(ELEMENT_PATHS, NON_ELEMENT_PATHS, Stream.of(DOMAIN_PATHS))
          .flatMap(identity())
          .toArray(String[]::new);

  // Paths that should be visible to regular users:
  private static final String[] USER_VIEWABLE_PATHS =
      Stream.of(
              Stream.of(USER_EDITABLE_PATHS),
              Stream.of(DOMAIN_RESOURCE_PATHS),
              Stream.of(DOMAINTEMPLATE_PATHS))
          .flatMap(identity())
          .toArray(String[]::new);

  // Paths that require the role 'content-creator' for read access:
  private static final String CONTENT_CREATOR_PATHS = "/content-creation/**";

  // Paths that must only be accessible by the admin role:
  private static final String[] ADMIN_PATHS = {"/admin/**", "/domaintemplates/*/createdomains"};

  // Paths that never change state on the server:
  // Searches and inspections are transient and may be POSTed by regular users.
  private static final String[] TRANSIENT_PATHS = {"/**/searches/**", "/**/evaluation/**"};

  // Paths to monitoring and metrics information:
  private static final String ACTUATOR_PATHS = "/actuator/**";

  // Paths to the Swagger-UI OpenAPI frontend:
  private static final String[] SWAGGER_UI_PATHS = {
    "/v2/api-docs/**",
    "/v3/api-docs/**",
    "/swagger.json",
    "/swagger-ui.html",
    "/swagger-resources/**",
    "/webjars/**",
    "/swagger-ui/**"
  };

  @Value("${veo.cors.origins}")
  private String[] origins;

  @Value("${veo.cors.headers}")
  private String[] allowedHeaders;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(
        new Customizer<CsrfConfigurer<HttpSecurity>>() {
          @Override
          @SuppressFBWarnings("SPRING_CSRF_PROTECTION_DISABLED")
          public void customize(CsrfConfigurer<HttpSecurity> csrf) {
            csrf.disable();
          }
        });
    http.cors(Customizer.withDefaults());
    http.headers(headers -> headers.cacheControl(cc -> cc.disable()));

    // Anonymous access (a user with role "ROLE_ANONYMOUS" must be enabled for
    // swagger-ui). We cannot disable it.
    // Make sure that no critical API can be accessed by an anonymous user!
    // .anonymous().disable()

    http.sessionManagement(
        sessionManagement ->
            sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    http.authorizeHttpRequests(
        auth -> {
          // public access to root and actuator endpoints:
          auth.requestMatchers(ROOT_PATH, ACTUATOR_PATHS).permitAll();

          // public access to swagger-ui:
          auth.requestMatchers(HttpMethod.GET, SWAGGER_UI_PATHS).permitAll();

          // admin access:
          for (String path : ADMIN_PATHS) {
            auth.requestMatchers(antMatcher(path)).hasRole("veo-admin");
          }

          // POST is allowed to transient paths for regular users:
          for (String path : TRANSIENT_PATHS) {
            auth.requestMatchers(antMatcher(HttpMethod.POST, path)).hasRole("veo-user");
          }

          // content-creator access:
          auth.requestMatchers(antMatcher(CONTENT_CREATOR_PATHS)).hasRole("veo-content-creator");

          // read-only access:
          Stream.of(USER_VIEWABLE_PATHS)
              .forEach(
                  path ->
                      Stream.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS)
                          .forEach(
                              method ->
                                  auth.requestMatchers(antMatcher(method, path))
                                      .hasRole("veo-user")));

          // write-only access:
          Stream.of(USER_EDITABLE_PATHS)
              .forEach(
                  path ->
                      Stream.of(
                              HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)
                          .forEach(
                              method ->
                                  auth.requestMatchers(antMatcher(method, path))
                                      .hasRole("veo-write")));

          // authentication without specific role requirements and fallback in case of missing
          // paths:
          auth.anyRequest().hasRole("veo-user");
        });

    http.oauth2ResourceServer(
        oauth2ResourceServer ->
            oauth2ResourceServer.jwt(
                jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
    return http.build();
  }

  private JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter =
        new JwtGrantedAuthoritiesConverter();
    jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
    jwtGrantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
    return jwtAuthenticationConverter;
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
}
