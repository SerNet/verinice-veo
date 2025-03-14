/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

import static org.veo.rest.TypeDefinitionsController.URL_BASE_PATH;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.veo.adapter.presenter.api.TypeDefinition;
import org.veo.adapter.presenter.api.TypeDefinitionProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping(URL_BASE_PATH)
@AllArgsConstructor
@Schema(description = "Provides entity type meta information.")
@Deprecated
// TODO #3698: remove
public class TypeDefinitionsController {
  public static final String URL_BASE_PATH = "/types";

  private final TypeDefinitionProvider provider;

  @GetMapping
  @Operation(summary = "Retrieves meta information for element types")
  public CompletableFuture<Map<String, TypeDefinition>> getAll() {
    return CompletableFuture.supplyAsync(provider::getAll);
  }
}
