/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Ben Nasrallah.
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
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

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

import com.github.JanLoebel.jsonschemavalidation.JsonSchemaValidation;

import org.veo.adapter.IdRefResolver;
import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.create.CreateScenarioDto;
import org.veo.adapter.presenter.api.dto.full.FullScenarioDto;
import org.veo.adapter.presenter.api.io.mapper.CreateElementInputMapper;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.adapter.presenter.api.io.mapper.GetElementsInputMapper;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.inspection.Finding;
import org.veo.core.usecase.InspectElementUseCase;
import org.veo.core.usecase.base.CreateElementUseCase;
import org.veo.core.usecase.base.DeleteElementUseCase;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.base.ModifyElementUseCase.InputData;
import org.veo.core.usecase.decision.EvaluateElementUseCase;
import org.veo.core.usecase.scenario.CreateScenarioUseCase;
import org.veo.core.usecase.scenario.GetScenarioUseCase;
import org.veo.core.usecase.scenario.GetScenariosUseCase;
import org.veo.core.usecase.scenario.UpdateScenarioUseCase;
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

/** REST service which provides methods to manage scenarios. */
@RestController
@RequestMapping(ScenarioController.URL_BASE_PATH)
@Slf4j
public class ScenarioController extends AbstractElementController<Scenario, FullScenarioDto> {

  public ScenarioController(
      GetScenarioUseCase getScenarioUseCase,
      GetScenariosUseCase getScenariosUseCase,
      CreateScenarioUseCase createScenarioUseCase,
      UpdateScenarioUseCase updateScenarioUseCase,
      DeleteElementUseCase deleteElementUseCase,
      EvaluateElementUseCase evaluateElementUseCase,
      InspectElementUseCase inspectElementUseCase) {
    super(Scenario.class, getScenarioUseCase, evaluateElementUseCase, inspectElementUseCase);
    this.getScenariosUseCase = getScenariosUseCase;
    this.createScenarioUseCase = createScenarioUseCase;
    this.updateScenarioUseCase = updateScenarioUseCase;
    this.deleteElementUseCase = deleteElementUseCase;
  }

  public static final String URL_BASE_PATH = "/" + Scenario.PLURAL_TERM;

  private final CreateScenarioUseCase createScenarioUseCase;
  private final UpdateScenarioUseCase updateScenarioUseCase;
  private final GetScenariosUseCase getScenariosUseCase;
  private final DeleteElementUseCase deleteElementUseCase;

  @GetMapping
  @Operation(summary = "Loads all scenarios")
  public @Valid Future<PageDto<FullScenarioDto>> getScenarios(
      @Parameter(required = false, hidden = true) Authentication auth,
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
      @RequestParam(value = UPDATED_BY_PARAM, required = false) String updatedBy,
      @RequestParam(
              value = PAGE_SIZE_PARAM,
              required = false,
              defaultValue = PAGE_SIZE_DEFAULT_VALUE)
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
    Client client = null;
    try {
      client = getAuthenticatedClient(auth);
    } catch (NoSuchElementException e) {
      return CompletableFuture.supplyAsync(PageDto::emptyPage);
    }

    return getScenarios(
        GetElementsInputMapper.map(
            client,
            unitUuid,
            displayName,
            subType,
            status,
            childElementIds,
            hasChildElements,
            hasParentElements,
            description,
            designator,
            name,
            updatedBy,
            PagingMapper.toConfig(pageSize, pageNumber, sortColumn, sortOrder)));
  }

  private CompletableFuture<PageDto<FullScenarioDto>> getScenarios(
      GetElementsUseCase.InputData inputData) {
    return useCaseInteractor.execute(
        getScenariosUseCase,
        inputData,
        output ->
            PagingMapper.toPage(
                output.getElements(), entityToDtoTransformer::transformScenario2Dto));
  }

  @Override
  @Operation(summary = "Loads a scenario")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Scenario loaded",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = FullScenarioDto.class))),
        @ApiResponse(responseCode = "404", description = "Scenario not found")
      })
  @GetMapping(ControllerConstants.UUID_PARAM_SPEC)
  public @Valid Future<ResponseEntity<FullScenarioDto>> getElement(
      @Parameter(required = false, hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String uuid,
      WebRequest request) {
    return super.getElement(auth, uuid, request);
  }

  @Override
  @Operation(summary = "Loads the parts of a scenario")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Parts loaded",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array =
                        @ArraySchema(schema = @Schema(implementation = FullScenarioDto.class)))),
        @ApiResponse(responseCode = "404", description = "Scenario not found")
      })
  @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}/parts")
  public @Valid CompletableFuture<ResponseEntity<List<FullScenarioDto>>> getElementParts(
      @Parameter(required = false, hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String uuid,
      WebRequest request) {
    return super.getElementParts(auth, uuid, request);
  }

  @PostMapping()
  @Operation(summary = "Creates a scenario")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Scenario created")})
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createScenario(
      @Parameter(hidden = true) ApplicationUser user,
      @Valid @NotNull @RequestBody @JsonSchemaValidation(Scenario.SINGULAR_TERM)
          CreateScenarioDto dto,
      @Parameter(description = SCOPE_IDS_DESCRIPTION)
          @RequestParam(name = SCOPE_IDS_PARAM, required = false)
          List<String> scopeIds) {
    return useCaseInteractor.execute(
        createScenarioUseCase,
        (Supplier<CreateElementUseCase.InputData<Scenario>>)
            () -> {
              Client client = getClient(user);
              IdRefResolver idRefResolver = createIdRefResolver(client);
              return CreateElementInputMapper.map(
                  dtoToEntityTransformer.transformDto2Scenario(dto, idRefResolver),
                  client,
                  scopeIds);
            },
        output -> {
          ApiResponseBody body = CreateOutputMapper.map(output.getEntity());
          return RestApiResponse.created(URL_BASE_PATH, body);
        });
  }

  @PutMapping(value = "/{id}")
  @Operation(summary = "Updates a scenario")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Scenario updated"),
        @ApiResponse(responseCode = "404", description = "Scenario not found")
      })
  public CompletableFuture<FullScenarioDto> updateScenario(
      @Parameter(hidden = true) ApplicationUser user,
      @RequestHeader(ControllerConstants.IF_MATCH_HEADER) @NotBlank String eTag,
      @PathVariable String id,
      @Valid @NotNull @RequestBody @JsonSchemaValidation(Scenario.SINGULAR_TERM)
          FullScenarioDto scenarioDto) {
    scenarioDto.applyResourceId(id);
    return useCaseInteractor.execute(
        updateScenarioUseCase,
        new Supplier<InputData<Scenario>>() {
          @Override
          public InputData<Scenario> get() {
            Client client = getClient(user);
            IdRefResolver idRefResolver = createIdRefResolver(client);
            return new InputData<>(
                dtoToEntityTransformer.transformDto2Scenario(scenarioDto, idRefResolver),
                client,
                eTag,
                user.getUsername());
          }
        },
        output -> entityToDtoTransformer.transformScenario2Dto(output.getEntity()));
  }

  @DeleteMapping(ControllerConstants.UUID_PARAM_SPEC)
  @Operation(summary = "Deletes a scenario")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Scenario deleted"),
        @ApiResponse(responseCode = "404", description = "Scenario not found")
      })
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteScenario(
      @Parameter(required = false, hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String uuid) {
    ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
    Client client = getClient(user.getClientId());
    return useCaseInteractor.execute(
        deleteElementUseCase,
        new DeleteElementUseCase.InputData(Scenario.class, Key.uuidFrom(uuid), client),
        output -> ResponseEntity.noContent().build());
  }

  @Override
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
  protected String buildSearchUri(String id) {
    return linkTo(
            methodOn(ScenarioController.class)
                .runSearch(ANY_AUTH, id, ANY_INT, ANY_INT, ANY_STRING, ANY_STRING))
        .withSelfRel()
        .getHref();
  }

  @GetMapping(value = "/searches/{searchId}")
  @Operation(summary = "Finds scenarios for the search.")
  public @Valid Future<PageDto<FullScenarioDto>> runSearch(
      @Parameter(required = false, hidden = true) Authentication auth,
      @PathVariable String searchId,
      @RequestParam(
              value = PAGE_SIZE_PARAM,
              required = false,
              defaultValue = PAGE_SIZE_DEFAULT_VALUE)
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
      return getScenarios(
          GetElementsInputMapper.map(
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
          "Evaluates decisions and inspections on a transient scenario without persisting anything")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Element evaluated",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = EvaluateElementOutputSchema.class)))
      })
  @PostMapping(value = "/evaluation")
  public @Valid CompletableFuture<ResponseEntity<EvaluateElementUseCase.OutputData>> evaluate(
      @Parameter(required = true, hidden = true) Authentication auth,
      @Valid @RequestBody FullScenarioDto element,
      @RequestParam(value = DOMAIN_PARAM) String domainId) {
    return super.evaluate(auth, element, domainId);
  }

  @Operation(summary = "Runs inspections on a persisted scenario")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Inspections have run",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = Finding.class)))),
        @ApiResponse(responseCode = "404", description = "Scenario not found")
      })
  @GetMapping(value = UUID_PARAM_SPEC + "/inspection")
  public @Valid CompletableFuture<ResponseEntity<Set<Finding>>> inspect(
      @Parameter(required = true, hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String uuid,
      @RequestParam(value = DOMAIN_PARAM) String domainId) {
    return inspect(auth, uuid, domainId, Scenario.class);
  }

  @Override
  protected FullScenarioDto entity2Dto(Scenario entity) {
    return entityToDtoTransformer.transformScenario2Dto(entity);
  }
}
