/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import static org.veo.rest.ControllerConstants.UUID_DESCRIPTION;
import static org.veo.rest.ControllerConstants.UUID_EXAMPLE;

import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public interface ElementInDomainResource {
  @GetMapping(value = "/json-schema")
  @ApiResponse(responseCode = "200", description = "Schema loaded")
  @ApiResponse(responseCode = "404", description = "Domain not found")
  CompletableFuture<ResponseEntity<String>> getJsonSchema(
      @Parameter(required = true, hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String domainId);
}
