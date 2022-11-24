/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.rest.common.SearchResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

public abstract class AbstractEntityControllerWithDefaultSearch extends AbstractEntityController {

  @PostMapping(value = "/searches")
  @Operation(summary = "Creates a new search with the given search criteria.")
  public @Valid Future<ResponseEntity<SearchResponse>> createSearch(
      @Parameter(required = false, hidden = true) Authentication auth,
      @Valid @RequestBody SearchQueryDto search) {
    return CompletableFuture.supplyAsync(() -> createSearchResponseBody(search));
  }
}
