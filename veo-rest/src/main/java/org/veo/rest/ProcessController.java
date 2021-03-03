/*******************************************************************************
 * Copyright (c) 2018 Alexander Koderman.
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
import org.veo.adapter.presenter.api.dto.create.CreateProcessDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessDto;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.adapter.presenter.api.io.mapper.GetEntitiesInputMapper;
import org.veo.core.entity.Client;
import org.veo.core.entity.EntityTypeNames;
import org.veo.core.entity.Key;
import org.veo.core.entity.Process;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.core.usecase.base.CreateEntityUseCase;
import org.veo.core.usecase.base.DeleteEntityUseCase;
import org.veo.core.usecase.base.GetEntitiesUseCase;
import org.veo.core.usecase.base.ModifyEntityUseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.process.CreateProcessUseCase;
import org.veo.core.usecase.process.GetProcessUseCase;
import org.veo.core.usecase.process.GetProcessesUseCase;
import org.veo.core.usecase.process.UpdateProcessUseCase;
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
 * Controller for the resource API of "Process" entities.
 * <p>
 * A process is a business entity
 */
@RestController
@RequestMapping(ProcessController.URL_BASE_PATH)
@Slf4j
public class ProcessController extends AbstractEntityController {

    public static final String URL_BASE_PATH = "/" + EntityTypeNames.PROCESSES;

    private final UseCaseInteractor useCaseInteractor;
    private final CreateProcessUseCase createProcessUseCase;
    private final GetProcessUseCase getProcessUseCase;
    private final UpdateProcessUseCase updateProcessUseCase;
    private final DeleteEntityUseCase deleteEntityUseCase;
    private final GetProcessesUseCase getProcessesUseCase;

    public ProcessController(UseCaseInteractor useCaseInteractor,
            CreateProcessUseCase createProcessUseCase, GetProcessUseCase getProcessUseCase,
            UpdateProcessUseCase putProcessUseCase, DeleteEntityUseCase deleteEntityUseCase,
            GetProcessesUseCase getProcessesUseCase) {
        this.useCaseInteractor = useCaseInteractor;
        this.createProcessUseCase = createProcessUseCase;
        this.getProcessUseCase = getProcessUseCase;
        this.updateProcessUseCase = putProcessUseCase;
        this.deleteEntityUseCase = deleteEntityUseCase;
        this.getProcessesUseCase = getProcessesUseCase;
    }

    /**
     * Load the process for the given id. The result is provided asynchronously by
     * the executed use case.
     *
     * @param id
     *            an ID in the UUID format as specified in RFC 4122
     * @return the process for the given ID if one was found. Null otherwise.
     */
    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<FullProcessDto>> getProcessById(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String id) {
        CompletableFuture<FullProcessDto> processFuture = useCaseInteractor.execute(getProcessUseCase,
                                                                                    new Supplier<GetProcessUseCase.InputData>() {
                                                                                        @Override
                                                                                        public org.veo.core.usecase.process.GetProcessUseCase.InputData get() {
                                                                                            return new GetProcessUseCase.InputData(
                                                                                                    Key.uuidFrom(id),
                                                                                                    getAuthenticatedClient(auth));
                                                                                        }
                                                                                    }

                                                                                    ,
                                                                                    output -> entityToDtoTransformer.transformProcess2Dto(output.getProcess()));

        return processFuture.thenApply(processDto -> ResponseEntity.ok()
                                                                   .eTag(ETag.from(processDto.getId(),
                                                                                   processDto.getVersion()))
                                                                   .body(processDto));
    }

    @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}/parts")
    @Operation(summary = "Loads the parts of a process")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Parts loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = FullProcessDto.class)))),
            @ApiResponse(responseCode = "404", description = "Process not found") })
    public @Valid CompletableFuture<List<EntityLayerSupertypeDto>> getParts(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        Client client = getAuthenticatedClient(auth);
        return useCaseInteractor.execute(getProcessUseCase, new GetProcessUseCase.InputData(
                Key.uuidFrom(uuid), client), output -> {
                    Process scope = output.getProcess();
                    return scope.getParts()
                                .stream()
                                .map(part -> entityToDtoTransformer.transform2Dto(part))
                                .collect(Collectors.toList());
                });
    }

    @PostMapping()
    @Operation(summary = "Creates a process")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Process created") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> createProcess(
            @Parameter(hidden = true) ApplicationUser user,
            @Valid @NotNull @RequestBody CreateProcessDto dto) {
        return useCaseInteractor.execute(createProcessUseCase,
                                         (Supplier<CreateEntityUseCase.InputData<Process>>) () -> {
                                             Client client = getClient(user);
                                             ModelObjectReferenceResolver modelObjectReferenceResolver = createModelObjectReferenceResolver(client);
                                             return new CreateEntityUseCase.InputData<>(
                                                     dtoToEntityTransformer.transformDto2Process(dto,
                                                                                                 null,
                                                                                                 modelObjectReferenceResolver),
                                                     client, user.getUsername());
                                         }

                                         , output -> {
                                             ApiResponseBody body = CreateOutputMapper.map(output.getEntity());
                                             return RestApiResponse.created(URL_BASE_PATH, body);
                                         });
    }

    @PutMapping(value = "/{id}")
    @Operation(summary = "Updates a process")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Process updated"),
            @ApiResponse(responseCode = "404", description = "Process not found") })
    public @Valid CompletableFuture<FullProcessDto> updateProcess(
            @Parameter(hidden = true) ApplicationUser user,
            @RequestHeader(ControllerConstants.IF_MATCH_HEADER) @NotBlank String eTag,
            @PathVariable String id, @Valid @RequestBody FullProcessDto processDto) {
        processDto.applyResourceId(id);
        return useCaseInteractor.execute(updateProcessUseCase,
                                         new Supplier<ModifyEntityUseCase.InputData<Process>>() {

                                             @Override
                                             public ModifyEntityUseCase.InputData<Process> get() {
                                                 Client client = getClient(user);
                                                 ModelObjectReferenceResolver modelObjectReferenceResolver = createModelObjectReferenceResolver(client);
                                                 return new ModifyEntityUseCase.InputData<Process>(
                                                         dtoToEntityTransformer.transformDto2Process(processDto,
                                                                                                     Key.uuidFrom(processDto.getId()),
                                                                                                     modelObjectReferenceResolver),
                                                         client, eTag, user.getUsername());
                                             }
                                         }

                                         ,
                                         output -> entityToDtoTransformer.transformProcess2Dto(output.getEntity()));
    }

    @DeleteMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Deletes a process")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Process deleted"),
            @ApiResponse(responseCode = "404", description = "Process not found") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteProcess(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        return useCaseInteractor.execute(deleteEntityUseCase,
                                         new DeleteEntityUseCase.InputData(Process.class,
                                                 Key.uuidFrom(uuid), getAuthenticatedClient(auth)),
                                         output -> ResponseEntity.ok()
                                                                 .build());
    }

    @GetMapping
    @Operation(summary = "Loads all processs")
    public @Valid CompletableFuture<List<FullProcessDto>> getProcesses(
            @Parameter(required = false, hidden = true) Authentication auth,
            @UnitUuidParam @RequestParam(value = UNIT_PARAM, required = false) String parentUuid,
            @UnitUuidParam @RequestParam(value = DISPLAY_NAME_PARAM,
                                         required = false) String displayName,
            @RequestParam(value = SUB_TYPE_PARAM, required = false) String subType) {
        Client client = null;
        try {
            client = getAuthenticatedClient(auth);
        } catch (NoSuchElementException e) {
            return CompletableFuture.supplyAsync(Collections::emptyList);
        }

        return getProcesses(GetEntitiesInputMapper.map(client, parentUuid, displayName, subType));
    }

    private CompletableFuture<List<FullProcessDto>> getProcesses(
            GetEntitiesUseCase.InputData inputData) {
        return useCaseInteractor.execute(getProcessesUseCase, inputData,
                                         output -> output.getEntities()
                                                         .stream()
                                                         .map(u -> entityToDtoTransformer.transformProcess2Dto(u))
                                                         .collect(Collectors.toList()));
    }

    @Override
    protected String buildSearchUri(String id) {
        return linkTo(methodOn(ProcessController.class).runSearch(ANY_AUTH, id)).withSelfRel()
                                                                                .getHref();
    }

    @GetMapping(value = "/searches/{searchId}")
    @Operation(summary = "Finds controls for the search.")
    public @Valid CompletableFuture<List<FullProcessDto>> runSearch(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String searchId) {
        try {
            return getProcesses(GetEntitiesInputMapper.map(getAuthenticatedClient(auth),
                                                           SearchQueryDto.decodeFromSearchId(searchId)));
        } catch (IOException e) {
            log.error(String.format("Could not decode search URL: %s", e.getLocalizedMessage()));
            return null;
        }
    }
}
