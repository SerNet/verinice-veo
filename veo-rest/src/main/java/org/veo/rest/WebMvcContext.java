/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.rest;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.ElementType;
import org.veo.core.repository.ClientReadOnlyRepository;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.LinkQuery;
import org.veo.core.repository.ParentElementQuery;
import org.veo.rest.common.ClientNotActiveException;
import org.veo.rest.security.ApplicationUser;

import tools.jackson.databind.ObjectMapper;

/** Adds custom controller method parameter resolvers. */
@Configuration
class WebMvcContext implements WebMvcConfigurer {

  @Autowired private ObjectMapper defaultMapper;
  @Autowired private ClientReadOnlyRepository clientRepository;

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
    argumentResolvers.add(new ApplicationUserArgumentResolver());
  }

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer.defaultContentType(MediaType.APPLICATION_JSON);
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.addFirst(new CompactJsonHttpMessageConverter(defaultMapper));
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(
        new Converter<String, ElementType>() {
          @Override
          public ElementType convert(String source) {
            return ElementType.valueOf(source.toUpperCase(Locale.US));
          }
        });
    registry.addConverter(
        new Converter<String, ParentElementQuery.SortCriterion>() {
          @Override
          public ParentElementQuery.SortCriterion convert(String source) {
            return ParentElementQuery.SortCriterion.fromString(source);
          }
        });
    registry.addConverter(
        new Converter<String, LinkQuery.SortCriterion>() {
          @Override
          public LinkQuery.SortCriterion convert(String source) {
            return LinkQuery.SortCriterion.fromString(source);
          }
        });
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(
            new HandlerInterceptor() {
              @Override
              public boolean preHandle(
                  HttpServletRequest request, HttpServletResponse response, Object handler) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                Optional.ofNullable(auth)
                    .map(Authentication::getPrincipal)
                    .map(ApplicationUser::findAuthenticatedUser)
                    .map(UserAccessRights::getClientId)
                    .ifPresent(
                        clientId -> {
                          clientRepository
                              .findById(clientId)
                              .filter(ClientRepository.IS_CLIENT_ACTIVE)
                              .orElseThrow(() -> new ClientNotActiveException(clientId.toString()));
                        });
                return true;
              }
            })
        .excludePathPatterns(
            "/admin/**", "/content-creation/**", "/content-customizing/**", "/messages/**");
  }
}
