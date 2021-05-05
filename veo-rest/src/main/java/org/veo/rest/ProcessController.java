/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2018  Alexander Koderman.
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

import org.springframework.beans.factory.annotation.Autowired;
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
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.EntityLayerSupertypeDto;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.create.CreateProcessDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessDto;
import org.veo.adapter.presenter.api.dto.full.ProcessRiskDto;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.adapter.presenter.api.io.mapper.GetEntitiesInputMapper;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Process;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.core.usecase.base.CreateEntityUseCase;
import org.veo.core.usecase.base.DeleteEntityUseCase;
import org.veo.core.usecase.base.GetEntitiesUseCase;
import org.veo.core.usecase.base.ModifyEntityUseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.process.CreateProcessRiskUseCase;
import org.veo.core.usecase.process.CreateProcessUseCase;
import org.veo.core.usecase.process.GetProcessRiskUseCase;
import org.veo.core.usecase.process.GetProcessRisksUseCase;
import org.veo.core.usecase.process.GetProcessUseCase;
import org.veo.core.usecase.process.GetProcessesUseCase;
import org.veo.core.usecase.process.UpdateProcessRiskUseCase;
import org.veo.core.usecase.process.UpdateProcessUseCase;
import org.veo.core.usecase.risk.DeleteRiskUseCase;
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
public class ProcessController extends AbstractEntityController implements ProcessRiskResource {

    public static final String URL_BASE_PATH = "/" + Process.PLURAL_TERM;

    private final UseCaseInteractor useCaseInteractor;
    private final CreateProcessUseCase createProcessUseCase;
    private final GetProcessUseCase getProcessUseCase;
    private final UpdateProcessUseCase updateProcessUseCase;
    private final DeleteEntityUseCase deleteEntityUseCase;
    private final GetProcessesUseCase getProcessesUseCase;
    private final GetProcessRiskUseCase getProcessRiskUseCase;
    private final CreateProcessRiskUseCase createProcessRiskUseCase;
    private final GetProcessRisksUseCase getProcessRisksUseCase;
    private final DeleteRiskUseCase deleteRiskUseCase;
    private final UpdateProcessRiskUseCase updateProcessRiskUseCase;

    @Autowired
    ReferenceAssembler urlAssembler;

    public ProcessController(UseCaseInteractor useCaseInteractor,
            CreateProcessUseCase createProcessUseCase, GetProcessUseCase getProcessUseCase,
            UpdateProcessUseCase putProcessUseCase, DeleteEntityUseCase deleteEntityUseCase,
            GetProcessesUseCase getProcessesUseCase,
            CreateProcessRiskUseCase createProcessRiskUseCase,
            GetProcessRiskUseCase getProcessRiskUseCase,
            GetProcessRisksUseCase getProcessRisksUseCase, DeleteRiskUseCase deleteRiskUseCase,
            UpdateProcessRiskUseCase updateProcessRiskUseCase) {
        this.useCaseInteractor = useCaseInteractor;
        this.createProcessUseCase = createProcessUseCase;
        this.getProcessUseCase = getProcessUseCase;
        this.updateProcessUseCase = putProcessUseCase;
        this.deleteEntityUseCase = deleteEntityUseCase;
        this.getProcessesUseCase = getProcessesUseCase;
        this.createProcessRiskUseCase = createProcessRiskUseCase;
        this.getProcessRiskUseCase = getProcessRiskUseCase;
        this.getProcessRisksUseCase = getProcessRisksUseCase;
        this.deleteRiskUseCase = deleteRiskUseCase;
        this.updateProcessRiskUseCase = updateProcessRiskUseCase;
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
                                                                                    (Supplier<UseCase.IdAndClient>) () -> new UseCase.IdAndClient(
                                                                                            Key.uuidFrom(id),
                                                                                            getAuthenticatedClient(auth))

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
        return useCaseInteractor.execute(getProcessUseCase,
                                         new UseCase.IdAndClient(Key.uuidFrom(uuid), client),
                                         output -> {
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
                                                                                                 modelObjectReferenceResolver),
                                                     client);
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
    @Operation(summary = "Loads all processes")
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

        return getProcesses(GetEntitiesInputMapper.map(client, parentUuid, displayName, subType,
                                                       PagingConfiguration.UNPAGED));
    }

    private CompletableFuture<List<FullProcessDto>> getProcesses(
            GetEntitiesUseCase.InputData inputData) {
        return useCaseInteractor.execute(getProcessesUseCase, inputData,
                                         output -> output.getEntities()
                                                         .getResultPage()
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
                                                           SearchQueryDto.decodeFromSearchId(searchId),
                                                           PagingConfiguration.UNPAGED));
        } catch (IOException e) {
            log.error("Could not decode search URL: {}", e.getLocalizedMessage());
            return null;
        }
    }

    @Override
    public @Valid CompletableFuture<List<ProcessRiskDto>> getRisks(
            @Parameter(hidden = true) ApplicationUser user, String processId) {

        Client client = getClient(user.getClientId());
        var input = new GetProcessRisksUseCase.InputData(client, Key.uuidFrom(processId));

        return useCaseInteractor.execute(getProcessRisksUseCase, input, output -> {
            return output.getRisks()
                         .stream()
                         .map(risk -> ProcessRiskDto.from(risk, referenceAssembler))
                         .collect(Collectors.toList());
        });
    }

    @Override
    public @Valid CompletableFuture<ResponseEntity<ProcessRiskDto>> getRisk(
            @Parameter(hidden = true) ApplicationUser user, String processId, String scenarioId) {

        Client client = getClient(user.getClientId());
        var input = new GetProcessRiskUseCase.InputData(client, Key.uuidFrom(processId),
                Key.uuidFrom(scenarioId));

        var riskFuture = useCaseInteractor.execute(getProcessRiskUseCase, input,
                                                   output -> ProcessRiskDto.from(output.getRisk(),
                                                                                 referenceAssembler));

        return riskFuture.thenApply(riskDto -> ResponseEntity.ok()
                                                             .eTag(ETag.from(riskDto.getProcess()
                                                                                    .getId(),
                                                                             riskDto.getScenario()
                                                                                    .getId(),
                                                                             riskDto.getVersion()))
                                                             .body(riskDto));
    }

    @Override
    public CompletableFuture<ResponseEntity<ApiResponseBody>> createRisk(ApplicationUser user,
            @Valid @NotNull ProcessRiskDto dto, String processId) {

        var input = new CreateProcessRiskUseCase.InputData(getClient(user.getClientId()),
                Key.uuidFrom(processId), urlAssembler.toKey(dto.getScenario()),
                urlAssembler.toKeys(dto.getDomains()), urlAssembler.toKey(dto.getMitigation()),
                urlAssembler.toKey(dto.getRiskOwner()));

        return useCaseInteractor.execute(createProcessRiskUseCase, input, output -> {
            var url = String.format("%s/%s/%s", URL_BASE_PATH, output.getRisk()
                                                                     .getEntity()
                                                                     .getId()
                                                                     .uuidValue(),
                                    ProcessRiskResource.RESOURCE_NAME);
            var body = new ApiResponseBody(true, Optional.of(output.getRisk()
                                                                   .getScenario()
                                                                   .getId()
                                                                   .uuidValue()),
                    "Process risk created successfully.", "");
            return RestApiResponse.created(url, body);
        });
    }

    @Override
    public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteRisk(ApplicationUser user,
            String processId, String scenarioId) {

        Client client = getClient(user.getClientId());
        var input = new DeleteRiskUseCase.InputData(Process.class, client, Key.uuidFrom(processId),
                Key.uuidFrom(scenarioId));

        return useCaseInteractor.execute(deleteRiskUseCase, input, output -> ResponseEntity.ok()
                                                                                           .build());
    }

    @Override
    public @Valid CompletableFuture<ResponseEntity<ProcessRiskDto>> updateRisk(ApplicationUser user,
            String processId, String scenarioId, @Valid @NotNull ProcessRiskDto dto, String eTag) {

        var input = new UpdateProcessRiskUseCase.InputData(getClient(user.getClientId()),
                Key.uuidFrom(processId), urlAssembler.toKey(dto.getScenario()),
                urlAssembler.toKeys(dto.getDomains()), urlAssembler.toKey(dto.getMitigation()),
                urlAssembler.toKey(dto.getRiskOwner()), eTag);

        // update risk and return saved risk with updated ETag, timestamps etc.:
        return useCaseInteractor.execute(updateProcessRiskUseCase, input, output -> null)
                                .thenCompose(o -> this.getRisk(user, processId, scenarioId));
    }
}
