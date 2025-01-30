/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.rest.schemas.resource;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.veo.adapter.presenter.api.dto.TranslationsDto;
import org.veo.rest.RestApplication;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/** REST endpoint which provides methods to UI translations in JSON format. */
@RestController
@RequestMapping(TranslationsResource.URL_BASE_PATH)
@SecurityRequirement(name = RestApplication.SECURITY_SCHEME_OAUTH)
// @RolesAllowed("ROLE_USER")
public interface TranslationsResource {

  String URL_BASE_PATH = "/translations";

  @GetMapping()
  @Operation(
      summary = "Retrieves a map of UI translation key-value pairs.",
      externalDocs =
          @ExternalDocumentation(
              description = "Languages are specified as IANA language subtags",
              url =
                  "https://www.iana.org/assignments/language-subtag-registry/language-subtag-registry"))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Translations for requested languages",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TranslationsDto.class))),
    @ApiResponse(responseCode = "404", description = "Translation not found")
  })
  CompletableFuture<ResponseEntity<TranslationsDto>> getSchema(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(
              required = true,
              description =
                  "The language(s) for which the translation key-value pairs are returned.",
              example = "de,en,pr",
              schema =
                  @Schema(type = "string", description = "IETF BCP 47 language tag (see RFC 5646)"))
          @RequestParam(value = "languages")
          Set<String> languages,
      @Parameter(
              required = false,
              description = "The domain id whose translations are to be included.",
              example = "15f58e45-48b7-409e-a32f-48d208aac5d5",
              schema =
                  @Schema(
                      type = "string",
                      description = "must be a valid UUID string following RFC 4122"))
          @RequestParam(value = "domain", required = false)
          String domainId);
}
