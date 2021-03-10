/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.rest.schemas.resource;

import java.util.List;

import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.veo.core.entity.Asset;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.service.SchemaIdentifiersDTO;
import org.veo.rest.RestApplication;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * REST service which provides methods to query schemas for business entities.
 * The schemas that are delivered will be generated according to: - the entity,
 * i.e. "Process" - the domain, i.e. "GDPR" - the user's granted authorities (to
 * be determined - currently all properties are returned regardless of the user)
 */
@RestController
@RequestMapping(EntitySchemaResource.URL_BASE_PATH)
@SecurityRequirement(name = RestApplication.SECURITY_SCHEME_OAUTH)
// @RolesAllowed("SCOPE_veo-user") // configured in WebSecurityConfig instead.
// Other scopes could be used to secure individual methods.
public interface EntitySchemaResource {

    String URL_BASE_PATH = "/schemas";

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Returns a list of all available entity schemas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Schemas returned",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) })
    @Valid
    SchemaIdentifiersDTO getSchemas(@Parameter(hidden = true) Authentication auth);

    // @formatter:off
    @GetMapping(value = "/{type:[\\w]+}")
    @Operation(summary = "Retrieves an entity schema.")
    @ApiResponses(value = {
            // TODO reference new metaschema here (not yet available):
            @ApiResponse(responseCode = "200",
                         description = "Schema loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Schema not found")
    })
    ResponseEntity<String> getSchema(

            @Parameter(hidden = true) Authentication auth,

            @Parameter(required = true,
                description = "The entity for which the schema will be returned.",
                example = Process.SINGULAR_TERM,
                schema = @Schema(
                    type = "string",
                    allowableValues = { Asset.SINGULAR_TERM, Control.SINGULAR_TERM, Document.SINGULAR_TERM,
                            Incident.SINGULAR_TERM, Person.SINGULAR_TERM, Process.SINGULAR_TERM, Scenario.SINGULAR_TERM,
                            Scope.SINGULAR_TERM },
                    description = "A valid entity type identifier."
                )
            )
            @PathVariable String type,

            @Parameter(required = true,
                description = "A list of domains. Attributes of these domains will be returned for the given entity type.",
                example = "GDPR,ISO_27001",
                schema = @Schema(
                    type = "string",
                    description = "List of domain identifiers - must not contain any reserved characters "
                            + "defined in RFC 3986."
                )
            )
            @RequestParam(value = "domains") List<String> domains);
    // @formatter:on
}
