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
import org.veo.adapter.presenter.api.dto.AbstractControlDto;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.create.CreateControlDto;
import org.veo.adapter.presenter.api.dto.full.FullControlDto;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.EntityTypeNames;
import org.veo.core.entity.Key;
import org.veo.core.usecase.base.CreateEntityUseCase;
import org.veo.core.usecase.base.DeleteEntityUseCase;
import org.veo.core.usecase.base.ModifyEntityUseCase;
import org.veo.core.usecase.base.ModifyEntityUseCase.InputData;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.control.CreateControlUseCase;
import org.veo.core.usecase.control.GetControlUseCase;
import org.veo.core.usecase.control.GetControlsUseCase;
import org.veo.core.usecase.control.UpdateControlUseCase;
import org.veo.rest.annotations.ParameterUuid;
import org.veo.rest.annotations.UnitUuidParam;
import org.veo.rest.common.RestApiResponse;
import org.veo.rest.interactor.UseCaseInteractorImpl;
import org.veo.rest.security.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

/**
 * REST service which provides methods to manage controls.
 */
@RestController
@RequestMapping(ControlController.URL_BASE_PATH)
@Slf4j
public class ControlController extends AbstractEntityController {

    public static final String URL_BASE_PATH = "/" + EntityTypeNames.CONTROLS;

    private final UseCaseInteractorImpl useCaseInteractor;
    private final CreateControlUseCase createControlUseCase;
    private final GetControlUseCase getControlUseCase;
    private final GetControlsUseCase getControlsUseCase;
    private final UpdateControlUseCase updateControlUseCase;
    private final DeleteEntityUseCase deleteEntityUseCase;
    private final ModelObjectReferenceResolver referenceResolver;

    public ControlController(UseCaseInteractorImpl useCaseInteractor,
            CreateControlUseCase createControlUseCase, GetControlUseCase getControlUseCase,
            GetControlsUseCase getControlsUseCase, UpdateControlUseCase updateControlUseCase,
            DeleteEntityUseCase deleteEntityUseCase,
            ModelObjectReferenceResolver referenceResolver) {
        this.useCaseInteractor = useCaseInteractor;
        this.createControlUseCase = createControlUseCase;
        this.getControlUseCase = getControlUseCase;
        this.getControlsUseCase = getControlsUseCase;
        this.updateControlUseCase = updateControlUseCase;
        this.deleteEntityUseCase = deleteEntityUseCase;
        this.referenceResolver = referenceResolver;
    }

    @GetMapping
    @Operation(summary = "Loads all controls")
    public @Valid CompletableFuture<List<FullControlDto>> getControls(
            @Parameter(required = false, hidden = true) Authentication auth,
            @UnitUuidParam @RequestParam(value = UNIT_PARAM, required = false) String unitUuid,
            @UnitUuidParam @RequestParam(value = DISPLAY_NAME_PARAM,
                                         required = false) String displayName) {
        Client client = null;
        try {
            client = getAuthenticatedClient(auth);
        } catch (NoSuchElementException e) {
            return CompletableFuture.supplyAsync(Collections::emptyList);
        }

        final GetControlsUseCase.InputData inputData = new GetControlsUseCase.InputData(client,
                Optional.ofNullable(unitUuid), Optional.ofNullable(displayName));
        return useCaseInteractor.execute(getControlsUseCase, inputData, output -> {
            return output.getEntities()
                         .stream()
                         .map(u -> FullControlDto.from(u, referenceAssembler))
                         .collect(Collectors.toList());

        });
    }

    @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Loads a control")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Control loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = AbstractControlDto.class))),
            @ApiResponse(responseCode = "404", description = "Control not found") })
    public @Valid CompletableFuture<ResponseEntity<FullControlDto>> getControl(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        Client client = getAuthenticatedClient(auth);

        CompletableFuture<FullControlDto> controlFuture = useCaseInteractor.execute(getControlUseCase,
                                                                                    new GetControlUseCase.InputData(
                                                                                            Key.uuidFrom(uuid),
                                                                                            client),
                                                                                    output -> FullControlDto.from(output.getControl(),
                                                                                                                  referenceAssembler));

        return controlFuture.thenApply(controlDto -> ResponseEntity.ok()
                                                                   .eTag(ETag.from(controlDto.getId(),
                                                                                   controlDto.getVersion()))
                                                                   .body(controlDto));
    }

    @PostMapping()
    @Operation(summary = "Creates a control")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Control created") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> createControl(
            @Parameter(hidden = true) ApplicationUser user,
            @Valid @NotNull @RequestBody CreateControlDto dto) {
        return useCaseInteractor.execute(createControlUseCase,
                                         (Supplier<CreateEntityUseCase.InputData<Control>>) () -> {

                                             Client client = getClient(user);
                                             DtoToEntityContext tcontext = referenceResolver.loadIntoContext(client,
                                                                                                             dto.getReferences());
                                             return new CreateEntityUseCase.InputData<>(
                                                     dto.toEntity(tcontext), client,
                                                     user.getUsername());
                                         }, output -> {
                                             ApiResponseBody body = CreateOutputMapper.map(output.getEntity());
                                             return RestApiResponse.created(URL_BASE_PATH, body);
                                         });
    }

    @PutMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Updates a control")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Control updated"),
            @ApiResponse(responseCode = "404", description = "Control not found") })
    public CompletableFuture<FullControlDto> updateControl(
            @Parameter(hidden = true) ApplicationUser user,
            @RequestHeader(ControllerConstants.IF_MATCH_HEADER) @NotBlank String eTag,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid,
            @Valid @NotNull @RequestBody FullControlDto controlDto) {
        applyId(uuid, controlDto);
        return useCaseInteractor.execute(updateControlUseCase,
                                         new Supplier<ModifyEntityUseCase.InputData<Control>>() {

                                             @Override
                                             public InputData<Control> get() {
                                                 Client client = getClient(user);
                                                 DtoToEntityContext tcontext = referenceResolver.loadIntoContext(client,
                                                                                                                 controlDto.getReferences());
                                                 return new ModifyEntityUseCase.InputData<Control>(
                                                         controlDto.toEntity(tcontext), client,
                                                         eTag, user.getUsername());
                                             }

                                         },

                                         output -> FullControlDto.from(output.getEntity(),
                                                                       referenceAssembler));
    }

    @DeleteMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Deletes a control")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Control deleted"),
            @ApiResponse(responseCode = "404", description = "Control not found") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteControl(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        Client client = getAuthenticatedClient(auth);
        return useCaseInteractor.execute(deleteEntityUseCase,
                                         new DeleteEntityUseCase.InputData(Control.class,
                                                 Key.uuidFrom(uuid), client),
                                         output -> ResponseEntity.ok()
                                                                 .build());
    }

    @Override
    protected String buildSearchUri(String id) {
        return linkTo(methodOn(ControlController.class).runSearch(ANY_AUTH, id)).withSelfRel()
                                                                                .getHref();
    }

    @GetMapping(value = "/searches/{searchId}")
    @Operation(summary = "Finds controls for the search.")
    public @Valid CompletableFuture<List<FullControlDto>> runSearch(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String searchId) {
        // TODO VEO-38 replace this placeholder implementation with a search usecase:
        try {
            var searchQuery = SearchQueryDto.decodeFromSearchId(searchId);
            return getControls(auth, searchQuery.getUnitId(), searchQuery.getDisplayName());
        } catch (IOException e) {
            log.error(String.format("Could not decode search URL: %s", e.getLocalizedMessage()));
            return null;
        }
    }
}
