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
import static org.veo.rest.ControllerConstants.PARENT_PARAM;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.veo.adapter.ModelObjectReferenceResolver;
import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.create.CreateUnitDto;
import org.veo.adapter.presenter.api.dto.full.FullUnitDto;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.adapter.presenter.api.unit.CreateUnitInputMapper;
import org.veo.core.entity.Client;
import org.veo.core.entity.EntityTypeNames;
import org.veo.core.entity.Key;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.unit.ChangeUnitUseCase;
import org.veo.core.usecase.unit.CreateUnitUseCase;
import org.veo.core.usecase.unit.DeleteUnitUseCase;
import org.veo.core.usecase.unit.GetUnitUseCase;
import org.veo.core.usecase.unit.GetUnitsUseCase;
import org.veo.core.usecase.unit.UpdateUnitUseCase;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST service which provides methods to manage units.
 * <p>
 * Uses async calls with {@code CompletableFuture} to parallelize long running
 * operations (i.e. network calls to the database or to other HTTP services).
 *
 * @see <a href=
 *      "https://spring.io/guides/gs/async-method">https://spring.io/guides/gs/async-method/</a>
 */
@RestController
@RequestMapping(UnitController.URL_BASE_PATH)
@RequiredArgsConstructor
@Slf4j
public class UnitController extends AbstractEntityController {

    public static final String URL_BASE_PATH = "/" + EntityTypeNames.UNITS;

    private final UseCaseInteractor useCaseInteractor;
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
                                            array = @ArraySchema(schema = @Schema(implementation = FullUnitDto.class)))) })

    public @Valid CompletableFuture<List<FullUnitDto>> getUnits(
            @Parameter(required = false, hidden = true) Authentication auth,
            @UnitUuidParam @RequestParam(value = PARENT_PARAM, required = false) String parentUuid,
            @UnitUuidParam @RequestParam(value = DISPLAY_NAME_PARAM,
                                         required = false) String displayName) {
        Client client = null;
        try {
            client = getAuthenticatedClient(auth);
        } catch (NoSuchElementException e) {
            return CompletableFuture.supplyAsync(Collections::emptyList);
        }

        // TODO VEO-425 apply display name filter.
        final GetUnitsUseCase.InputData inputData = new GetUnitsUseCase.InputData(client,
                Optional.ofNullable(parentUuid));

        return useCaseInteractor.execute(getUnitsUseCase, inputData, output -> {
            return output.getUnits()
                         .stream()
                         .map(u -> entityToDtoTransformer.transformUnit2Dto(u))
                         .collect(Collectors.toList());
        });
    }

    @Async
    @GetMapping(value = "/{id}")
    @Operation(summary = "Loads a unit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Unit loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = FullUnitDto.class))),
            @ApiResponse(responseCode = "404", description = "Unit not found") })
    public @Valid CompletableFuture<ResponseEntity<FullUnitDto>> getUnit(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String id) {

        CompletableFuture<FullUnitDto> unitFuture = useCaseInteractor.execute(getUnitUseCase,
                                                                              new GetUnitUseCase.InputData(
                                                                                      Key.uuidFrom(id),
                                                                                      getAuthenticatedClient(auth)),
                                                                              output -> entityToDtoTransformer.transformUnit2Dto(output.getUnit()));
        return unitFuture.thenApply(unitDto -> ResponseEntity.ok()
                                                             .eTag(ETag.from(unitDto.getId(),
                                                                             unitDto.getVersion()))
                                                             .body(unitDto));
    }

    // TODO: veo-279 use the complete dto
    @Async
    @PostMapping()
    @Operation(summary = "Creates a unit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201",
                         description = "Unit created",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = RestApiResponse.class))) })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> createUnit(
            @Parameter(hidden = true) ApplicationUser user,
            @Valid @RequestBody CreateUnitDto createUnitDto) {

        return useCaseInteractor.execute(createUnitUseCase,
                                         CreateUnitInputMapper.map(createUnitDto,
                                                                   user.getClientId()),
                                         output -> {
                                             ApiResponseBody body = CreateOutputMapper.map(output.getUnit());
                                             return RestApiResponse.created(URL_BASE_PATH, body);
                                         });
    }

    @Async
    @PutMapping(value = "/{id}")
    // @Operation(summary = "Updates a unit")
    // @ApiResponses(value = { @ApiResponse(responseCode = "200", description =
    // "Unit updated"),
    // @ApiResponse(responseCode = "404", description = "Unit not found") })
    public CompletableFuture<FullUnitDto> updateUnit(@Parameter(hidden = true) ApplicationUser user,
            @RequestHeader(ControllerConstants.IF_MATCH_HEADER) @NotBlank String eTag,
            @PathVariable String id, @Valid @RequestBody FullUnitDto unitDto) {
        unitDto.applyResourceId(id);
        return useCaseInteractor.execute(putUnitUseCase,
                                         (Supplier<ChangeUnitUseCase.InputData>) () -> {
                                             Client client = getClient(user);
                                             ModelObjectReferenceResolver modelObjectReferenceResolver = createModelObjectReferenceResolver(client);
                                             return new UpdateUnitUseCase.InputData(
                                                     dtoToEntityTransformer.transformDto2Unit(unitDto,
                                                                                              modelObjectReferenceResolver),
                                                     client, eTag, user.getUsername());
                                         },
                                         output -> entityToDtoTransformer.transformUnit2Dto(output.getUnit()));
    }

    @Async
    @DeleteMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Deletes a unit")
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Unit deleted"),
            @ApiResponse(responseCode = "404", description = "Unit not found") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteUnit(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid) {

        return useCaseInteractor.execute(deleteUnitUseCase, new DeleteUnitUseCase.InputData(
                Key.uuidFrom(uuid), getAuthenticatedClient(auth)), output -> {
                    return RestApiResponse.noContent();
                });
    }

    @Override
    protected String buildSearchUri(String id) {
        return linkTo(methodOn(UnitController.class).runSearch(ANY_AUTH, id)).withSelfRel()
                                                                             .getHref();
    }

    @GetMapping(value = "/searches/{searchId}")
    @Operation(summary = "Finds units for the search.")
    public @Valid CompletableFuture<List<FullUnitDto>> runSearch(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String searchId) {
        // TODO VEO-425 Use custom search query DTO & criteria API, apply
        // display name
        // filter and allow recursive parent unit filter.
        try {
            var dto = SearchQueryDto.decodeFromSearchId(searchId);
            // TODO VEO-425 Apply all unit id values (as IN condition).
            if (dto.getUnitId() != null && !dto.getUnitId().values.isEmpty()) {
                return getUnits(auth, dto.getUnitId()
                                         .getValues()
                                         .iterator()
                                         .next(),
                                null);
            }
            return getUnits(auth, null, null);

        } catch (IOException e) {
            log.error("Could not decode search URL.", e.getLocalizedMessage());
            throw new IllegalArgumentException("Could not decode search URL.");
        }
    }
}
