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
import static org.veo.rest.ControllerConstants.UUID_PARAM_SPEC;

import java.util.concurrent.Future;

import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.presenter.api.dto.full.FullIncidentInDomainDto;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Incident;
import org.veo.core.usecase.incident.GetIncidentUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

/** REST service which provides methods to manage incidents from the viewpoint of a domain. */
@RestController
@RequestMapping(IncidentInDomainController.URL_BASE_PATH)
@Slf4j
public class IncidentInDomainController
    extends AbstractElementInDomainController<Incident, FullIncidentInDomainDto> {
  public IncidentInDomainController(GetIncidentUseCase getIncidentUseCase) {
    super(Incident.class, getIncidentUseCase);
  }
  // TODO VEO-2000 replace /domians with Domain.PLURAL_TERM
  public static final String URL_BASE_PATH = "/domians/{domainId}/" + Incident.PLURAL_TERM;

  @Override
  @GetMapping(UUID_PARAM_SPEC)
  @Operation(summary = "Loads an incident from the viewpoint of a domain")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Incident loaded",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = FullIncidentInDomainDto.class))),
        @ApiResponse(responseCode = "404", description = "Incident not found"),
        @ApiResponse(responseCode = "404", description = "Domain not found"),
        @ApiResponse(responseCode = "404", description = "Incident not associated with domain"),
      })
  public @Valid Future<ResponseEntity<FullIncidentInDomainDto>> getElement(
      @Parameter(required = true, hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String uuid,
      WebRequest request) {
    return super.getElement(auth, domainId, uuid, request);
  }

  @Override
  protected FullIncidentInDomainDto entity2Dto(Incident entity, Domain domain) {
    return entityToDtoTransformer.transformIncident2Dto(entity, domain);
  }
}
