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

import static org.veo.rest.ControllerConstants.PARENT_PARAM;
import static org.veo.rest.ControllerConstants.UUID_PARAM;
import static org.veo.rest.ControllerConstants.UUID_REGEX;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.io.mapper.CreateUnitOutputMapper;
import org.veo.adapter.presenter.api.request.CreateUnitDto;
import org.veo.adapter.presenter.api.response.UnitDto;
import org.veo.adapter.presenter.api.response.transformer.DtoEntityToTargetContext;
import org.veo.adapter.presenter.api.response.transformer.DtoTargetToEntityContext;
import org.veo.adapter.presenter.api.unit.CreateUnitInputMapper;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.impl.UnitImpl;
import org.veo.core.usecase.unit.ChangeUnitUseCase;
import org.veo.core.usecase.unit.CreateUnitUseCase;
import org.veo.core.usecase.unit.DeleteUnitUseCase;
import org.veo.core.usecase.unit.GetUnitUseCase;
import org.veo.core.usecase.unit.GetUnitsUseCase;
import org.veo.core.usecase.unit.UpdateUnitUseCase;
import org.veo.rest.annotations.ParameterUuid;
import org.veo.rest.annotations.ParameterUuidParent;
import org.veo.rest.common.RestApiResponse;
import org.veo.rest.interactor.UseCaseInteractorImpl;
import org.veo.rest.security.ApplicationUser;

/**
 * REST service which provides methods to manage units.
 *
 * Uses async calls with {@code CompletableFuture} to parallelize long running
 * operations (i.e. network calls to the database or to other HTTP services).
 *
 * @see <a href=
 *      "https://spring.io/guides/gs/async-method">https://spring.io/guides/gs/async-method/</a>
 */
@RestController
@RequestMapping(UnitController.URL_BASE_PATH)
@RequiredArgsConstructor
public class UnitController extends AbstractEntityController {

    public static final String URL_BASE_PATH = "/units";

    private final UseCaseInteractorImpl useCaseInteractor;
    private final CreateUnitUseCase createUnitUseCase;
    private final GetUnitUseCase getUnitUseCase;
    private final UpdateUnitUseCase putUnitUseCase;
    private final DeleteUnitUseCase deleteUnitUseCase;
    private final GetUnitsUseCase getUnitsUseCase;

    @GetMapping
    @Operation(summary = "Loads all units")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Units loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = UnitDto.class)))) })

    public @Valid CompletableFuture<List<UnitDto>> getUnits(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuidParent @RequestParam(value = PARENT_PARAM,
                                               required = false) String parentUuid) {
        DtoEntityToTargetContext tcontext = DtoEntityToTargetContext.getCompleteTransformationContext();

        return useCaseInteractor.execute(getUnitsUseCase,
                                         new GetUnitsUseCase.InputData(getAuthenticatedClient(auth),
                                                 Optional.ofNullable(parentUuid)),
                                         units -> units.stream()
                                                       .map(u -> UnitDto.from(u, tcontext))
                                                       .collect(Collectors.toList()));
    }

    @Async
    @GetMapping(value = "/{id}")
    @Operation(summary = "Loads a unit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Unit loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = UnitDto.class))),
            @ApiResponse(responseCode = "404", description = "Unit not found") })
    public @Valid CompletableFuture<UnitDto> getUnit(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String id) {

        return useCaseInteractor.execute(getUnitUseCase, new GetUnitUseCase.InputData(
                Key.uuidFrom(id), getAuthenticatedClient(auth)), unit -> {
                    DtoEntityToTargetContext tcontext = DtoEntityToTargetContext.getCompleteTransformationContext();
                    tcontext.partialDomain();
                    return UnitDto.from(unit, tcontext);
                });
    }

    @Async
    @PostMapping()
    @Operation(summary = "Creates a unit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                         description = "Unit created",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = RestApiResponse.class))) })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> createUnit(
            @Parameter(required = false, hidden = true) Authentication auth,
            @Valid @RequestBody CreateUnitDto createUnitDto) {

        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());

        return useCaseInteractor.execute(createUnitUseCase,
                                         CreateUnitInputMapper.map(createUnitDto,
                                                                   user.getClientId()),
                                         unit -> {
                                             ApiResponseBody body = CreateUnitOutputMapper.map(unit);
                                             return RestApiResponse.created(URL_BASE_PATH, body);
                                         });
    }

    @Async
    @PutMapping(value = "/{id}")
    // @Operation(summary = "Updates a unit")
    // @ApiResponses(value = { @ApiResponse(responseCode = "200", description =
    // "Unit updated"),
    // @ApiResponse(responseCode = "404", description = "Unit not found") })
    public @Valid CompletableFuture<UnitDto> updateUnit(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String id, @Valid @RequestBody UnitDto unitDto) {

        DtoTargetToEntityContext tcontext = configureDtoContext(getAuthenticatedClient(auth),
                                                                Collections.emptyList());

        return useCaseInteractor.execute(putUnitUseCase,
                                         new UpdateUnitUseCase.InputData(unitDto.toUnit(tcontext),
                                                 getAuthenticatedClient(auth)),
                                         unit -> UnitDto.from(unit,
                                                              DtoEntityToTargetContext.getCompleteTransformationContext()));
    }

    @Async
    @DeleteMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Deletes a unit")
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Unit deleted"),
            @ApiResponse(responseCode = "404", description = "Unit not found") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteUnit(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid) {

        Unit unit = new UnitImpl(Key.uuidFrom(uuid), null, null);

        return useCaseInteractor.execute(deleteUnitUseCase, new ChangeUnitUseCase.InputData(unit,
                getAuthenticatedClient(auth)), output -> {
                    return RestApiResponse.noContent();
                });
    }

}
