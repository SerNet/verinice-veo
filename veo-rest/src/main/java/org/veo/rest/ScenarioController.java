/*******************************************************************************
 * Copyright (c) 2020 Alexander Ben Nasrallah.
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
package org.veo.rest;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.veo.rest.ControllerConstants.ANY_AUTH;
import static org.veo.rest.ControllerConstants.DISPLAY_NAME_PARAM;
import static org.veo.rest.ControllerConstants.SUB_TYPE_PARAM;
import static org.veo.rest.ControllerConstants.UNIT_PARAM;
import static org.veo.rest.ControllerConstants.UUID_PARAM;
import static org.veo.rest.ControllerConstants.UUID_REGEX;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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

import org.veo.adapter.ModelObjectReferenceResolver;
import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.EntityLayerSupertypeDto;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.create.CreateScenarioDto;
import org.veo.adapter.presenter.api.dto.full.FullScenarioDto;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.adapter.presenter.api.io.mapper.GetEntitiesInputMapper;
import org.veo.core.entity.Client;
import org.veo.core.entity.EntityTypeNames;
import org.veo.core.entity.Key;
import org.veo.core.entity.Scenario;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.core.usecase.base.CreateEntityUseCase;
import org.veo.core.usecase.base.DeleteEntityUseCase;
import org.veo.core.usecase.base.GetEntitiesUseCase;
import org.veo.core.usecase.base.ModifyEntityUseCase.InputData;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.scenario.CreateScenarioUseCase;
import org.veo.core.usecase.scenario.GetScenarioUseCase;
import org.veo.core.usecase.scenario.GetScenariosUseCase;
import org.veo.core.usecase.scenario.UpdateScenarioUseCase;
import org.veo.rest.annotations.ParameterUuid;
import org.veo.rest.annotations.UnitUuidParam;
import org.veo.rest.common.RestApiResponse;
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

/**
 * REST service which provides methods to manage scenarios.
 */
@RestController
@RequestMapping(ScenarioController.URL_BASE_PATH)
@Slf4j
public class ScenarioController extends AbstractEntityController {

    public ScenarioController(UseCaseInteractor useCaseInteractor,
            GetScenarioUseCase getScenarioUseCase, GetScenariosUseCase getScenariosUseCase,
            CreateScenarioUseCase createScenarioUseCase,
            UpdateScenarioUseCase updateScenarioUseCase, DeleteEntityUseCase deleteEntityUseCase) {
        this.useCaseInteractor = useCaseInteractor;
        this.getScenarioUseCase = getScenarioUseCase;
        this.getScenariosUseCase = getScenariosUseCase;
        this.createScenarioUseCase = createScenarioUseCase;
        this.updateScenarioUseCase = updateScenarioUseCase;
        this.deleteEntityUseCase = deleteEntityUseCase;
    }

    public static final String URL_BASE_PATH = "/" + EntityTypeNames.SCENARIOS;

    private final UseCaseInteractor useCaseInteractor;
    private final CreateScenarioUseCase createScenarioUseCase;
    private final UpdateScenarioUseCase updateScenarioUseCase;
    private final GetScenarioUseCase getScenarioUseCase;
    private final GetScenariosUseCase getScenariosUseCase;
    private final DeleteEntityUseCase deleteEntityUseCase;

    @GetMapping
    @Operation(summary = "Loads all scenarios")
    public @Valid CompletableFuture<List<FullScenarioDto>> getScenarios(
            @Parameter(required = false, hidden = true) Authentication auth,
            @UnitUuidParam @RequestParam(value = UNIT_PARAM, required = false) String unitUuid,
            @UnitUuidParam @RequestParam(value = DISPLAY_NAME_PARAM,
                                         required = false) String displayName,
            @RequestParam(value = SUB_TYPE_PARAM, required = false) String subType) {
        Client client = null;
        try {
            client = getAuthenticatedClient(auth);
        } catch (NoSuchElementException e) {
            return CompletableFuture.supplyAsync(Collections::emptyList);
        }

        return getScenarios(GetEntitiesInputMapper.map(client, unitUuid, displayName, subType));
    }

    private CompletableFuture<List<FullScenarioDto>> getScenarios(
            GetEntitiesUseCase.InputData inputData) {
        return useCaseInteractor.execute(getScenariosUseCase, inputData,
                                         output -> output.getEntities()
                                                         .stream()
                                                         .map(a -> entityToDtoTransformer.transformScenario2Dto(a))
                                                         .collect(Collectors.toList()));
    }

    @GetMapping(value = "/{id}")
    @Operation(summary = "Loads an scenario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Scenario loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = FullScenarioDto.class))),
            @ApiResponse(responseCode = "404", description = "Scenario not found") })
    public @Valid CompletableFuture<ResponseEntity<FullScenarioDto>> getScenario(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String id) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        Client client = getClient(user.getClientId());

        CompletableFuture<FullScenarioDto> scenarioFuture = useCaseInteractor.execute(getScenarioUseCase,
                                                                                      new GetScenarioUseCase.InputData(
                                                                                              Key.uuidFrom(id),
                                                                                              client),
                                                                                      output -> {
                                                                                          return entityToDtoTransformer.transformScenario2Dto(output.getScenario());
                                                                                      });

        return scenarioFuture.thenApply(scenarioDto -> ResponseEntity.ok()
                                                                     .eTag(ETag.from(scenarioDto.getId(),
                                                                                     scenarioDto.getVersion()))
                                                                     .body(scenarioDto));
    }

    @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}/parts")
    @Operation(summary = "Loads the parts of a scenario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Parts loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = FullScenarioDto.class)))),
            @ApiResponse(responseCode = "404", description = "Scenario not found") })
    public @Valid CompletableFuture<List<EntityLayerSupertypeDto>> getParts(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        Client client = getAuthenticatedClient(auth);
        return useCaseInteractor.execute(getScenarioUseCase, new GetScenarioUseCase.InputData(
                Key.uuidFrom(uuid), client), output -> {
                    Scenario scope = output.getScenario();
                    return scope.getParts()
                                .stream()
                                .map(part -> entityToDtoTransformer.transform2Dto(part))
                                .collect(Collectors.toList());
                });
    }

    @PostMapping()
    @Operation(summary = "Creates an scenario")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Scenario created") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> createScenario(
            @Parameter(hidden = true) ApplicationUser user,
            @Valid @NotNull @RequestBody CreateScenarioDto dto) {
        return useCaseInteractor.execute(createScenarioUseCase,
                                         (Supplier<CreateEntityUseCase.InputData<Scenario>>) () -> {
                                             Client client = getClient(user);
                                             ModelObjectReferenceResolver modelObjectReferenceResolver = createModelObjectReferenceResolver(client);
                                             return new CreateEntityUseCase.InputData<>(
                                                     dtoToEntityTransformer.transformDto2Scenario(dto,
                                                                                                  null,
                                                                                                  modelObjectReferenceResolver),
                                                     client, user.getUsername());
                                         }, output -> {
                                             ApiResponseBody body = CreateOutputMapper.map(output.getEntity());
                                             return RestApiResponse.created(URL_BASE_PATH, body);
                                         });
    }

    @PutMapping(value = "/{id}")
    @Operation(summary = "Updates an scenario")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Scenario updated"),
            @ApiResponse(responseCode = "404", description = "Scenario not found") })
    public CompletableFuture<FullScenarioDto> updateScenario(
            @Parameter(hidden = true) ApplicationUser user,
            @RequestHeader(ControllerConstants.IF_MATCH_HEADER) @NotBlank String eTag,
            @PathVariable String id, @Valid @NotNull @RequestBody FullScenarioDto scenarioDto) {
        applyId(id, scenarioDto);
        return useCaseInteractor.execute(updateScenarioUseCase,
                                         new Supplier<InputData<Scenario>>() {

                                             @Override
                                             public InputData<Scenario> get() {
                                                 Client client = getClient(user);
                                                 ModelObjectReferenceResolver modelObjectReferenceResolver = createModelObjectReferenceResolver(client);
                                                 return new InputData<Scenario>(
                                                         dtoToEntityTransformer.transformDto2Scenario(scenarioDto,
                                                                                                      Key.uuidFrom(scenarioDto.getId()),
                                                                                                      modelObjectReferenceResolver),
                                                         client, eTag, user.getUsername());
                                             }
                                         }

                                         ,
                                         output -> entityToDtoTransformer.transformScenario2Dto(output.getEntity()));
    }

    @DeleteMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Deletes an scenario")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Scenario deleted"),
            @ApiResponse(responseCode = "404", description = "Scenario not found") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteScenario(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        Client client = getClient(user.getClientId());
        return useCaseInteractor.execute(deleteEntityUseCase,
                                         new DeleteEntityUseCase.InputData(Scenario.class,
                                                 Key.uuidFrom(uuid), client),
                                         output -> ResponseEntity.ok()
                                                                 .build());
    }

    @Override
    @SuppressFBWarnings // ignore warning on call to method proxy factory
    protected String buildSearchUri(String id) {
        return linkTo(methodOn(ScenarioController.class).runSearch(ANY_AUTH, id)).withSelfRel()
                                                                                 .getHref();
    }

    @GetMapping(value = "/searches/{searchId}")
    @Operation(summary = "Finds scenarios for the search.")
    public @Valid CompletableFuture<List<FullScenarioDto>> runSearch(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String searchId) {
        try {
            return getScenarios(GetEntitiesInputMapper.map(getAuthenticatedClient(auth),
                                                           SearchQueryDto.decodeFromSearchId(searchId)));
        } catch (IOException e) {
            log.error(String.format("Could not decode search URL: %s", e.getLocalizedMessage()));
            return null;
        }
    }
}
