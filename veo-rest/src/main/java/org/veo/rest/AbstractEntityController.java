/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Urs Zeidler.
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.Client;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Versioned;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.service.DbIdRefResolver;
import org.veo.core.usecase.service.IdRefResolver;
import org.veo.rest.common.SearchResponse;
import org.veo.service.EtagService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractEntityController extends AbstractVeoController {

  @Autowired private RepositoryProvider repositoryProvider;

  @Autowired EntityToDtoTransformer entityToDtoTransformer;

  @Autowired DtoToEntityTransformer dtoToEntityTransformer;

  @Autowired ReferenceAssembler urlAssembler;

  @Autowired protected EtagService etagService;

  protected AbstractEntityController() {}

  protected CacheControl defaultCacheControl = CacheControl.noCache();

  protected IdRefResolver createIdRefResolver(Client client) {
    return new DbIdRefResolver(repositoryProvider, client);
  }

  protected abstract String buildSearchUri(String searchId);

  protected ResponseEntity<SearchResponse> createSearchResponseBody(SearchQueryDto search) {
    try {
      // Build search URI and remove optional request param placeholders.
      var searchUri = buildSearchUri(search.getSearchId()).replaceFirst("\\{[^}]*}", "");
      return ResponseEntity.created(new URI(searchUri)).body(new SearchResponse(searchUri));
    } catch (IOException | URISyntaxException e) {
      log.error("Could not create search.", e);
      throw new IllegalArgumentException(String.format("Could not create search %s", search));
    }
  }

  protected <T extends Identifiable & Versioned> Optional<String> getEtag(
      Class<T> entityClass, String id) {
    return etagService.getEtag(entityClass, id);
  }
}
