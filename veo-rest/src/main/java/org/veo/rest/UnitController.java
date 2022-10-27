/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Daniel Murygin.
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
import static org.veo.rest.ControllerConstants.PARENT_PARAM;
import static org.veo.rest.ControllerConstants.UUID_DESCRIPTION;
import static org.veo.rest.ControllerConstants.UUID_EXAMPLE;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

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

import org.veo.adapter.IdRefResolver;
import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.create.CreateUnitDto;
import org.veo.adapter.presenter.api.dto.full.FullUnitDto;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.adapter.presenter.api.openapi.IdRefTailoringReferenceParameterReferencedElement;
import org.veo.adapter.presenter.api.response.IncarnateDescriptionsDto;
import org.veo.adapter.presenter.api.unit.CreateUnitInputMapper;
import org.veo.core.entity.Client;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.catalogitem.ApplyIncarnationDescriptionUseCase;
import org.veo.core.usecase.catalogitem.GetIncarnationDescriptionUseCase;
import org.veo.core.usecase.unit.ChangeUnitUseCase;
import org.veo.core.usecase.unit.CreateUnitUseCase;
import org.veo.core.usecase.unit.DeleteUnitUseCase;
import org.veo.core.usecase.unit.GetUnitUseCase;
import org.veo.core.usecase.unit.GetUnitsUseCase;
import org.veo.core.usecase.unit.UpdateUnitUseCase;
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
 *
 * <p>Uses async calls with {@code CompletableFuture} to parallelize long running operations (i.e.
 * network calls to the database or to other HTTP services).
 *
 * @see <a href=
 *     "https://spring.io/guides/gs/async-method">https://spring.io/guides/gs/async-method/</a>
 */
@RestController
@RequestMapping(UnitController.URL_BASE_PATH)
@RequiredArgsConstructor
@Slf4j
public class UnitController extends AbstractEntityControllerWithDefaultSearch {

  public static final String URL_BASE_PATH = "/" + Unit.PLURAL_TERM;

  private final CreateUnitUseCase createUnitUseCase;
  private final GetUnitUseCase getUnitUseCase;
  private final UpdateUnitUseCase putUnitUseCase;
  private final DeleteUnitUseCase deleteUnitUseCase;
  private final GetUnitsUseCase getUnitsUseCase;
  private final ApplyIncarnationDescriptionUseCase applyIncarnationDescriptionUseCase;
  private final GetIncarnationDescriptionUseCase getIncarnationDescriptionUseCase;

  @GetMapping(value = "/{unitId}/incarnations")
  @Operation(
      summary = "Get the information to incarnate a set of catalogItems in a unit.",
      tags = "modeling")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "incarnation description provided",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = IncarnateDescriptionsDto.class))),
        @ApiResponse(responseCode = "404", description = "incarnation description not found")
      })
  public @Valid CompletableFuture<ResponseEntity<IncarnateDescriptionsDto>> getIncarnations(
      @Parameter(required = false, hidden = true) Authentication auth,
      @Parameter(description = "The target unit for the catalog items.") @PathVariable
          String unitId,
      @Parameter(description = "The list of catalog items to create in the unit.")
          @RequestParam(required = true)
          List<String> itemIds) {
    Client client = getAuthenticatedClient(auth);
    Key<UUID> containerId = Key.uuidFrom(unitId);
    List<Key<UUID>> list = itemIds.stream().map(Key::uuidFrom).toList();

    CompletableFuture<IncarnateDescriptionsDto> catalogFuture =
        useCaseInteractor.execute(
            getIncarnationDescriptionUseCase,
            new GetIncarnationDescriptionUseCase.InputData(client, containerId, list),
            output -> new IncarnateDescriptionsDto(output.getReferences(), urlAssembler));
    return catalogFuture.thenApply(result -> ResponseEntity.ok().body(result));
  }

  @PostMapping("/{unitId}/incarnations")
  @Operation(
      summary =
          "Applies an incarnation descriptions to an unit. "
              + "This description need to be obtained by the system via the corresponding "
              + "GET operation for a set of catalog items.",
      tags = "modeling")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Catalog items incarnated.",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array =
                        @ArraySchema(
                            schema =
                                @Schema(
                                    implementation =
                                        IdRefTailoringReferenceParameterReferencedElement.class,
                                    description = "A reference list of the created elements"))))
      })
  public CompletableFuture<ResponseEntity<List<IdRef<Element>>>> applyIncarnations(
      @Parameter(required = false, hidden = true) Authentication auth,
      @Parameter(description = "The target unit for the catalog items.") @PathVariable
          String unitId,
      @Valid @RequestBody IncarnateDescriptionsDto applyInformation) {
    Client client = getAuthenticatedClient(auth);
    CompletableFuture<List<IdRef<Element>>> completableFuture =
        useCaseInteractor.execute(
            applyIncarnationDescriptionUseCase,
            (Supplier<ApplyIncarnationDescriptionUseCase.InputData>)
                () -> {
                  IdRefResolver idRefResolver = createIdRefResolver(client);
                  return new ApplyIncarnationDescriptionUseCase.InputData(
                      client, Key.uuidFrom(unitId), applyInformation.dto2Model(idRefResolver));
                },
            output ->
                output.getNewElements().stream()
                    .map(c -> IdRef.from(c, referenceAssembler))
                    .toList());
    return completableFuture.thenApply(result -> ResponseEntity.status(201).body(result));
  }

  @GetMapping
  @Operation(summary = "Loads all units")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Units loaded",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = FullUnitDto.class))))
      })
  public @Valid Future<List<FullUnitDto>> getUnits(
      @Parameter(required = false, hidden = true) Authentication auth,
      @UnitUuidParam @RequestParam(value = PARENT_PARAM, required = false) String parentUuid,
      @RequestParam(value = DISPLAY_NAME_PARAM, required = false) String displayName) {
    Client client = null;
    try {
      client = getAuthenticatedClient(auth);
    } catch (NoSuchElementException e) {
      return CompletableFuture.supplyAsync(Collections::emptyList);
    }

    // TODO VEO-425 apply display name filter.
    final GetUnitsUseCase.InputData inputData =
        new GetUnitsUseCase.InputData(client, Optional.ofNullable(parentUuid));

    return useCaseInteractor.execute(
        getUnitsUseCase,
        inputData,
        output ->
            output.getUnits().stream()
                .map(u -> entityToDtoTransformer.transformUnit2Dto(u))
                .toList());
  }

  @GetMapping(value = "/{id}")
  @Operation(summary = "Loads a unit")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Unit loaded",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = FullUnitDto.class))),
        @ApiResponse(responseCode = "404", description = "Unit not found")
      })
  public @Valid Future<ResponseEntity<FullUnitDto>> getUnit(
      @Parameter(required = false, hidden = true) Authentication auth,
      @PathVariable String id,
      WebRequest request) {
    if (getEtag(Unit.class, id).map(request::checkNotModified).orElse(false)) {
      return null;
    }
    CompletableFuture<FullUnitDto> unitFuture =
        useCaseInteractor.execute(
            getUnitUseCase,
            new UseCase.IdAndClient(Key.uuidFrom(id), getAuthenticatedClient(auth)),
            output -> entityToDtoTransformer.transformUnit2Dto(output.getUnit()));
    return unitFuture.thenApply(
        unitDto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(unitDto));
  }

  // TODO: veo-279 use the complete dto
  @PostMapping()
  @Operation(summary = "Creates a unit")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Unit created",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = RestApiResponse.class)))
      })
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createUnit(
      @Parameter(hidden = true) ApplicationUser user,
      @Valid @RequestBody CreateUnitDto createUnitDto) {
    return useCaseInteractor.execute(
        createUnitUseCase,
        CreateUnitInputMapper.map(createUnitDto, user.getClientId(), user.getMaxUnits()),
        output -> {
          ApiResponseBody body = CreateOutputMapper.map(output.getUnit());
          return RestApiResponse.created(URL_BASE_PATH, body);
        });
  }

  @PutMapping(value = "/{id}")
  // @Operation(summary = "Updates a unit")
  // @ApiResponses(value = { @ApiResponse(responseCode = "200", description =
  // "Unit updated"),
  // @ApiResponse(responseCode = "404", description = "Unit not found") })
  public CompletableFuture<FullUnitDto> updateUnit(
      @Parameter(hidden = true) ApplicationUser user,
      @RequestHeader(ControllerConstants.IF_MATCH_HEADER) @NotBlank String eTag,
      @PathVariable String id,
      @Valid @RequestBody FullUnitDto unitDto) {
    unitDto.applyResourceId(id);
    return useCaseInteractor.execute(
        putUnitUseCase,
        (Supplier<ChangeUnitUseCase.InputData>)
            () -> {
              Client client = getClient(user);
              IdRefResolver idRefResolver = createIdRefResolver(client);
              return new UpdateUnitUseCase.InputData(
                  dtoToEntityTransformer.transformDto2Unit(unitDto, idRefResolver),
                  client,
                  eTag,
                  user.getUsername());
            },
        output -> entityToDtoTransformer.transformUnit2Dto(output.getUnit()));
  }

  @DeleteMapping(ControllerConstants.UUID_PARAM_SPEC)
  @Operation(summary = "Deletes a unit")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "204", description = "Unit deleted"),
        @ApiResponse(responseCode = "404", description = "Unit not found")
      })
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteUnit(
      @Parameter(required = false, hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String uuid) {

    return useCaseInteractor.execute(
        deleteUnitUseCase,
        new DeleteUnitUseCase.InputData(Key.uuidFrom(uuid), getAuthenticatedClient(auth)),
        output -> RestApiResponse.noContent());
  }

  @Override
  protected String buildSearchUri(String id) {
    return linkTo(methodOn(UnitController.class).runSearch(ANY_AUTH, id)).withSelfRel().getHref();
  }

  @GetMapping(value = "/searches/{searchId}")
  @Operation(summary = "Finds units for the search.")
  public @Valid Future<List<FullUnitDto>> runSearch(
      @Parameter(required = false, hidden = true) Authentication auth,
      @PathVariable String searchId) {
    // TODO VEO-425 Use custom search query DTO & criteria API, apply
    // display name
    // filter and allow recursive parent unit filter.
    try {
      var dto = SearchQueryDto.decodeFromSearchId(searchId);
      // TODO VEO-425 Apply all unit id values (as IN condition).
      if (dto.getUnitId() != null && !dto.getUnitId().values.isEmpty()) {
        return getUnits(auth, dto.getUnitId().getValues().iterator().next(), null);
      }
      return getUnits(auth, null, null);

    } catch (IOException e) {
      log.error("Could not decode search URL: {}", e.getLocalizedMessage());
      throw new IllegalArgumentException("Could not decode search URL.");
    }
  }
}
