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

import static org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer.transformGroup2Dto;
import static org.veo.rest.ControllerConstants.UNIT_PARAM;
import static org.veo.rest.ControllerConstants.UUID_PARAM;
import static org.veo.rest.ControllerConstants.UUID_REGEX;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.EntityLayerSupertypeDto;
import org.veo.adapter.presenter.api.dto.EntityLayerSupertypeGroupDto;
import org.veo.adapter.presenter.api.dto.FullGroupDto;
import org.veo.adapter.presenter.api.dto.create.CreateGroupDto;
import org.veo.adapter.presenter.api.dto.full.FullAssetGroupDto;
import org.veo.adapter.presenter.api.dto.full.FullControlGroupDto;
import org.veo.adapter.presenter.api.dto.full.FullCustomLinkDto;
import org.veo.adapter.presenter.api.dto.full.FullCustomPropertiesDto;
import org.veo.adapter.presenter.api.dto.full.FullDocumentGroupDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonGroupDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessGroupDto;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoContext;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.Client;
import org.veo.core.entity.GroupType;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelGroup;
import org.veo.core.entity.ModelObject;
import org.veo.core.usecase.group.CreateGroupUseCase;
import org.veo.core.usecase.group.DeleteGroupUseCase;
import org.veo.core.usecase.group.GetGroupUseCase;
import org.veo.core.usecase.group.GetGroupsUseCase;
import org.veo.core.usecase.group.PutGroupUseCase;
import org.veo.core.usecase.group.UpdateGroupUseCase;
import org.veo.rest.annotations.ParameterUuid;
import org.veo.rest.annotations.UnitUuidParam;
import org.veo.rest.common.RestApiResponse;
import org.veo.rest.interactor.UseCaseInteractorImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * REST service which provides methods to manage groups.
 */
@RestController
@RequestMapping(GroupController.URL_BASE_PATH)
public class GroupController extends AbstractEntityController {

    public static final String URL_BASE_PATH = "/groups";

    protected static final String TYPE_PARAM = "type";

    private final UseCaseInteractorImpl useCaseInteractor;
    private final ObjectMapper objectMapper;

    private final CreateGroupUseCase<ResponseEntity<ApiResponseBody>> createGroupUseCase;
    private final GetGroupUseCase<EntityLayerSupertypeGroupDto<?, FullCustomPropertiesDto, FullCustomLinkDto>> getGroupUseCase;
    private final GetGroupsUseCase<?, List<EntityLayerSupertypeGroupDto<?, FullCustomPropertiesDto, FullCustomLinkDto>>> getGroupsUseCase;
    private final GetGroupUseCase<List<EntityLayerSupertypeDto<FullCustomPropertiesDto, FullCustomLinkDto>>> getGroupMemberUseCase;
    private final PutGroupUseCase<EntityLayerSupertypeGroupDto<?, FullCustomPropertiesDto, FullCustomLinkDto>> putGroupUseCase;
    private final DeleteGroupUseCase deleteGroupUseCase;

    public GroupController(UseCaseInteractorImpl useCaseInteractor, ObjectMapper objectMapper,
            CreateGroupUseCase createGroupUseCase,
            GetGroupUseCase<EntityLayerSupertypeGroupDto<?, FullCustomPropertiesDto, FullCustomLinkDto>> getGroupUseCase,
            GetGroupsUseCase<?, List<EntityLayerSupertypeGroupDto<?, FullCustomPropertiesDto, FullCustomLinkDto>>> getGroupsUseCase,
            GetGroupUseCase<List<EntityLayerSupertypeDto<FullCustomPropertiesDto, FullCustomLinkDto>>> getGroupMemberUseCase,
            PutGroupUseCase putGroupUseCase, DeleteGroupUseCase deleteGroupUseCase) {
        this.useCaseInteractor = useCaseInteractor;
        this.createGroupUseCase = createGroupUseCase;
        this.getGroupUseCase = getGroupUseCase;
        this.getGroupsUseCase = getGroupsUseCase;
        this.getGroupMemberUseCase = getGroupMemberUseCase;
        this.putGroupUseCase = putGroupUseCase;
        this.deleteGroupUseCase = deleteGroupUseCase;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @Operation(summary = "Loads all groups")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Members loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = FullGroupDto.class)))),
            @ApiResponse(responseCode = "404", description = "Group not found") })
    public @Valid CompletableFuture<List<EntityLayerSupertypeGroupDto<?, FullCustomPropertiesDto, FullCustomLinkDto>>> getGroups(
            @Parameter(required = false, hidden = true) Authentication auth,
            @UnitUuidParam @RequestParam(value = UNIT_PARAM, required = false) String unitUuid,
            @RequestParam(value = TYPE_PARAM, required = true) GroupType type) {
        EntityToDtoContext tcontext = EntityToDtoContext.getCompleteTransformationContext();
        return useCaseInteractor.execute(getGroupsUseCase, new GetGroupsUseCase.InputData(
                getAuthenticatedClient(auth), type, Optional.ofNullable(unitUuid)),
                                         output -> output.getGroups()
                                                         .stream()
                                                         .map(u -> EntityLayerSupertypeGroupDto.from(u,
                                                                                                     tcontext))
                                                         .collect(Collectors.toList()));

    }

    @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Loads a group")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Group loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = FullGroupDto.class))),
            @ApiResponse(responseCode = "404", description = "Group not found") })
    public @Valid CompletableFuture<EntityLayerSupertypeGroupDto<?, FullCustomPropertiesDto, FullCustomLinkDto>> getGroup(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid,
            @RequestParam(value = TYPE_PARAM, required = true) GroupType type) {
        Client client = getAuthenticatedClient(auth);
        return useCaseInteractor.execute(getGroupUseCase, new GetGroupUseCase.InputData(
                Key.uuidFrom(uuid), type, client), output -> {
                    EntityToDtoContext tcontext = EntityToDtoContext.getCompleteTransformationContext();
                    return EntityLayerSupertypeGroupDto.from(output.getGroup(), tcontext);
                });
    }

    @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}/members")
    @Operation(summary = "Loads the members of a group")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Members loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = FullGroupDto.class)))),
            @ApiResponse(responseCode = "404", description = "Group not found") })
    public @Valid CompletableFuture<List<EntityLayerSupertypeDto<FullCustomPropertiesDto, FullCustomLinkDto>>> getMembers(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid,
            @RequestParam(value = TYPE_PARAM, required = true) GroupType type) {
        Client client = getAuthenticatedClient(auth);
        return useCaseInteractor.execute(getGroupMemberUseCase, new GetGroupUseCase.InputData(
                Key.uuidFrom(uuid), type, client), output -> {
                    EntityToDtoContext tcontext = EntityToDtoContext.getCompleteTransformationContext();
                    ModelGroup<?> group = output.getGroup();
                    return group.getMembers()
                                .stream()
                                .map(member -> EntityToDtoTransformer.transform2Dto(tcontext,
                                                                                    member))
                                .collect(Collectors.toList());
                });
    }

    // TODO: veo-281
    @PostMapping()
    @Operation(summary = "Creates a group")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Group created") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> createGroup(
            @Parameter(required = false, hidden = true) Authentication auth,
            @Valid @NotNull @RequestBody CreateGroupDto createGroupDto) {
        Client client = getAuthenticatedClient(auth);
        return useCaseInteractor.execute(createGroupUseCase, new CreateGroupUseCase.InputData(
                Key.uuidFrom(createGroupDto.getOwner()
                                           .getId()),
                createGroupDto.getName(), createGroupDto.getType(), client), output -> {
                    ModelGroup<?> group = output.getGroup();
                    Optional<String> groupId = group.getId() == null ? Optional.empty()
                            : Optional.ofNullable(group.getId()
                                                       .uuidValue());
                    ApiResponseBody apiResponseBody = new ApiResponseBody(true, groupId,
                            "Group created successfully.");
                    return RestApiResponse.created(URL_BASE_PATH, apiResponseBody);
                });
    }

    // TODO: veo-281
    @PutMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Updates a group")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Group updated"),
            @ApiResponse(responseCode = "404", description = "Group not found") })
    public <T extends ModelObject> CompletableFuture<EntityLayerSupertypeGroupDto<?, FullCustomPropertiesDto, FullCustomLinkDto>> updateGroup(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid,
            @NotNull @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                                                                           schema = @Schema(implementation = FullGroupDto.class))) String requestBody,
            @RequestParam(value = TYPE_PARAM, required = true) GroupType type)
            throws JsonProcessingException {
        Client client = getAuthenticatedClient(auth);
        Class dtoClass = getFullDtoClass(type);
        var groupDto = (EntityLayerSupertypeGroupDto<?, FullCustomPropertiesDto, FullCustomLinkDto>) objectMapper.readValue(requestBody,
                                                                                                                            dtoClass);
        DtoToEntityContext fromDtoContext = configureDtoContext(client, groupDto.getReferences());
        EntityToDtoContext toDtoContext = EntityToDtoContext.getCompleteTransformationContext();
        ModelGroup<?> group = groupDto.toEntity(fromDtoContext);
        return useCaseInteractor.execute(putGroupUseCase,
                                         new UpdateGroupUseCase.InputData(group, client), output -> // TODO:
                                                                                                    // do
                                                                                                    // we
                                                                                                    // need
                                                                                                    // the
                                                                                                    // transform
                                                                                                    // group
                                                                                                    // funtion
                                         transformGroup2Dto(toDtoContext, output.getGroup()));
    }

    @DeleteMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Deletes a group")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Group deleted"),
            @ApiResponse(responseCode = "404", description = "Group not found") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteGroup(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid,
            @RequestParam(value = TYPE_PARAM, required = true) GroupType type) {
        Client client = getAuthenticatedClient(auth);

        return useCaseInteractor.execute(deleteGroupUseCase,
                                         new DeleteGroupUseCase.InputData(Key.uuidFrom(uuid), type,
                                                 client),
                                         output -> ResponseEntity.ok()
                                                                 .build());
    }

    private Class getFullDtoClass(GroupType type) {
        switch (type) {
        case Asset:
            return FullAssetGroupDto.class;
        case Control:
            return FullControlGroupDto.class;
        case Document:
            return FullDocumentGroupDto.class;
        case Person:
            return FullPersonGroupDto.class;
        case Process:
            return FullProcessGroupDto.class;
        default:
            throw new IllegalArgumentException("Unsupported type " + type);
        }
    }
}
