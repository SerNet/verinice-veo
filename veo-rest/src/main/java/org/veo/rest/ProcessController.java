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
import static org.veo.rest.ControllerConstants.ANY_INT;
import static org.veo.rest.ControllerConstants.ANY_STRING;
import static org.veo.rest.ControllerConstants.DESCRIPTION_PARAM;
import static org.veo.rest.ControllerConstants.DESIGNATOR_PARAM;
import static org.veo.rest.ControllerConstants.DISPLAY_NAME_PARAM;
import static org.veo.rest.ControllerConstants.NAME_PARAM;
import static org.veo.rest.ControllerConstants.PAGE_NUMBER_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.PAGE_NUMBER_PARAM;
import static org.veo.rest.ControllerConstants.PAGE_SIZE_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.PAGE_SIZE_PARAM;
import static org.veo.rest.ControllerConstants.SORT_COLUMN_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.SORT_COLUMN_PARAM;
import static org.veo.rest.ControllerConstants.SORT_ORDER_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.SORT_ORDER_PARAM;
import static org.veo.rest.ControllerConstants.SORT_ORDER_PATTERN;
import static org.veo.rest.ControllerConstants.STATUS_PARAM;
import static org.veo.rest.ControllerConstants.SUB_TYPE_PARAM;
import static org.veo.rest.ControllerConstants.UNIT_PARAM;
import static org.veo.rest.ControllerConstants.UPDATED_BY_PARAM;
import static org.veo.rest.ControllerConstants.UUID_PARAM;
import static org.veo.rest.ControllerConstants.UUID_REGEX;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
import org.veo.adapter.presenter.api.dto.create.CreateProcessDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessDto;
import org.veo.adapter.presenter.api.dto.full.ProcessRiskDto;
import org.veo.adapter.presenter.api.io.mapper.CategorizedRiskValueMapper;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.adapter.presenter.api.io.mapper.GetElementsInputMapper;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Process;
import org.veo.core.usecase.base.CreateElementUseCase;
import org.veo.core.usecase.base.DeleteElementUseCase;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.base.ModifyElementUseCase.InputData;
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
 * Controller for the resource API of "Process" entities.
 * <p>
 * A process is a business entity
 */
@RestController
@RequestMapping(ProcessController.URL_BASE_PATH)
@Slf4j
public class ProcessController extends AbstractElementController<Process, FullProcessDto>
        implements ProcessRiskResource {

    public static final String URL_BASE_PATH = "/" + Process.PLURAL_TERM;

    private final CreateProcessUseCase createProcessUseCase;
    private final UpdateProcessUseCase updateProcessUseCase;
    private final DeleteElementUseCase deleteElementUseCase;
    private final GetProcessesUseCase getProcessesUseCase;
    private final GetProcessRiskUseCase getProcessRiskUseCase;
    private final CreateProcessRiskUseCase createProcessRiskUseCase;
    private final GetProcessRisksUseCase getProcessRisksUseCase;
    private final DeleteRiskUseCase deleteRiskUseCase;
    private final UpdateProcessRiskUseCase updateProcessRiskUseCase;

    public ProcessController(CreateProcessUseCase createProcessUseCase,
            GetProcessUseCase getProcessUseCase, UpdateProcessUseCase putProcessUseCase,
            DeleteElementUseCase deleteElementUseCase, GetProcessesUseCase getProcessesUseCase,
            CreateProcessRiskUseCase createProcessRiskUseCase,
            GetProcessRiskUseCase getProcessRiskUseCase,
            GetProcessRisksUseCase getProcessRisksUseCase, DeleteRiskUseCase deleteRiskUseCase,
            UpdateProcessRiskUseCase updateProcessRiskUseCase) {
        super(Process.class, getProcessUseCase);
        this.createProcessUseCase = createProcessUseCase;
        this.updateProcessUseCase = putProcessUseCase;
        this.deleteElementUseCase = deleteElementUseCase;
        this.getProcessesUseCase = getProcessesUseCase;
        this.createProcessRiskUseCase = createProcessRiskUseCase;
        this.getProcessRiskUseCase = getProcessRiskUseCase;
        this.getProcessRisksUseCase = getProcessRisksUseCase;
        this.deleteRiskUseCase = deleteRiskUseCase;
        this.updateProcessRiskUseCase = updateProcessRiskUseCase;
    }

    @Override
    @Operation(summary = "Loads a process")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Process loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = FullProcessDto.class))),
            @ApiResponse(responseCode = "404", description = "Process not found") })
    @GetMapping(ControllerConstants.UUID_PARAM_SPEC)
    public @Valid CompletableFuture<ResponseEntity<FullProcessDto>> getElement(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid, WebRequest request) {
        return super.getElement(auth, uuid, request);
    }

    @Override
    @Operation(summary = "Loads the parts of a process")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Parts loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = FullProcessDto.class)))),
            @ApiResponse(responseCode = "404", description = "Process not found") })
    @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}/parts")
    public @Valid CompletableFuture<ResponseEntity<List<FullProcessDto>>> getElementParts(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid, WebRequest request) {
        return super.getElementParts(auth, uuid, request);
    }

    @PostMapping()
    @Operation(summary = "Creates a process")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Process created") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> createProcess(
            @Parameter(hidden = true) ApplicationUser user,
            @Valid @NotNull @RequestBody @JsonSchemaValidation(Process.SINGULAR_TERM) CreateProcessDto dto) {

        return useCaseInteractor.execute(createProcessUseCase,
                                         (Supplier<CreateElementUseCase.InputData<Process>>) () -> {
                                             Client client = getClient(user);
                                             IdRefResolver idRefResolver = createIdRefResolver(client);
                                             return new CreateElementUseCase.InputData<>(
                                                     dtoToEntityTransformer.transformDto2Process(dto,
                                                                                                 idRefResolver),
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
            @PathVariable String id,
            @Valid @RequestBody @JsonSchemaValidation(Process.SINGULAR_TERM) FullProcessDto processDto) {
        processDto.applyResourceId(id);
        return useCaseInteractor.execute(updateProcessUseCase,
                                         (Supplier<InputData<Process>>) () -> {
                                             Client client = getClient(user);
                                             IdRefResolver idRefResolver = createIdRefResolver(client);
                                             return new InputData<>(
                                                     dtoToEntityTransformer.transformDto2Process(processDto,
                                                                                                 idRefResolver),
                                                     client, eTag, user.getUsername());
                                         }

                                         ,
                                         output -> entityToDtoTransformer.transformProcess2Dto(output.getEntity()));
    }

    @DeleteMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Deletes a process")
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Process deleted"),
            @ApiResponse(responseCode = "404", description = "Process not found") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteProcess(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        return useCaseInteractor.execute(deleteElementUseCase,
                                         new DeleteElementUseCase.InputData(Process.class,
                                                 Key.uuidFrom(uuid), getAuthenticatedClient(auth)),
                                         output -> ResponseEntity.noContent()
                                                                 .build());
    }

    @GetMapping
    @Operation(summary = "Loads all processes")
    public @Valid CompletableFuture<PageDto<FullProcessDto>> getProcesses(
            @Parameter(required = false, hidden = true) Authentication auth,
            @UnitUuidParam @RequestParam(value = UNIT_PARAM, required = false) String parentUuid,
            @UnitUuidParam @RequestParam(value = DISPLAY_NAME_PARAM,
                                         required = false) String displayName,
            @RequestParam(value = SUB_TYPE_PARAM, required = false) String subType,
            @RequestParam(value = STATUS_PARAM, required = false) String status,
            @RequestParam(value = DESCRIPTION_PARAM, required = false) String description,
            @RequestParam(value = DESIGNATOR_PARAM, required = false) String designator,
            @RequestParam(value = NAME_PARAM, required = false) String name,
            @RequestParam(value = UPDATED_BY_PARAM, required = false) String updatedBy,
            @RequestParam(value = PAGE_SIZE_PARAM,
                          required = false,
                          defaultValue = PAGE_SIZE_DEFAULT_VALUE) Integer pageSize,
            @RequestParam(value = PAGE_NUMBER_PARAM,
                          required = false,
                          defaultValue = PAGE_NUMBER_DEFAULT_VALUE) Integer pageNumber,
            @RequestParam(value = SORT_COLUMN_PARAM,
                          required = false,
                          defaultValue = SORT_COLUMN_DEFAULT_VALUE) String sortColumn,
            @RequestParam(value = SORT_ORDER_PARAM,
                          required = false,
                          defaultValue = SORT_ORDER_DEFAULT_VALUE) @Pattern(regexp = SORT_ORDER_PATTERN) String sortOrder) {
        Client client;
        try {
            client = getAuthenticatedClient(auth);
        } catch (NoSuchElementException e) {
            return CompletableFuture.supplyAsync(PageDto::emptyPage);
        }

        return getProcesses(GetElementsInputMapper.map(client, parentUuid, displayName, subType,
                                                       status, description, designator, name,
                                                       updatedBy,
                                                       PagingMapper.toConfig(pageSize, pageNumber,
                                                                             sortColumn,
                                                                             sortOrder)));
    }

    private CompletableFuture<PageDto<FullProcessDto>> getProcesses(
            GetElementsUseCase.InputData inputData) {
        return useCaseInteractor.execute(getProcessesUseCase, inputData,
                                         output -> PagingMapper.toPage(output.getElements(),
                                                                       entityToDtoTransformer::transformProcess2Dto));
    }

    @Override
    @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
    protected String buildSearchUri(String id) {
        return linkTo(methodOn(ProcessController.class).runSearch(ANY_AUTH, id, ANY_INT, ANY_INT,
                                                                  ANY_STRING, ANY_STRING))
                                                                                          .withSelfRel()
                                                                                          .getHref();
    }

    @GetMapping(value = "/searches/{searchId}")
    @Operation(summary = "Finds controls for the search.")
    public @Valid CompletableFuture<PageDto<FullProcessDto>> runSearch(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String searchId,
            @RequestParam(value = PAGE_SIZE_PARAM,
                          required = false,
                          defaultValue = PAGE_SIZE_DEFAULT_VALUE) Integer pageSize,
            @RequestParam(value = PAGE_NUMBER_PARAM,
                          required = false,
                          defaultValue = PAGE_NUMBER_DEFAULT_VALUE) Integer pageNumber,
            @RequestParam(value = SORT_COLUMN_PARAM,
                          required = false,
                          defaultValue = SORT_COLUMN_DEFAULT_VALUE) String sortColumn,
            @RequestParam(value = SORT_ORDER_PARAM,
                          required = false,
                          defaultValue = SORT_ORDER_DEFAULT_VALUE) @Pattern(regexp = SORT_ORDER_PATTERN) String sortOrder) {
        try {
            return getProcesses(GetElementsInputMapper.map(getAuthenticatedClient(auth),
                                                           SearchQueryDto.decodeFromSearchId(searchId),
                                                           PagingMapper.toConfig(pageSize,
                                                                                 pageNumber,
                                                                                 sortColumn,
                                                                                 sortOrder)));
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

        return useCaseInteractor.execute(getProcessRisksUseCase, input, output -> output.getRisks()
                                                                                        .stream()
                                                                                        .map(risk -> ProcessRiskDto.from(risk,
                                                                                                                         referenceAssembler))
                                                                                        .collect(Collectors.toList()));
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

        //@formatter:off
        var input = new CreateProcessRiskUseCase.InputData(
                getClient(user.getClientId()),
                Key.uuidFrom(processId),
                urlAssembler.toKey(dto.getScenario()),
                urlAssembler.toKeys(dto.getDomainReferences()),
                urlAssembler.toKey(dto.getMitigation()),
                urlAssembler.toKey(dto.getRiskOwner()),
                CategorizedRiskValueMapper.map(dto.getDomainsWithRiskValues())
        );
        //@formatter:on

        return useCaseInteractor.execute(createProcessRiskUseCase, input, output -> {
            if (!output.isNewlyCreatedRisk())
                return RestApiResponse.noContent();

            var url = String.format("%s/%s/%s", URL_BASE_PATH, output.getRisk()
                                                                     .getEntity()
                                                                     .getId()
                                                                     .uuidValue(),
                                    ProcessRiskResource.RESOURCE_NAME);
            var body = new ApiResponseBody(true, Optional.of(output.getRisk()
                                                                   .getScenario()
                                                                   .getId()
                                                                   .uuidValue()),
                    "Process risk created successfully.");
            return RestApiResponse.created(url, body);
        });
    }

    @Override
    public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteRisk(ApplicationUser user,
            String processId, String scenarioId) {

        Client client = getClient(user.getClientId());
        var input = new DeleteRiskUseCase.InputData(Process.class, client, Key.uuidFrom(processId),
                Key.uuidFrom(scenarioId));

        return useCaseInteractor.execute(deleteRiskUseCase, input,
                                         output -> ResponseEntity.noContent()
                                                                 .build());
    }

    @Override
    public @Valid CompletableFuture<ResponseEntity<ProcessRiskDto>> updateRisk(ApplicationUser user,
            String processId, String scenarioId, @Valid @NotNull ProcessRiskDto dto, String eTag) {

        //@formatter:off
        var input = new UpdateProcessRiskUseCase.InputData(
                getClient(user.getClientId()),
                Key.uuidFrom(processId),
                urlAssembler.toKey(dto.getScenario()),
                urlAssembler.toKeys(dto.getDomainReferences()),
                urlAssembler.toKey(dto.getMitigation()),
                urlAssembler.toKey(dto.getRiskOwner()),
                eTag,
                CategorizedRiskValueMapper.map(dto.getDomainsWithRiskValues())
                );
        //@formatter:on

        // update risk and return saved risk with updated ETag, timestamps etc.:
        return useCaseInteractor.execute(updateProcessRiskUseCase, input, output -> null)
                                .thenCompose(o -> this.getRisk(user, processId, scenarioId));
    }

    @Override
    protected FullProcessDto entity2Dto(Process entity) {
        return entityToDtoTransformer.transformProcess2Dto(entity);
    }
}
