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
import java.util.Locale;
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

import org.veo.adapter.persistence.schema.RelationGraphService;
import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.AbstractElementInDomainDto;
import org.veo.adapter.presenter.api.dto.GraphResultDto;
import org.veo.adapter.presenter.api.dto.LinkMapDto;
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.dto.create.CreateDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.create.CreatePersonInDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonInDomainDto;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.adapter.presenter.api.io.mapper.QueryInputMapper;
import org.veo.adapter.presenter.api.response.InOrOutboundLinkDto;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.Person;
import org.veo.core.repository.LinkQuery;
import org.veo.core.repository.ParentElementQuery;
import org.veo.core.usecase.base.CreateElementUseCase;
import org.veo.core.usecase.base.UpdatePersonInDomainUseCase;
import org.veo.core.usecase.decision.EvaluateElementUseCase;
import org.veo.core.usecase.person.GetPersonUseCase;
import org.veo.rest.annotations.UnitUuidParam;
import org.veo.rest.common.ElementInDomainService;
import org.veo.rest.schemas.EvaluateElementOutputSchema;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

/** REST service which provides methods to manage persons from the viewpoint of a domain. */
@RequiredArgsConstructor
@RestController
@RequestMapping(PersonInDomainController.URL_BASE_PATH)
public class PersonInDomainController implements ElementInDomainResource {
  public static final String URL_BASE_PATH =
      "/" + Domain.PLURAL_TERM + "/{domainId}/" + Person.PLURAL_TERM;
  private final GetPersonUseCase getPersonUseCase;
  private final CreateElementUseCase<Person> createUseCase;
  private final UpdatePersonInDomainUseCase updateUseCase;
  private final ElementInDomainService elementService;
  private final EntityToDtoTransformer entityToDtoTransformer;
  private final RelationGraphService relationGraphService;

  @Operation(summary = "Loads a person from the viewpoint of a domain")
  @ApiResponse(
      responseCode = "200",
      description = "Person loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = FullPersonInDomainDto.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Person or domain not found or person not associated with domain")
  @GetMapping(UUID_PARAM_SPEC)
  public @Valid Future<ResponseEntity<FullPersonInDomainDto>> getElement(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      WebRequest request) {
    return elementService.getElement(
        domainId,
        uuid,
        request,
        Person.class,
        getPersonUseCase,
        entityToDtoTransformer::transformPerson2Dto);
  }

  @GetMapping
  @Operation(summary = "Loads all persons in a domain")
  public @Valid Future<PageDto<FullPersonInDomainDto>> getPersons(
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
        entityToDtoTransformer::transformPerson2Dto,
        ElementType.PERSON);
  }

  @Operation(summary = "Loads the parents (scopes and composites) of a person in a domain")
  @ApiResponse(
      responseCode = "200",
      description = "Parents loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array =
                  @ArraySchema(
                      schema = @Schema(implementation = AbstractElementInDomainDto.class))))
  @ApiResponse(responseCode = "404", description = "Person or domain not found")
  @GetMapping(value = "/{" + UUID_PARAM + "}/parents")
  public @Valid Future<PageDto<AbstractElementInDomainDto<Element>>> getParents(
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
      @RequestParam(value = SORT_COLUMN_PARAM, required = false, defaultValue = "NAME")
          ParentElementQuery.SortCriterion sortColumn,
      @RequestParam(
              value = SORT_ORDER_PARAM,
              required = false,
              defaultValue = SORT_ORDER_DEFAULT_VALUE)
          @Pattern(regexp = SORT_ORDER_PATTERN)
          String sortOrder) {
    return elementService.getParents(
        domainId,
        uuid,
        ElementType.PERSON,
        entityToDtoTransformer::transformElement2Dto,
        pageSize,
        pageNumber,
        sortColumn,
        sortOrder);
  }

  @Operation(summary = "Loads the parts of a person in a domain")
  @ApiResponse(
      responseCode = "200",
      description = "Parts loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = FullPersonInDomainDto.class))))
  @ApiResponse(responseCode = "404", description = "Person not found")
  @GetMapping(value = "/{" + UUID_PARAM + "}/parts")
  public @Valid Future<PageDto<FullPersonInDomainDto>> getElementParts(
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
          String sortOrder) {
    elementService.ensureElementExists(domainId, uuid, getPersonUseCase);
    return elementService.getElements(
        domainId,
        QueryInputMapper.map(
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
        entityToDtoTransformer::transformPerson2Dto,
        ElementType.PERSON);
  }

  @Operation(summary = "Creates a person, assigning it to the domain")
  @PostMapping
  @ApiResponse(
      responseCode = "201",
      description = "Person created",
      headers = @Header(name = "Location"))
  @ApiResponse(responseCode = "404", description = "Domain not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createElement(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Valid @NotNull @RequestBody CreatePersonInDomainDto dto,
      @Parameter(description = SCOPE_IDS_DESCRIPTION)
          @RequestParam(name = SCOPE_IDS_PARAM, required = false)
          List<UUID> scopeIds) {
    return elementService.createElement(domainId, dto, scopeIds, createUseCase);
  }

  @Operation(summary = "Associates an existing person with a domain")
  @PostMapping(UUID_PARAM_SPEC)
  @ApiResponse(responseCode = "200", description = "Person associated with domain")
  @ApiResponse(
      responseCode = "404",
      description = "Person or domain not found or person already associated with domain")
  public CompletableFuture<ResponseEntity<FullPersonInDomainDto>> associateElementWithDomain(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      @Valid @NotNull @RequestBody CreateDomainAssociationDto dto) {
    return elementService.associateElementWithDomain(
        domainId, uuid, dto, Person.class, entityToDtoTransformer::transformPerson2Dto);
  }

  @Operation(summary = "Updates a person from the viewpoint of a domain")
  @PutMapping(UUID_PARAM_SPEC)
  @ApiResponse(responseCode = "200", description = "Person updated")
  @ApiResponse(
      responseCode = "404",
      description = "Person not found or person not associated with domain")
  public CompletableFuture<ResponseEntity<FullPersonInDomainDto>> updateElement(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @RequestHeader(IF_MATCH_HEADER) @NotBlank(message = IF_MATCH_HEADER_NOT_BLANK_MESSAGE)
          String eTag,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      @Valid @NotNull @RequestBody FullPersonInDomainDto dto) {
    return elementService.update(
        domainId, eTag, uuid, dto, updateUseCase, entityToDtoTransformer::transformPerson2Dto);
  }

  @Operation(summary = "Retrieve inbound and outbound links for a person in a domain")
  @GetMapping(UUID_PARAM_SPEC + "/links")
  @ApiResponse(responseCode = "200", description = "Links loaded")
  @ApiResponse(
      responseCode = "404",
      description = "Person or domain not found or person not associated with domain")
  public CompletableFuture<ResponseEntity<PageDto<InOrOutboundLinkDto>>> getLinks(
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
        domainId, uuid, Person.class, pageSize, pageNumber, sortColumn, sortOrder);
  }

  @Operation(summary = "Adds links to an existing person")
  @PostMapping(UUID_PARAM_SPEC + "/links")
  @ApiResponse(responseCode = "204", description = "Links added")
  @ApiResponse(responseCode = "400", description = "Invalid link")
  @ApiResponse(
      responseCode = "404",
      description = "Person or domain not found or person not associated with domain")
  @ApiResponse(responseCode = "409", description = "Link already exists")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> addLinks(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      @Valid @NotNull @RequestBody LinkMapDto links) {
    return elementService.addLinks(domainId, uuid, links, Person.class);
  }

  @Operation(
      summary =
          "Evaluates decisions and inspections on a transient person without persisting anything")
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
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Valid @RequestBody FullPersonInDomainDto dto) {
    return elementService.evaluate(dto, domainId);
  }

  @Operation(summary = "Returns domain-specific person JSON schema")
  @Override
  public @Valid CompletableFuture<ResponseEntity<String>> getJsonSchema(UUID domainId) {
    return elementService.getJsonSchema(domainId, ElementType.PERSON);
  }

  @GetMapping("/{personId}/relations")
  @Operation(
      summary =
          "EXPERIMENTAL API - Returns the direct relations of a person for graph visualization")
  public GraphResultDto getPersonGraph(
      @PathVariable UUID domainId, @PathVariable UUID personId, Locale locale) {
    return relationGraphService.getGraph(personId, domainId, ElementType.PERSON, locale);
  }
}
