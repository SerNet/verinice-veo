/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
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
import org.veo.adapter.presenter.api.dto.create.CreateIncidentDto;
import org.veo.adapter.presenter.api.dto.full.FullIncidentDto;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.adapter.presenter.api.io.mapper.GetEntitiesInputMapper;
import org.veo.core.entity.Client;
import org.veo.core.entity.EntityTypeNames;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Key;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.core.usecase.base.CreateEntityUseCase;
import org.veo.core.usecase.base.DeleteEntityUseCase;
import org.veo.core.usecase.base.GetEntitiesUseCase;
import org.veo.core.usecase.base.ModifyEntityUseCase.InputData;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.incident.CreateIncidentUseCase;
import org.veo.core.usecase.incident.GetIncidentUseCase;
import org.veo.core.usecase.incident.GetIncidentsUseCase;
import org.veo.core.usecase.incident.UpdateIncidentUseCase;
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
 * REST service which provides methods to manage incidents.
 */
@RestController
@RequestMapping(IncidentController.URL_BASE_PATH)
@Slf4j
public class IncidentController extends AbstractEntityController {

    public IncidentController(UseCaseInteractor useCaseInteractor,
            GetIncidentUseCase getIncidentUseCase, GetIncidentsUseCase getIncidentsUseCase,
            CreateIncidentUseCase createIncidentUseCase,
            UpdateIncidentUseCase updateIncidentUseCase, DeleteEntityUseCase deleteEntityUseCase) {
        this.useCaseInteractor = useCaseInteractor;
        this.getIncidentUseCase = getIncidentUseCase;
        this.getIncidentsUseCase = getIncidentsUseCase;
        this.createIncidentUseCase = createIncidentUseCase;
        this.updateIncidentUseCase = updateIncidentUseCase;
        this.deleteEntityUseCase = deleteEntityUseCase;
    }

    public static final String URL_BASE_PATH = "/" + EntityTypeNames.INCIDENTS;

    private final UseCaseInteractor useCaseInteractor;
    private final CreateIncidentUseCase createIncidentUseCase;
    private final UpdateIncidentUseCase updateIncidentUseCase;
    private final GetIncidentUseCase getIncidentUseCase;
    private final GetIncidentsUseCase getIncidentsUseCase;
    private final DeleteEntityUseCase deleteEntityUseCase;

    @GetMapping
    @Operation(summary = "Loads all incidents")
    public @Valid CompletableFuture<List<FullIncidentDto>> getIncidents(
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

        return getIncidents(GetEntitiesInputMapper.map(client, unitUuid, displayName, subType));
    }

    private CompletableFuture<List<FullIncidentDto>> getIncidents(
            GetEntitiesUseCase.InputData inputData) {
        return useCaseInteractor.execute(getIncidentsUseCase, inputData,
                                         output -> output.getEntities()
                                                         .stream()
                                                         .map(a -> entityToDtoTransformer.transformIncident2Dto(a))
                                                         .collect(Collectors.toList()));
    }

    @GetMapping(value = "/{id}")
    @Operation(summary = "Loads an incident")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Incident loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = FullIncidentDto.class))),
            @ApiResponse(responseCode = "404", description = "Incident not found") })
    public @Valid CompletableFuture<ResponseEntity<FullIncidentDto>> getIncident(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String id) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        Client client = getClient(user.getClientId());

        CompletableFuture<FullIncidentDto> incidentFuture = useCaseInteractor.execute(getIncidentUseCase,
                                                                                      new GetIncidentUseCase.InputData(
                                                                                              Key.uuidFrom(id),
                                                                                              client),
                                                                                      output -> {
                                                                                          return entityToDtoTransformer.transformIncident2Dto(output.getIncident());
                                                                                      });

        return incidentFuture.thenApply(incidentDto -> ResponseEntity.ok()
                                                                     .eTag(ETag.from(incidentDto.getId(),
                                                                                     incidentDto.getVersion()))
                                                                     .body(incidentDto));
    }

    @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}/parts")
    @Operation(summary = "Loads the parts of an incident")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Parts loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = FullIncidentDto.class)))),
            @ApiResponse(responseCode = "404", description = "Incident not found") })
    public @Valid CompletableFuture<List<EntityLayerSupertypeDto>> getParts(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        Client client = getAuthenticatedClient(auth);
        return useCaseInteractor.execute(getIncidentUseCase, new GetIncidentUseCase.InputData(
                Key.uuidFrom(uuid), client), output -> {
                    Incident scope = output.getIncident();
                    return scope.getParts()
                                .stream()
                                .map(part -> entityToDtoTransformer.transform2Dto(part))
                                .collect(Collectors.toList());
                });
    }

    @PostMapping()
    @Operation(summary = "Creates an incident")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Incident created") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> createIncident(
            @Parameter(hidden = true) ApplicationUser user,
            @Valid @NotNull @RequestBody CreateIncidentDto dto) {
        return useCaseInteractor.execute(createIncidentUseCase,
                                         (Supplier<CreateEntityUseCase.InputData<Incident>>) () -> {
                                             Client client = getClient(user);
                                             ModelObjectReferenceResolver modelObjectReferenceResolver = createModelObjectReferenceResolver(client);
                                             return new CreateEntityUseCase.InputData<>(
                                                     dtoToEntityTransformer.transformDto2Incident(dto,
                                                                                                  modelObjectReferenceResolver),
                                                     client, user.getUsername());
                                         }, output -> {
                                             ApiResponseBody body = CreateOutputMapper.map(output.getEntity());
                                             return RestApiResponse.created(URL_BASE_PATH, body);
                                         });
    }

    @PutMapping(value = "/{id}")
    @Operation(summary = "Updates an incident")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Incident updated"),
            @ApiResponse(responseCode = "404", description = "Incident not found") })
    public CompletableFuture<FullIncidentDto> updateIncident(
            @Parameter(hidden = true) ApplicationUser user,
            @RequestHeader(ControllerConstants.IF_MATCH_HEADER) @NotBlank String eTag,
            @PathVariable String id, @Valid @NotNull @RequestBody FullIncidentDto incidentDto) {
        incidentDto.applyResourceId(id);
        return useCaseInteractor.execute(updateIncidentUseCase,
                                         new Supplier<InputData<Incident>>() {

                                             @Override
                                             public InputData<Incident> get() {
                                                 Client client = getClient(user);
                                                 ModelObjectReferenceResolver modelObjectReferenceResolver = createModelObjectReferenceResolver(client);
                                                 return new InputData<Incident>(
                                                         dtoToEntityTransformer.transformDto2Incident(incidentDto,
                                                                                                      modelObjectReferenceResolver),
                                                         client, eTag, user.getUsername());
                                             }
                                         }

                                         ,
                                         output -> entityToDtoTransformer.transformIncident2Dto(output.getEntity()));
    }

    @DeleteMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Deletes an incident")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Incident deleted"),
            @ApiResponse(responseCode = "404", description = "Incident not found") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteIncident(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        Client client = getClient(user.getClientId());
        return useCaseInteractor.execute(deleteEntityUseCase,
                                         new DeleteEntityUseCase.InputData(Incident.class,
                                                 Key.uuidFrom(uuid), client),
                                         output -> ResponseEntity.ok()
                                                                 .build());
    }

    @Override
    protected String buildSearchUri(String id) {
        return linkTo(methodOn(IncidentController.class).runSearch(ANY_AUTH, id)).withSelfRel()
                                                                                 .getHref();
    }

    @GetMapping(value = "/searches/{searchId}")
    @Operation(summary = "Finds incidents for the search.")
    public @Valid CompletableFuture<List<FullIncidentDto>> runSearch(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String searchId) {
        try {
            return getIncidents(GetEntitiesInputMapper.map(getAuthenticatedClient(auth),
                                                           SearchQueryDto.decodeFromSearchId(searchId)));
        } catch (IOException e) {
            log.error(String.format("Could not decode search URL: %s", e.getLocalizedMessage()));
            return null;
        }
    }
}
