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

import static org.veo.rest.ControllerConstants.ABBREVIATION_PARAM;
import static org.veo.rest.ControllerConstants.CHILD_ELEMENT_IDS_PARAM;
import static org.veo.rest.ControllerConstants.DESCRIPTION_PARAM;
import static org.veo.rest.ControllerConstants.DESIGNATOR_PARAM;
import static org.veo.rest.ControllerConstants.DISPLAY_NAME_PARAM;
import static org.veo.rest.ControllerConstants.HAS_CHILD_ELEMENTS_PARAM;
import static org.veo.rest.ControllerConstants.HAS_PARENT_ELEMENTS_PARAM;
import static org.veo.rest.ControllerConstants.IF_MATCH_HEADER;
import static org.veo.rest.ControllerConstants.IF_MATCH_HEADER_NOT_BLANK_MESSAGE;
import static org.veo.rest.ControllerConstants.NAME_PARAM;
import static org.veo.rest.ControllerConstants.PAGE_NUMBER_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.PAGE_NUMBER_PARAM;
import static org.veo.rest.ControllerConstants.PAGE_SIZE_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.PAGE_SIZE_PARAM;
import static org.veo.rest.ControllerConstants.SCOPE_IDS_DESCRIPTION;
import static org.veo.rest.ControllerConstants.SCOPE_IDS_PARAM;
import static org.veo.rest.ControllerConstants.SORT_COLUMN_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.SORT_COLUMN_PARAM;
import static org.veo.rest.ControllerConstants.SORT_ORDER_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.SORT_ORDER_PARAM;
import static org.veo.rest.ControllerConstants.SORT_ORDER_PATTERN;
import static org.veo.rest.ControllerConstants.STATUS_PARAM;
import static org.veo.rest.ControllerConstants.SUB_TYPE_PARAM;
import static org.veo.rest.ControllerConstants.UNIT_PARAM;
import static org.veo.rest.ControllerConstants.UPDATED_BY_PARAM;
import static org.veo.rest.ControllerConstants.UUID_DESCRIPTION;
import static org.veo.rest.ControllerConstants.UUID_EXAMPLE;
import static org.veo.rest.ControllerConstants.UUID_PARAM;
import static org.veo.rest.ControllerConstants.UUID_PARAM_SPEC;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.LinkMapDto;
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.dto.create.CreateDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.create.CreateIncidentInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullIncidentInDomainDto;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.adapter.presenter.api.io.mapper.QueryInputMapper;
import org.veo.adapter.presenter.api.response.InOrOutboundLinkDto;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.Incident;
import org.veo.core.repository.LinkQuery;
import org.veo.core.usecase.base.CreateElementUseCase;
import org.veo.core.usecase.base.UpdateIncidentInDomainUseCase;
import org.veo.core.usecase.decision.EvaluateElementUseCase;
import org.veo.core.usecase.incident.GetIncidentUseCase;
import org.veo.rest.annotations.UnitUuidParam;
import org.veo.rest.common.ClientLookup;
import org.veo.rest.common.ElementInDomainService;
import org.veo.rest.schemas.EvaluateElementOutputSchema;
import org.veo.rest.security.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** REST service which provides methods to manage incidents from the viewpoint of a domain. */
@RequiredArgsConstructor
@RestController
@RequestMapping(IncidentInDomainController.URL_BASE_PATH)
@Slf4j
public class IncidentInDomainController implements ElementInDomainResource {
  public static final String URL_BASE_PATH =
      "/" + Domain.PLURAL_TERM + "/{domainId}/" + Incident.PLURAL_TERM;
  private final ClientLookup clientLookup;
  private final GetIncidentUseCase getIncidentUseCase;
  private final CreateElementUseCase<Incident> createUseCase;
  private final UpdateIncidentInDomainUseCase updateUseCase;
  private final ElementInDomainService elementService;
  private final EntityToDtoTransformer entityToDtoTransformer;

  @GetMapping(UUID_PARAM_SPEC)
  @Operation(summary = "Loads an incident from the viewpoint of a domain")
  @ApiResponse(
      responseCode = "200",
      description = "Incident loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = FullIncidentInDomainDto.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Incident or domain not found or incident not associated with domain")
  public @Valid Future<ResponseEntity<FullIncidentInDomainDto>> getElement(
      @Parameter(required = true, hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      WebRequest request) {
    return elementService.getElement(
        auth,
        domainId,
        uuid,
        request,
        Incident.class,
        getIncidentUseCase,
        entityToDtoTransformer::transformIncident2Dto);
  }

  @GetMapping
  @Operation(summary = "Loads all incidents in a domain")
  public @Valid Future<PageDto<FullIncidentInDomainDto>> getIncidents(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @UnitUuidParam @RequestParam(value = UNIT_PARAM, required = false) UUID unitUuid,
      @RequestParam(value = DISPLAY_NAME_PARAM, required = false) String displayName,
      @RequestParam(value = ABBREVIATION_PARAM, required = false) String abbreviation,
      @RequestParam(value = SUB_TYPE_PARAM, required = false) String subType,
      @RequestParam(value = STATUS_PARAM, required = false) String status,
      @RequestParam(value = CHILD_ELEMENT_IDS_PARAM, required = false) List<UUID> childElementIds,
      @RequestParam(value = HAS_PARENT_ELEMENTS_PARAM, required = false) Boolean hasParentElements,
      @RequestParam(value = HAS_CHILD_ELEMENTS_PARAM, required = false) Boolean hasChildElements,
      @RequestParam(value = DESCRIPTION_PARAM, required = false) String description,
      @RequestParam(value = DESIGNATOR_PARAM, required = false) String designator,
      @RequestParam(value = NAME_PARAM, required = false) String name,
      @RequestParam(value = UPDATED_BY_PARAM, required = false) String updatedBy,
      @RequestParam(
              value = PAGE_SIZE_PARAM,
              required = false,
              defaultValue = PAGE_SIZE_DEFAULT_VALUE)
          @Min(1)
          Integer pageSize,
      @RequestParam(
              value = PAGE_NUMBER_PARAM,
              required = false,
              defaultValue = PAGE_NUMBER_DEFAULT_VALUE)
          Integer pageNumber,
      @RequestParam(
              value = SORT_COLUMN_PARAM,
              required = false,
              defaultValue = SORT_COLUMN_DEFAULT_VALUE)
          String sortColumn,
      @RequestParam(
              value = SORT_ORDER_PARAM,
              required = false,
              defaultValue = SORT_ORDER_DEFAULT_VALUE)
          @Pattern(regexp = SORT_ORDER_PATTERN)
          String sortOrder) {
    return elementService.getElements(
        domainId,
        QueryInputMapper.map(
            clientLookup.getClient(auth),
            unitUuid,
            domainId,
            displayName,
            subType,
            status,
            childElementIds,
            hasChildElements,
            hasParentElements,
            null,
            null,
            description,
            designator,
            name,
            abbreviation,
            updatedBy,
            PagingMapper.toConfig(pageSize, pageNumber, sortColumn, sortOrder)),
        entityToDtoTransformer::transformIncident2Dto,
        ElementType.INCIDENT);
  }

  @Operation(summary = "Loads the parts of an incident in a domain")
  @ApiResponse(
      responseCode = "200",
      description = "Parts loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array =
                  @ArraySchema(schema = @Schema(implementation = FullIncidentInDomainDto.class))))
  @ApiResponse(responseCode = "404", description = "Incident not found")
  @GetMapping(value = "/{" + UUID_PARAM + "}/parts")
  public @Valid Future<PageDto<FullIncidentInDomainDto>> getElementParts(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      @RequestParam(
              value = PAGE_SIZE_PARAM,
              required = false,
              defaultValue = PAGE_SIZE_DEFAULT_VALUE)
          @Min(1)
          Integer pageSize,
      @RequestParam(
              value = PAGE_NUMBER_PARAM,
              required = false,
              defaultValue = PAGE_NUMBER_DEFAULT_VALUE)
          Integer pageNumber,
      @RequestParam(
              value = SORT_COLUMN_PARAM,
              required = false,
              defaultValue = SORT_COLUMN_DEFAULT_VALUE)
          String sortColumn,
      @RequestParam(
              value = SORT_ORDER_PARAM,
              required = false,
              defaultValue = SORT_ORDER_DEFAULT_VALUE)
          @Pattern(regexp = SORT_ORDER_PATTERN)
          String sortOrder,
      WebRequest request) {
    var client = clientLookup.getClient(auth);
    elementService.ensureElementExists(client, domainId, uuid, getIncidentUseCase);
    return elementService.getElements(
        domainId,
        QueryInputMapper.map(
            client,
            null,
            domainId,
            null,
            null,
            null,
            null,
            null,
            null,
            uuid,
            null,
            null,
            null,
            null,
            null,
            null,
            PagingMapper.toConfig(pageSize, pageNumber, sortColumn, sortOrder)),
        entityToDtoTransformer::transformIncident2Dto,
        ElementType.INCIDENT);
  }

  @Operation(summary = "Creates an incident, assigning it to the domain")
  @PostMapping
  @ApiResponse(
      responseCode = "201",
      description = "Incident created",
      headers = @Header(name = "Location"))
  @ApiResponse(responseCode = "404", description = "Domain not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createElement(
      @Parameter(required = true, hidden = true) ApplicationUser user,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Valid @NotNull @RequestBody CreateIncidentInDomainDto dto,
      @Parameter(description = SCOPE_IDS_DESCRIPTION)
          @RequestParam(name = SCOPE_IDS_PARAM, required = false)
          List<UUID> scopeIds) {
    return elementService.createElement(user, domainId, dto, scopeIds, createUseCase);
  }

  @Operation(summary = "Associates an existing incident with a domain")
  @PostMapping(UUID_PARAM_SPEC)
  @ApiResponse(responseCode = "200", description = "Incident associated with domain")
  @ApiResponse(
      responseCode = "404",
      description = "Incident or domain not found or incident already associated with domain")
  public CompletableFuture<ResponseEntity<FullIncidentInDomainDto>> associateElementWithDomain(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      @Valid @NotNull @RequestBody CreateDomainAssociationDto dto) {
    return elementService.associateElementWithDomain(
        auth, domainId, uuid, dto, Incident.class, entityToDtoTransformer::transformIncident2Dto);
  }

  @Operation(summary = "Updates a incident from the viewpoint of a domain")
  @PutMapping(UUID_PARAM_SPEC)
  @ApiResponse(responseCode = "200", description = "Incident updated")
  @ApiResponse(
      responseCode = "404",
      description = "Incident not found or incident not associated with domain")
  public CompletableFuture<ResponseEntity<FullIncidentInDomainDto>> updateElement(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @RequestHeader(IF_MATCH_HEADER) @NotBlank(message = IF_MATCH_HEADER_NOT_BLANK_MESSAGE)
          String eTag,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      @Valid @NotNull @RequestBody FullIncidentInDomainDto dto) {
    return elementService.update(
        auth,
        domainId,
        eTag,
        uuid,
        dto,
        updateUseCase,
        entityToDtoTransformer::transformIncident2Dto);
  }

  @Operation(summary = "Retrieve inbound and outbound links for an incident in a domain")
  @GetMapping(UUID_PARAM_SPEC + "/links")
  @ApiResponse(responseCode = "200", description = "Links loaded")
  @ApiResponse(
      responseCode = "404",
      description = "Incident or domain not found or incident not associated with domain")
  public CompletableFuture<ResponseEntity<PageDto<InOrOutboundLinkDto>>> getLinks(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      @RequestParam(
              value = PAGE_SIZE_PARAM,
              required = false,
              defaultValue = PAGE_SIZE_DEFAULT_VALUE)
          @Min(1)
          Integer pageSize,
      @RequestParam(
              value = PAGE_NUMBER_PARAM,
              required = false,
              defaultValue = PAGE_NUMBER_DEFAULT_VALUE)
          Integer pageNumber,
      @RequestParam(value = SORT_COLUMN_PARAM, required = false, defaultValue = "DIRECTION")
          LinkQuery.SortCriterion sortColumn,
      @RequestParam(
              value = SORT_ORDER_PARAM,
              required = false,
              defaultValue = SORT_ORDER_DEFAULT_VALUE)
          @Pattern(regexp = SORT_ORDER_PATTERN)
          String sortOrder) {
    return elementService.getLinks(
        auth, domainId, uuid, Incident.class, pageSize, pageNumber, sortColumn, sortOrder);
  }

  @Operation(summary = "Adds links to an existing incident")
  @PostMapping(UUID_PARAM_SPEC + "/links")
  @ApiResponse(responseCode = "204", description = "Links added")
  @ApiResponse(responseCode = "400", description = "Invalid link")
  @ApiResponse(
      responseCode = "404",
      description = "Incident or domain not found or incident not associated with domain")
  @ApiResponse(responseCode = "409", description = "Link already exists")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> addLinks(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      @Valid @NotNull @RequestBody LinkMapDto links) {
    return elementService.addLinks(auth, domainId, uuid, links, Incident.class);
  }

  @Operation(
      summary =
          "Evaluates decisions and inspections on a transient incident without persisting anything")
  @ApiResponse(
      responseCode = "200",
      description = "Element evaluated",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = EvaluateElementOutputSchema.class)))
  @ApiResponse(responseCode = "404", description = "Domain not found")
  @PostMapping(value = "/evaluation")
  public @Valid CompletableFuture<ResponseEntity<EvaluateElementUseCase.OutputData>> evaluate(
      @Parameter(required = true, hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Valid @RequestBody FullIncidentInDomainDto dto) {
    return elementService.evaluate(auth, dto, domainId);
  }

  @Operation(summary = "Returns domain-specific incident JSON schema")
  @Override
  public @Valid CompletableFuture<ResponseEntity<String>> getJsonSchema(
      Authentication auth, UUID domainId) {
    return elementService.getJsonSchema(auth, domainId, ElementType.INCIDENT);
  }
}
