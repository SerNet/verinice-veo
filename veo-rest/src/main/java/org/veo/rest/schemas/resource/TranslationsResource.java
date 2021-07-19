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

import org.veo.rest.RestApplication;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * REST endpoint which provides methods to UI translations in JSON format.
 */
@RestController
@RequestMapping(TranslationsResource.URL_BASE_PATH)
@SecurityRequirement(name = RestApplication.SECURITY_SCHEME_OAUTH)
// @RolesAllowed("ROLE_USER")
public interface TranslationsResource {

    public static final String URL_BASE_PATH = "/translations";

    // @formatter:off
    @GetMapping()
    @Operation(summary = "Retrieves a map of UI translation key-value pairs.",
        externalDocs = @ExternalDocumentation(
              description = "Languages are specified as IANA language subtags",
              url = "https://www.iana.org/assignments/language-subtag-registry/language-subtag-registry"
        )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Translations for requested languages", // TODO add meta-schema
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Translation not found")
    })
    public CompletableFuture<ResponseEntity<String>> getSchema(

            @Parameter(required = false, hidden = true) Authentication auth,

            @Parameter(required = true,
                allowEmptyValue = false,
                description = "The language(s) for which the translation key-value pairs are returned.",
                example = "de,en,pr",
                allowReserved = false,
                schema = @Schema(
                    type = "string",
                    description = "IANA language subtag"
                )
            )
            @RequestParam(value = "languages", required = true) Set<String> languages);

    // @formatter:on
}
