/*******************************************************************************
 * Copyright (c) 2019 Daniel Murygin.
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
import java.util.Optional;
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
import org.veo.adapter.presenter.api.dto.create.CreateScopeDto;
import org.veo.adapter.presenter.api.dto.full.FullScopeDto;
import org.veo.adapter.presenter.api.io.mapper.GetEntitiesInputMapper;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Scope;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.core.usecase.base.CreateEntityUseCase;
import org.veo.core.usecase.base.DeleteEntityUseCase;
import org.veo.core.usecase.base.GetEntitiesUseCase;
import org.veo.core.usecase.base.ModifyEntityUseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.scope.CreateScopeUseCase;
import org.veo.core.usecase.scope.GetScopeUseCase;
import org.veo.core.usecase.scope.GetScopesUseCase;
import org.veo.core.usecase.scope.UpdateScopeUseCase;
import org.veo.rest.annotations.ParameterUuid;
import org.veo.rest.annotations.UnitUuidParam;
import org.veo.rest.common.RestApiResponse;
import org.veo.rest.security.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

/**
 * REST service which provides methods to manage scopes.
 */
@RestController
@RequestMapping(ScopeController.URL_BASE_PATH)
@Slf4j
public class ScopeController extends AbstractEntityController {

    public static final String URL_BASE_PATH = "/" + Scope.PLURAL_TERM;

    protected static final String TYPE_PARAM = "type";

    private final UseCaseInteractor useCaseInteractor;

    private final CreateScopeUseCase createScopeUseCase;
    private final GetScopeUseCase getScopeUseCase;
    private final GetScopesUseCase getScopesUseCase;
    private final UpdateScopeUseCase updateScopeUseCase;
    private final DeleteEntityUseCase deleteEntityUseCase;

    public ScopeController(UseCaseInteractor useCaseInteractor,
            CreateScopeUseCase createScopeUseCase, GetScopeUseCase getScopeUseCase,
            GetScopesUseCase getScopesUseCase, UpdateScopeUseCase updateScopeUseCase,
            DeleteEntityUseCase deleteEntityUseCase) {
        this.useCaseInteractor = useCaseInteractor;
        this.createScopeUseCase = createScopeUseCase;
        this.getScopeUseCase = getScopeUseCase;
        this.getScopesUseCase = getScopesUseCase;
        this.updateScopeUseCase = updateScopeUseCase;
        this.deleteEntityUseCase = deleteEntityUseCase;
    }

    @GetMapping
    @Operation(summary = "Loads all scopes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Members loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = FullScopeDto.class)))),
            @ApiResponse(responseCode = "404", description = "Scope not found") })
    public @Valid CompletableFuture<List<FullScopeDto>> getScopes(
            @Parameter(required = false, hidden = true) Authentication auth,
            @UnitUuidParam @RequestParam(value = UNIT_PARAM, required = false) String unitUuid,
            @RequestParam(value = DISPLAY_NAME_PARAM, required = false) String displayName,
            @RequestParam(value = SUB_TYPE_PARAM, required = false) String subType) {
        Client client = null;
        try {
            client = getAuthenticatedClient(auth);
        } catch (NoSuchElementException e) {
            return CompletableFuture.supplyAsync(Collections::emptyList);
        }

        final GetEntitiesUseCase.InputData inputData = GetEntitiesInputMapper.map(client, unitUuid,
                                                                                  displayName,
                                                                                  subType);
        return getScopes(inputData);
    }

    private CompletableFuture<List<FullScopeDto>> getScopes(
            GetEntitiesUseCase.InputData inputData) {
        return useCaseInteractor.execute(getScopesUseCase, inputData, output -> output.getEntities()
                                                                                      .stream()
                                                                                      .map(u -> entityToDtoTransformer.transformScope2Dto(u))
                                                                                      .collect(Collectors.toList()));
    }

    @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Loads a scope")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Scope loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = FullScopeDto.class))),
            @ApiResponse(responseCode = "404", description = "Scope not found") })
    public @Valid CompletableFuture<ResponseEntity<FullScopeDto>> getScope(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        Client client = getAuthenticatedClient(auth);

        CompletableFuture<FullScopeDto> scopeFuture = useCaseInteractor.execute(getScopeUseCase,
                                                                                new GetScopeUseCase.InputData(
                                                                                        Key.uuidFrom(uuid),
                                                                                        client),
                                                                                output -> entityToDtoTransformer.transformScope2Dto(output.getScope()));
        return scopeFuture.thenApply(scopeDto -> ResponseEntity.ok()
                                                               .eTag(ETag.from(scopeDto.getId(),
                                                                               scopeDto.getVersion()))
                                                               .body(scopeDto));
    }

    @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}/members")
    @Operation(summary = "Loads the members of a scope")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Members loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = FullScopeDto.class)))),
            @ApiResponse(responseCode = "404", description = "Scope not found") })
    public @Valid CompletableFuture<List<EntityLayerSupertypeDto>> getMembers(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        Client client = getAuthenticatedClient(auth);
        return useCaseInteractor.execute(getScopeUseCase,
                                         new GetScopeUseCase.InputData(Key.uuidFrom(uuid), client),
                                         output -> {
                                             Scope scope = output.getScope();
                                             return scope.getMembers()
                                                         .stream()
                                                         .map(member -> entityToDtoTransformer.transform2Dto(member))
                                                         .collect(Collectors.toList());
                                         });
    }

    @PostMapping()
    @Operation(summary = "Creates a scope")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Scope created") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> createScope(
            @Parameter(hidden = true) ApplicationUser user,
            @Valid @NotNull @RequestBody CreateScopeDto createScopeDto) {
        return useCaseInteractor.execute(createScopeUseCase,
                                         (Supplier<CreateEntityUseCase.InputData<Scope>>) () -> {
                                             Client client = getClient(user);
                                             ModelObjectReferenceResolver modelObjectReferenceResolver = createModelObjectReferenceResolver(client);
                                             return new CreateEntityUseCase.InputData<>(
                                                     dtoToEntityTransformer.transformDto2Scope(createScopeDto,
                                                                                               modelObjectReferenceResolver),
                                                     client);
                                         }, output -> {
                                             Scope scope = output.getEntity();
                                             Optional<String> scopeId = scope.getId() == null
                                                     ? Optional.empty()
                                                     : Optional.ofNullable(scope.getId()
                                                                                .uuidValue());
                                             ApiResponseBody apiResponseBody = new ApiResponseBody(
                                                     true, scopeId, "Scope created successfully.");
                                             return RestApiResponse.created(URL_BASE_PATH,
                                                                            apiResponseBody);
                                         });
    }

    @PutMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Updates a scope")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Scope updated"),
            @ApiResponse(responseCode = "404", description = "Scope not found") })
    public CompletableFuture<FullScopeDto> updateScope(
            @Parameter(hidden = true) ApplicationUser user,
            @RequestHeader(ControllerConstants.IF_MATCH_HEADER) @NotBlank String eTag,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid,
            @Valid @NotNull @RequestBody FullScopeDto scopeDto) {
        scopeDto.applyResourceId(uuid);
        return useCaseInteractor.execute(updateScopeUseCase,
                                         (Supplier<ModifyEntityUseCase.InputData<Scope>>) () -> {
                                             Client client = getClient(user);
                                             ModelObjectReferenceResolver modelObjectReferenceResolver = createModelObjectReferenceResolver(client);
                                             return new UpdateScopeUseCase.InputData<>(
                                                     dtoToEntityTransformer.transformDto2Scope(scopeDto,
                                                                                               modelObjectReferenceResolver),
                                                     client, eTag, user.getUsername());
                                         },
                                         output -> entityToDtoTransformer.transformScope2Dto(output.getEntity()));
    }

    @DeleteMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Deletes a scope")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Scope deleted"),
            @ApiResponse(responseCode = "404", description = "Scope not found") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteScope(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        Client client = getAuthenticatedClient(auth);

        return useCaseInteractor.execute(deleteEntityUseCase,
                                         new DeleteEntityUseCase.InputData(Scope.class,
                                                 Key.uuidFrom(uuid), client),
                                         output -> ResponseEntity.ok()
                                                                 .build());
    }

    @Override
    protected String buildSearchUri(String id) {
        return linkTo(methodOn(ScopeController.class).runSearch(ANY_AUTH, id)).withSelfRel()
                                                                              .getHref();
    }

    @GetMapping(value = "/searches/{searchId}")
    @Operation(summary = "Finds scopes for the search.")
    public @Valid CompletableFuture<List<FullScopeDto>> runSearch(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String searchId) {
        try {
            return getScopes(GetEntitiesInputMapper.map(getAuthenticatedClient(auth),
                                                        SearchQueryDto.decodeFromSearchId(searchId)));
        } catch (IOException e) {
            log.error(String.format("Could not decode search URL: %s", e.getLocalizedMessage()));
            return null;
        }
    }
}
