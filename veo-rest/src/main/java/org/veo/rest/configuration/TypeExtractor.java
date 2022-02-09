/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman.
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
package org.veo.rest.configuration;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;

import org.veo.adapter.presenter.api.dto.ModelDto;
import org.veo.adapter.presenter.api.dto.full.FullCatalogItemDto;
import org.veo.adapter.service.domaintemplate.dto.TransformDomainTemplateDto;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Uses Spring's {@code RequestMappingHandlerMapping} to resolve a URI to the
 * DTO returned by the controller method that is mapped to the URI.
 *
 * Unwraps the DTO type from the generic type parameter if it is returned via an
 * adapter or decorator, i.e. as a {@code CompletableFuture} or
 * {@code ResponseBody}.
 */
@Component
@AllArgsConstructor
@Slf4j
public class TypeExtractor {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ServletContext servletContext;

    private RequestMappingHandlerMapping getRequestHandlerMapping() {
        return applicationContext.getBean(RequestMappingHandlerMapping.class);
    }

    public Optional<Class<? extends ModelDto>> parseDtoType(String uriString) {
        String pathComponent = UriComponentsBuilder.fromUriString(uriString)
                                                   .build()
                                                   .getPath();
        if (pathComponent == null)
            return Optional.empty();

        if (uriString.startsWith("/catalogitems")) {
            return Optional.of(FullCatalogItemDto.class);
        }
        if (uriString.startsWith("/domaintemplates")) {
            return Optional.of(TransformDomainTemplateDto.class);
        }

        log.debug("Reduced URI string {} to path component {}", uriString, pathComponent);

        var strippedPathComponent = removeServletContextPath(pathComponent);

        var pathContainer = PathContainer.parsePath(strippedPathComponent);

        var returnValue = getRequestHandlerMapping().getHandlerMethods()
                                                    .entrySet()
                                                    .stream()
                                                    .filter(entry -> entry.getKey()
                                                                          .getMethodsCondition()
                                                                          .getMethods()
                                                                          .stream()
                                                                          .anyMatch(m -> m == RequestMethod.GET))
                                                    .filter(e -> e.getKey()
                                                                  .getPathPatternsCondition() != null)
                                                    .filter(e -> e.getKey()
                                                                  .getPathPatternsCondition()
                                                                  .getPatterns()
                                                                  .stream()
                                                                  .anyMatch(pattern -> pattern.matches(pathContainer)))
                                                    .peek(a -> log.debug("Found match for {} in {}",
                                                                         strippedPathComponent,
                                                                         a.getKey()
                                                                          .getPathPatternsCondition()
                                                                          .getPatterns()))
                                                    .map(e -> e.getValue()
                                                               .getReturnType())
                                                    .peek(e -> log.debug("Found return type {} for URI string {}",
                                                                         e.getGenericParameterType()
                                                                          .getTypeName(),
                                                                         uriString))
                                                    .findFirst()
                                                    .orElseThrow(() -> {
                                                        log.warn("No mapping found for URI string {}",
                                                                 uriString);
                                                        return new AssertionError(
                                                                String.format("No mapping found for URI: %s",
                                                                              uriString));
                                                    });

        return extractDtoType(returnValue.getGenericParameterType());
    }

    private String removeServletContextPath(String pathComponent) {
        var contextPath = servletContext.getContextPath();
        if (pathComponent.startsWith(contextPath))
            return pathComponent.substring(contextPath.length());
        else
            return pathComponent;
    }

    private Optional<Class<? extends ModelDto>> extractDtoType(Type type) {
        if (type instanceof Class) {
            return Optional.of((Class<? extends ModelDto>) type);
        }
        try {
            var actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
            return extractDtoType(actualTypeArguments[0]);
        } catch (IndexOutOfBoundsException | ClassCastException e) {
            log.warn("Could not extract DTO type from: {}", type);
        }
        return Optional.empty();
    }
}
