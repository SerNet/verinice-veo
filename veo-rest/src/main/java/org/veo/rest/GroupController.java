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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.request.CreateGroupDto;
import org.veo.adapter.presenter.api.response.EntityLayerSupertypeDto;
import org.veo.adapter.presenter.api.response.GroupDto;
import org.veo.adapter.presenter.api.response.groups.AssetGroupDto;
import org.veo.adapter.presenter.api.response.groups.ControlGroupDto;
import org.veo.adapter.presenter.api.response.groups.DocumentGroupDto;
import org.veo.adapter.presenter.api.response.groups.EntityLayerSupertypeGroupDto;
import org.veo.adapter.presenter.api.response.groups.PersonGroupDto;
import org.veo.adapter.presenter.api.response.groups.ProcessGroupDto;
import org.veo.adapter.presenter.api.response.transformer.DtoEntityToTargetContext;
import org.veo.adapter.presenter.api.response.transformer.DtoEntityToTargetTransformer;
import org.veo.adapter.presenter.api.response.transformer.DtoTargetToEntityContext;
import org.veo.adapter.presenter.api.response.transformer.DtoTargetToEntityTransformer;
import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.GroupType;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.impl.BaseModelGroup;
import org.veo.core.usecase.group.CreateGroupUseCase;
import org.veo.core.usecase.group.DeleteGroupUseCase;
import org.veo.core.usecase.group.GetGroupUseCase;
import org.veo.core.usecase.group.GetGroupsUseCase;
import org.veo.core.usecase.group.PutGroupUseCase;
import org.veo.core.usecase.group.UpdateGroupUseCase;
import org.veo.rest.annotations.ParameterUuid;
import org.veo.rest.annotations.ParameterUuidParent;
import org.veo.rest.common.RestApiResponse;
import org.veo.rest.interactor.UseCaseInteractorImpl;
import org.veo.rest.security.ApplicationUser;

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

    private final CreateGroupUseCase createGroupUseCase;
    private final GetGroupUseCase getGroupUseCase;
    private final GetGroupsUseCase<?> getGroupsUseCase;
    private final PutGroupUseCase putGroupUseCase;
    private final DeleteGroupUseCase deleteGroupUseCase;

    public GroupController(UseCaseInteractorImpl useCaseInteractor, ObjectMapper objectMapper,
            CreateGroupUseCase createGroupUseCase, GetGroupUseCase getGroupUseCase,
            GetGroupsUseCase<?> getGroupsUseCase, PutGroupUseCase putGroupUseCase,
            DeleteGroupUseCase deleteGroupUseCase) {
        this.useCaseInteractor = useCaseInteractor;
        this.createGroupUseCase = createGroupUseCase;
        this.getGroupUseCase = getGroupUseCase;
        this.getGroupsUseCase = getGroupsUseCase;
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
                                            array = @ArraySchema(schema = @Schema(implementation = GroupDto.class)))),
            @ApiResponse(responseCode = "404", description = "Group not found") })
    public @Valid CompletableFuture<List<EntityLayerSupertypeGroupDto>> getGroups(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuidParent @RequestParam(value = PARENT_PARAM,
                                               required = false) String parentUuid,
            @RequestParam(value = TYPE_PARAM, required = true) GroupType type) {
        DtoEntityToTargetContext tcontext = DtoEntityToTargetContext.getCompleteTransformationContext();
        return useCaseInteractor.execute(getGroupsUseCase, new GetGroupsUseCase.InputData(
                getAuthenticatedClient(auth), type, Optional.ofNullable(parentUuid)),
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
                                            schema = @Schema(implementation = GroupDto.class))),
            @ApiResponse(responseCode = "404", description = "Group not found") })
    public @Valid CompletableFuture<EntityLayerSupertypeGroupDto<?>> getGroup(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid,
            @RequestParam(value = TYPE_PARAM, required = true) GroupType type) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        Client client = getClient(user.getClientId());
        return useCaseInteractor.execute(getGroupUseCase, new GetGroupUseCase.InputData(
                Key.uuidFrom(uuid), type, client), output -> {
                    DtoEntityToTargetContext tcontext = DtoEntityToTargetContext.getCompleteTransformationContext();
                    tcontext.partialDomain()
                            .partialUnit();
                    return EntityLayerSupertypeGroupDto.from(output.getGroup(), tcontext);
                });
    }

    @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}/members")
    @Operation(summary = "Loads the members of a group")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Members loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = GroupDto.class)))),
            @ApiResponse(responseCode = "404", description = "Group not found") })
    public @Valid CompletableFuture<List<EntityLayerSupertypeDto>> getMembers(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid,
            @RequestParam(value = TYPE_PARAM, required = true) GroupType type) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        Client client = getClient(user.getClientId());
        return useCaseInteractor.execute(getGroupUseCase, new GetGroupUseCase.InputData(
                Key.uuidFrom(uuid), type, client), output -> {
                    DtoEntityToTargetContext tcontext = DtoEntityToTargetContext.getCompleteTransformationContext();
                    tcontext.partialDomain()
                            .partialUnit();
                    BaseModelGroup<?> group = output.getGroup();
                    return group.getMembers()
                                .stream()
                                .map(member -> DtoEntityToTargetTransformer.transformEntityLayerSupertype2Dto(tcontext,
                                                                                                              (EntityLayerSupertype) member))
                                .collect(Collectors.toList());
                });
    }

    @PostMapping()
    @Operation(summary = "Creates a group")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Group created") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> createGroup(
            @Parameter(required = false, hidden = true) Authentication auth,
            @Valid @NotNull @RequestBody CreateGroupDto createGroupDto) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        Client client = getClient(user.getClientId());
        return useCaseInteractor.execute(createGroupUseCase, new CreateGroupUseCase.InputData(
                Key.uuidFrom(createGroupDto.getOwner()
                                           .getId()),
                createGroupDto.getName(), createGroupDto.getType(), client), output -> {
                    BaseModelGroup<?> group = output.getGroup();
                    Optional<String> groupId = group.getId() == null ? Optional.empty()
                            : Optional.ofNullable(group.getId()
                                                       .uuidValue());
                    ApiResponseBody apiResponseBody = new ApiResponseBody(true, groupId,
                            "Group created successfully.");
                    return RestApiResponse.created(URL_BASE_PATH, apiResponseBody);
                });
    }

    @Transactional(TxType.REQUIRED)
    @PutMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Updates a group")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Group updated"),
            @ApiResponse(responseCode = "404", description = "Group not found") })
    public <T extends ModelObject> CompletableFuture<EntityLayerSupertypeGroupDto<T>> updateGroup(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid,
            @NotNull @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                                                                           schema = @Schema(implementation = GroupDto.class))) String requestBody,
            @RequestParam(value = TYPE_PARAM, required = true) GroupType type)
            throws JsonProcessingException {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        Client client = getClient(user.getClientId());
        Class dtoClass = getDtoClass(type);
        EntityLayerSupertypeGroupDto groupDto = (EntityLayerSupertypeGroupDto) objectMapper.readValue(requestBody,
                                                                                                      dtoClass);
        DtoTargetToEntityContext fromDtoContext = configureDtoContext(client,
                                                                      groupDto.getReferences());
        DtoEntityToTargetContext toDtoContext = DtoEntityToTargetContext.getCompleteTransformationContext();
        fromDtoContext.partialDomain()
                      .partialUnit();
        BaseModelGroup<?> group = (BaseModelGroup<?>) DtoTargetToEntityTransformer.transformDto2EntityLayerSupertype(fromDtoContext,
                                                                                                                     (EntityLayerSupertypeDto) groupDto);
        return useCaseInteractor.execute(putGroupUseCase, new UpdateGroupUseCase.InputData(group,
                client), output -> (EntityLayerSupertypeGroupDto<T>) DtoEntityToTargetTransformer.transformEntityLayerSupertype2Dto(toDtoContext, (EntityLayerSupertype) output.getGroup()));
    }

    @DeleteMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Deletes a group")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Group deleted"),
            @ApiResponse(responseCode = "404", description = "Group not found") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteGroup(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid,
            @RequestParam(value = TYPE_PARAM, required = true) GroupType type) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        Client client = getClient(user.getClientId());

        return useCaseInteractor.execute(deleteGroupUseCase,
                                         new DeleteGroupUseCase.InputData(Key.uuidFrom(uuid), type,
                                                 client),
                                         output -> ResponseEntity.ok()
                                                                 .build());
    }

    private Class getDtoClass(GroupType type) {
        switch (type) {
        case Asset:
            return AssetGroupDto.class;
        case Control:
            return ControlGroupDto.class;
        case Document:
            return DocumentGroupDto.class;
        case Person:
            return PersonGroupDto.class;
        case Process:
            return ProcessGroupDto.class;
        default:
            throw new IllegalArgumentException("Unsupported type " + type);
        }
    }
}
