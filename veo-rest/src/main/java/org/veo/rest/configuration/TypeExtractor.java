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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import org.veo.adapter.presenter.api.dto.ModelDto;
import org.veo.adapter.service.domaintemplate.dto.ExportDomainTemplateDto;
import org.veo.core.entity.exception.UnprocessableDataException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Uses Spring's {@code RequestMappingHandlerMapping} to resolve a URI to the DTO returned by the
 * controller method that is mapped to the URI.
 *
 * <p>Unwraps the DTO type from the generic type parameter if it is returned via an adapter or
 * decorator, i.e. as a {@code CompletableFuture} or {@code ResponseBody}.
 */
@Component
@AllArgsConstructor
@Slf4j
public class TypeExtractor {

  @Autowired private ApplicationContext applicationContext;

  private RequestMappingHandlerMapping getRequestHandlerMapping() {
    return applicationContext.getBean(
        "requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
  }

  public Optional<Class<? extends ModelDto>> parseDtoType(String uriString) {
    return extractDtoType(getMethodParam(uriString).getGenericParameterType());
  }

  private MethodParameter getMethodParam(String uriString) {
    UriComponents uriComponents = UriComponentsBuilder.fromUriString(uriString).build();
    String pathComponent = uriComponents.getPath();
    if (pathComponent != null) {
      log.debug("Reduced URI string {} to path component {}", uriString, pathComponent);
      var pathContainer = PathContainer.parsePath(pathComponent);
      // Incrementally strip segments from the start of the path until a matching endpoint is found.
      // This is necessary because the path may start with an unknown context path (e.g. /apps/veo/)
      while (!pathContainer.value().isEmpty()) {
        var type = findMethodParam(pathContainer);
        if (type.isPresent()) {
          return type.get();
        }
        pathContainer = pathContainer.subPath(1);
      }
    }
    throw new UnprocessableDataException(String.format("No mapping found for URI: %s", uriString));
  }

  private Optional<Class<? extends ModelDto>> findDtoType(PathContainer pathContainer) {
    log.debug("Searching for matching endpoint for path {}", pathContainer.value());

    if (pathContainer.value().startsWith("/domain-templates")) {
      return Optional.of(ExportDomainTemplateDto.class);
    }

    return findMethodParam(pathContainer)
        .map(MethodParameter::getGenericParameterType)
        .flatMap(this::extractDtoType);
  }

  private Optional<MethodParameter> findMethodParam(PathContainer pathContainer) {
    return getRequestHandlerMapping().getHandlerMethods().entrySet().stream()
        .filter(
            entry ->
                entry.getKey().getMethodsCondition().getMethods().stream()
                    .anyMatch(m -> m == RequestMethod.GET))
        .filter(e -> e.getKey().getPathPatternsCondition() != null)
        .filter(
            e ->
                e.getKey().getPathPatternsCondition().getPatterns().stream()
                    .anyMatch(pattern -> pattern.matches(pathContainer)))
        .peek(
            a ->
                log.debug(
                    "Found match for {} in {}",
                    pathContainer.value(),
                    a.getKey().getPathPatternsCondition().getPatterns()))
        .map(e -> e.getValue().getReturnType())
        .peek(
            e ->
                log.debug(
                    "Found return type {} for URI string {}",
                    e.getGenericParameterType().getTypeName(),
                    pathContainer.value()))
        .findFirst();
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
