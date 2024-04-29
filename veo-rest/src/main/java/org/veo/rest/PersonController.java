/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.veo.rest.ControllerConstants.ABBREVIATION_PARAM;
import static org.veo.rest.ControllerConstants.ANY_AUTH;
import static org.veo.rest.ControllerConstants.ANY_INT;
import static org.veo.rest.ControllerConstants.ANY_STRING;
import static org.veo.rest.ControllerConstants.CHILD_ELEMENT_IDS_PARAM;
import static org.veo.rest.ControllerConstants.DESCRIPTION_PARAM;
import static org.veo.rest.ControllerConstants.DESIGNATOR_PARAM;
import static org.veo.rest.ControllerConstants.DISPLAY_NAME_PARAM;
import static org.veo.rest.ControllerConstants.DOMAIN_PARAM;
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
import static org.veo.rest.ControllerConstants.UUID_REGEX;

import java.io.IOException;
import java.util.List;
import java.util.Set;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.create.CreatePersonDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonDto;
import org.veo.adapter.presenter.api.io.mapper.CreateElementInputMapper;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.adapter.presenter.api.io.mapper.QueryInputMapper;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Person;
import org.veo.core.entity.inspection.Finding;
import org.veo.core.usecase.InspectElementUseCase;
import org.veo.core.usecase.base.CreateElementUseCase;
import org.veo.core.usecase.base.DeleteElementUseCase;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.base.ModifyElementUseCase;
import org.veo.core.usecase.decision.EvaluateElementUseCase;
import org.veo.core.usecase.person.GetPersonUseCase;
import org.veo.core.usecase.person.UpdatePersonUseCase;
import org.veo.rest.annotations.UnitUuidParam;
import org.veo.rest.common.RestApiResponse;
import org.veo.rest.schemas.EvaluateElementOutputSchema;
import org.veo.rest.security.ApplicationUser;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

/** REST service which provides methods to manage persons. */
@RestController
@RequestMapping(PersonController.URL_BASE_PATH)
@Slf4j
public class PersonController extends AbstractCompositeElementController<Person, FullPersonDto> {

  public static final String URL_BASE_PATH = "/" + Person.PLURAL_TERM;

  private final CreateElementUseCase<Person> createPersonUseCase;
  private final UpdatePersonUseCase updatePersonUseCase;
  private final DeleteElementUseCase deleteElementUseCase;

  public PersonController(
      CreateElementUseCase<Person> createPersonUseCase,
      GetPersonUseCase getPersonUseCase,
      GetElementsUseCase getElementsUseCase,
      UpdatePersonUseCase updatePersonUseCase,
      DeleteElementUseCase deleteElementUseCase,
      EvaluateElementUseCase evaluateElementUseCase,
      InspectElementUseCase inspectElementUseCase) {
    super(
        Person.class,
        getPersonUseCase,
        evaluateElementUseCase,
        inspectElementUseCase,
        getElementsUseCase);
    this.createPersonUseCase = createPersonUseCase;
    this.updatePersonUseCase = updatePersonUseCase;
    this.deleteElementUseCase = deleteElementUseCase;
  }

  @GetMapping
  @Operation(summary = "Loads all persons")
  public @Valid Future<PageDto<FullPersonDto>> getPersons(
      @Parameter(hidden = true) Authentication auth,
      @UnitUuidParam @RequestParam(value = UNIT_PARAM, required = false) String unitUuid,
      @RequestParam(value = DISPLAY_NAME_PARAM, required = false) String displayName,
      @RequestParam(value = SUB_TYPE_PARAM, required = false) String subType,
      @RequestParam(value = STATUS_PARAM, required = false) String status,
      @RequestParam(value = CHILD_ELEMENT_IDS_PARAM, required = false) List<String> childElementIds,
      @RequestParam(value = HAS_PARENT_ELEMENTS_PARAM, required = false) Boolean hasParentElements,
      @RequestParam(value = HAS_CHILD_ELEMENTS_PARAM, required = false) Boolean hasChildElements,
      @RequestParam(value = DESCRIPTION_PARAM, required = false) String description,
      @RequestParam(value = DESIGNATOR_PARAM, required = false) String designator,
      @RequestParam(value = NAME_PARAM, required = false) String name,
      @RequestParam(value = ABBREVIATION_PARAM, required = false) String abbreviation,
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
    Client client = getAuthenticatedClient(auth);

    return getElements(
        QueryInputMapper.map(
            client,
            unitUuid,
            null,
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
            PagingMapper.toConfig(pageSize, pageNumber, sortColumn, sortOrder)));
  }

  @Override
  @Operation(summary = "Loads a person")
  @ApiResponse(
      responseCode = "200",
      description = "Person loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = FullPersonDto.class)))
  @ApiResponse(responseCode = "404", description = "Person not found")
  @GetMapping(UUID_PARAM_SPEC)
  public Future<ResponseEntity<FullPersonDto>> getElement(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String uuid,
      WebRequest request) {
    return super.getElement(auth, uuid, request);
  }

  @Override
  @Operation(summary = "Loads the parts of a person")
  @ApiResponse(
      responseCode = "200",
      description = "Parts loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = FullPersonDto.class))))
  @ApiResponse(responseCode = "404", description = "Person not found")
  @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}/parts")
  public CompletableFuture<ResponseEntity<List<FullPersonDto>>> getElementParts(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String uuid,
      WebRequest request) {
    return super.getElementParts(auth, uuid, request);
  }

  @PostMapping()
  @Operation(summary = "Creates a person")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Person created")})
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createPerson(
      @Parameter(hidden = true) ApplicationUser user,
      @Valid @NotNull @RequestBody CreatePersonDto dto,
      @Parameter(description = SCOPE_IDS_DESCRIPTION)
          @RequestParam(name = SCOPE_IDS_PARAM, required = false)
          List<String> scopeIds) {
    return useCaseInteractor.execute(
        createPersonUseCase,
        CreateElementInputMapper.map(dto, getClient(user), scopeIds),
        output -> {
          ApiResponseBody body = CreateOutputMapper.map(output.entity());
          return RestApiResponse.created(URL_BASE_PATH, body);
        });
  }

  @PutMapping(UUID_PARAM_SPEC)
  @Operation(summary = "Updates a person")
  @ApiResponse(responseCode = "200", description = "Person updated")
  @ApiResponse(responseCode = "404", description = "Person not found")
  public CompletableFuture<ResponseEntity<FullPersonDto>> updatePerson(
      @Parameter(hidden = true) ApplicationUser user,
      @RequestHeader(IF_MATCH_HEADER) @NotBlank(message = IF_MATCH_HEADER_NOT_BLANK_MESSAGE)
          String eTag,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String uuid,
      @Valid @NotNull @RequestBody FullPersonDto personDto) {
    personDto.applyResourceId(uuid);
    return useCaseInteractor.execute(
        updatePersonUseCase,
        new ModifyElementUseCase.InputData<>(
            uuid, personDto, getClient(user), eTag, user.getUsername()),
        output -> toResponseEntity(output.entity()));
  }

  @DeleteMapping(UUID_PARAM_SPEC)
  @Operation(summary = "Deletes a person")
  @ApiResponse(responseCode = "204", description = "Person deleted")
  @ApiResponse(responseCode = "404", description = "Person not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deletePerson(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String uuid) {
    Client client = getAuthenticatedClient(auth);
    return useCaseInteractor.execute(
        deleteElementUseCase,
        new DeleteElementUseCase.InputData(Person.class, Key.uuidFrom(uuid), client),
        output -> ResponseEntity.noContent().build());
  }

  @Override
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
  protected String buildSearchUri(String id) {
    return linkTo(
            methodOn(PersonController.class)
                .runSearch(ANY_AUTH, id, ANY_INT, ANY_INT, ANY_STRING, ANY_STRING))
        .withSelfRel()
        .getHref();
  }

  @GetMapping(value = "/searches/{searchId}")
  @Operation(summary = "Finds persons for the search.")
  public @Valid Future<PageDto<FullPersonDto>> runSearch(
      @Parameter(hidden = true) Authentication auth,
      @PathVariable String searchId,
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
    try {
      return getElements(
          QueryInputMapper.map(
              getAuthenticatedClient(auth),
              SearchQueryDto.decodeFromSearchId(searchId),
              PagingMapper.toConfig(pageSize, pageNumber, sortColumn, sortOrder)));
    } catch (IOException e) {
      log.error("Could not decode search URL: {}", e.getLocalizedMessage());
      return null;
    }
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
  @PostMapping(value = "/evaluation")
  public CompletableFuture<ResponseEntity<EvaluateElementUseCase.OutputData>> evaluate(
      @Parameter(required = true, hidden = true) Authentication auth,
      @Valid @RequestBody FullPersonDto element,
      @RequestParam(value = DOMAIN_PARAM) String domainId) {
    return super.evaluate(auth, element, domainId);
  }

  @Operation(summary = "Runs inspections on a persisted person")
  @ApiResponse(
      responseCode = "200",
      description = "Inspections have run",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = Finding.class))))
  @ApiResponse(responseCode = "404", description = "Person not found")
  @GetMapping(value = UUID_PARAM_SPEC + "/inspection")
  public @Valid CompletableFuture<ResponseEntity<Set<Finding>>> inspect(
      @Parameter(required = true, hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String uuid,
      @RequestParam(value = DOMAIN_PARAM) String domainId) {
    return inspect(auth, uuid, domainId, Person.class);
  }

  @Override
  protected FullPersonDto entity2Dto(Person entity) {
    return entityToDtoTransformer.transformPerson2Dto(entity, false);
  }
}
