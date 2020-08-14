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

import static org.veo.rest.ControllerConstants.UNIT_PARAM;
import static org.veo.rest.ControllerConstants.UUID_PARAM;
import static org.veo.rest.ControllerConstants.UUID_REGEX;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.create.CreateProcessDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessDto;
import org.veo.adapter.presenter.api.io.mapper.CreateProcessOutputMapper;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoContext;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Process;
import org.veo.core.usecase.base.DeleteEntityUseCase;
import org.veo.core.usecase.base.ModifyEntityUseCase;
import org.veo.core.usecase.process.CreateProcessUseCase;
import org.veo.core.usecase.process.CreateProcessUseCase.InputData;
import org.veo.core.usecase.process.GetProcessUseCase;
import org.veo.core.usecase.process.GetProcessesUseCase;
import org.veo.core.usecase.process.UpdateProcessUseCase;
import org.veo.rest.annotations.ParameterUuid;
import org.veo.rest.annotations.ParameterUuidParent;
import org.veo.rest.common.RestApiResponse;
import org.veo.rest.interactor.UseCaseInteractorImpl;
import org.veo.rest.security.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Controller for the resource API of "Process" entities.
 *
 * A process is a business entity
 */
@RestController
@RequestMapping(ProcessController.URL_BASE_PATH)
public class ProcessController extends AbstractEntityController {

    public static final String URL_BASE_PATH = "/processes";

    private UseCaseInteractorImpl useCaseInteractor;
    private CreateProcessUseCase<ResponseEntity<ApiResponseBody>> createProcessUseCase;
    private GetProcessUseCase<FullProcessDto> getProcessUseCase;
    private UpdateProcessUseCase<FullProcessDto> updateProcessUseCase;
    private final DeleteEntityUseCase deleteEntityUseCase;
    private GetProcessesUseCase<List<FullProcessDto>> getProcessesUseCase;

    public ProcessController(UseCaseInteractorImpl useCaseInteractor,
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
    public CompletableFuture<FullProcessDto> getProcessById(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String id) {
        return useCaseInteractor.execute(getProcessUseCase,
                                         new Supplier<GetProcessUseCase.InputData>() {
                                             @Override
                                             public org.veo.core.usecase.process.GetProcessUseCase.InputData get() {
                                                 return new GetProcessUseCase.InputData(
                                                         Key.uuidFrom(id),
                                                         getAuthenticatedClient(auth));
                                             }
                                         }

                                         , output -> {
                                             EntityToDtoContext tcontext = EntityToDtoContext.getCompleteTransformationContext();

                                             return FullProcessDto.from(output.getProcess(),
                                                                        tcontext);
                                         });
    }

    @PostMapping()
    @Operation(summary = "Creates a process")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Process created") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> createProcess(
            @Parameter(required = false, hidden = true) Authentication auth,
            @Valid @NotNull @RequestBody CreateProcessDto dto) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        return useCaseInteractor.execute(createProcessUseCase,
                                         new Supplier<CreateProcessUseCase.InputData>() {

                                             @Override
                                             public InputData get() {

                                                 Client client = getClient(user.getClientId());
                                                 DtoToEntityContext tcontext = configureDtoContext(client,
                                                                                                   dto.getReferences());
                                                 return new InputData(dto.toEntity(tcontext),
                                                         client);
                                             }
                                         }

                                         , output -> {
                                             ApiResponseBody body = CreateProcessOutputMapper.map(output.getProcess());
                                             return RestApiResponse.created(URL_BASE_PATH, body);
                                         });
    }

    @PutMapping(value = "/{id}")
    @Operation(summary = "Updates a process")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Process updated"),
            @ApiResponse(responseCode = "404", description = "Process not found") })
    public @Valid CompletableFuture<FullProcessDto> updateProcess(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String id, @Valid @RequestBody FullProcessDto processDto) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        return useCaseInteractor.execute(updateProcessUseCase,
                                         new Supplier<ModifyEntityUseCase.InputData<Process>>() {

                                             @Override
                                             public ModifyEntityUseCase.InputData<Process> get() {
                                                 Client client = getClient(user.getClientId());
                                                 DtoToEntityContext tcontext = configureDtoContext(client,
                                                                                                   processDto.getReferences());

                                                 return new ModifyEntityUseCase.InputData<Process>(
                                                         processDto.toEntity(tcontext),
                                                         getAuthenticatedClient(auth));
                                             }
                                         }

                                         ,
                                         output -> FullProcessDto.from(output.getEntity(),
                                                                       EntityToDtoContext.getCompleteTransformationContext()));
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
            @ParameterUuidParent @RequestParam(value = UNIT_PARAM,
                                               required = false) String unitUuid) {
        EntityToDtoContext tcontext = EntityToDtoContext.getCompleteTransformationContext();

        return useCaseInteractor.execute(getProcessesUseCase, new GetProcessesUseCase.InputData(
                getAuthenticatedClient(auth), Optional.ofNullable(unitUuid)), output -> {
                    return output.getEntities()
                                 .stream()
                                 .map(u -> FullProcessDto.from(u, tcontext))
                                 .collect(Collectors.toList());
                });
    }

}
