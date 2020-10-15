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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.veo.adapter.ModelObjectReferenceResolver;
import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.EntityLayerSupertypeDto;
import org.veo.adapter.presenter.api.dto.FullGroupDto;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.create.CreateGroupDto;
import org.veo.adapter.presenter.api.dto.full.FullAssetGroupDto;
import org.veo.adapter.presenter.api.dto.full.FullControlGroupDto;
import org.veo.adapter.presenter.api.dto.full.FullDocumentGroupDto;
import org.veo.adapter.presenter.api.dto.full.FullEntityLayerSupertypeGroupDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonGroupDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessGroupDto;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoContext;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.Client;
import org.veo.core.entity.EntityTypeNames;
import org.veo.core.entity.GroupType;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelGroup;
import org.veo.core.entity.ModelObject;
import org.veo.core.usecase.common.ETag;
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
 * REST service which provides methods to manage groups.
 */
@RestController
@RequestMapping(GroupController.URL_BASE_PATH)
@Slf4j
public class GroupController extends AbstractEntityController {

    public static final String URL_BASE_PATH = "/" + EntityTypeNames.GROUPS;

    protected static final String TYPE_PARAM = "type";

    private final UseCaseInteractorImpl useCaseInteractor;
    private final ObjectMapper objectMapper;

    private final CreateGroupUseCase<ResponseEntity<ApiResponseBody>> createGroupUseCase;
    private final GetGroupUseCase<FullEntityLayerSupertypeGroupDto<?>> getGroupUseCase;
    private final GetGroupsUseCase<?, List<FullEntityLayerSupertypeGroupDto<?>>> getGroupsUseCase;
    private final GetGroupUseCase<List<EntityLayerSupertypeDto>> getGroupMemberUseCase;
    private final PutGroupUseCase<FullEntityLayerSupertypeGroupDto<?>> putGroupUseCase;
    private final DeleteGroupUseCase deleteGroupUseCase;
    private final ModelObjectReferenceResolver referenceResolver;

    public GroupController(UseCaseInteractorImpl useCaseInteractor, ObjectMapper objectMapper,
            CreateGroupUseCase createGroupUseCase,
            GetGroupUseCase<FullEntityLayerSupertypeGroupDto<?>> getGroupUseCase,
            GetGroupsUseCase<?, List<FullEntityLayerSupertypeGroupDto<?>>> getGroupsUseCase,
            GetGroupUseCase<List<EntityLayerSupertypeDto>> getGroupMemberUseCase,
            PutGroupUseCase putGroupUseCase, DeleteGroupUseCase deleteGroupUseCase,
            ModelObjectReferenceResolver referenceResolver) {
        this.useCaseInteractor = useCaseInteractor;
        this.createGroupUseCase = createGroupUseCase;
        this.getGroupUseCase = getGroupUseCase;
        this.getGroupsUseCase = getGroupsUseCase;
        this.getGroupMemberUseCase = getGroupMemberUseCase;
        this.putGroupUseCase = putGroupUseCase;
        this.deleteGroupUseCase = deleteGroupUseCase;
        this.objectMapper = objectMapper;
        this.referenceResolver = referenceResolver;
    }

    @GetMapping
    @Operation(summary = "Loads all groups")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Members loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = FullGroupDto.class)))),
            @ApiResponse(responseCode = "404", description = "Group not found") })
    public @Valid CompletableFuture<List<FullEntityLayerSupertypeGroupDto<?>>> getGroups(
            @Parameter(required = false, hidden = true) Authentication auth,
            @UnitUuidParam @RequestParam(value = UNIT_PARAM, required = false) String unitUuid,
            @RequestParam(value = TYPE_PARAM, required = true) GroupType type) {
        Client client = null;
        try {
            client = getAuthenticatedClient(auth);
        } catch (NoSuchElementException e) {
            return CompletableFuture.supplyAsync(Collections::emptyList);
        }

        final GetGroupsUseCase.InputData inputData = new GetGroupsUseCase.InputData(client, type,
                Optional.ofNullable(unitUuid));
        EntityToDtoContext tcontext = EntityToDtoContext.getCompleteTransformationContext(referenceAssembler);
        return useCaseInteractor.execute(getGroupsUseCase, inputData, output -> output.getGroups()
                                                                                      .stream()
                                                                                      .map(u -> FullEntityLayerSupertypeGroupDto.from(u,
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
    public @Valid CompletableFuture<ResponseEntity<FullEntityLayerSupertypeGroupDto<?>>> getGroup(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid,
            @RequestParam(value = TYPE_PARAM, required = true) GroupType type) {
        Client client = getAuthenticatedClient(auth);

        CompletableFuture<FullEntityLayerSupertypeGroupDto<?>> groupFuture = useCaseInteractor.execute(getGroupUseCase,
                                                                                                       new GetGroupUseCase.InputData(
                                                                                                               Key.uuidFrom(uuid),
                                                                                                               type,
                                                                                                               client),
                                                                                                       output -> {
                                                                                                           EntityToDtoContext tcontext = EntityToDtoContext.getCompleteTransformationContext(referenceAssembler);
                                                                                                           return FullEntityLayerSupertypeGroupDto.from(output.getGroup(),
                                                                                                                                                        tcontext);
                                                                                                       });
        return groupFuture.thenApply(groupDto -> ResponseEntity.ok()
                                                               .eTag(ETag.from(groupDto.getId(),
                                                                               groupDto.getVersion()))
                                                               .body(groupDto));
    }

    @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}/members")
    @Operation(summary = "Loads the members of a group")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Members loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = FullGroupDto.class)))),
            @ApiResponse(responseCode = "404", description = "Group not found") })
    public @Valid CompletableFuture<List<EntityLayerSupertypeDto>> getMembers(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid,
            @RequestParam(value = TYPE_PARAM, required = true) GroupType type) {
        Client client = getAuthenticatedClient(auth);
        return useCaseInteractor.execute(getGroupMemberUseCase, new GetGroupUseCase.InputData(
                Key.uuidFrom(uuid), type, client), output -> {
                    EntityToDtoContext tcontext = EntityToDtoContext.getCompleteTransformationContext(referenceAssembler);
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
            @Parameter(hidden = true) ApplicationUser user,
            @Valid @NotNull @RequestBody CreateGroupDto createGroupDto) {
        Client client = getClient(user);
        return useCaseInteractor.execute(createGroupUseCase, new CreateGroupUseCase.InputData(
                Key.uuidFrom(createGroupDto.getOwner()
                                           .getId()),
                createGroupDto.getName(), createGroupDto.getType(), client, user.getUsername()),
                                         output -> {
                                             ModelGroup<?> group = output.getGroup();
                                             Optional<String> groupId = group.getId() == null
                                                     ? Optional.empty()
                                                     : Optional.ofNullable(group.getId()
                                                                                .uuidValue());
                                             ApiResponseBody apiResponseBody = new ApiResponseBody(
                                                     true, groupId, "Group created successfully.");
                                             return RestApiResponse.created(URL_BASE_PATH,
                                                                            apiResponseBody);
                                         });
    }

    // TODO: veo-281
    @PutMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Updates a group")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Group updated"),
            @ApiResponse(responseCode = "404", description = "Group not found") })
    public <T extends ModelObject> CompletableFuture<FullEntityLayerSupertypeGroupDto<?>> updateGroup(
            @Parameter(hidden = true) ApplicationUser user,
            @RequestHeader(ControllerConstants.IF_MATCH_HEADER) @NotBlank String eTag,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid,
            @NotNull @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                                                                           schema = @Schema(implementation = FullGroupDto.class))) String requestBody,
            @RequestParam(value = TYPE_PARAM, required = true) GroupType type)
            throws JsonProcessingException {
        Class dtoClass = getFullDtoClass(type);
        var groupDto = (FullEntityLayerSupertypeGroupDto<?>) objectMapper.readValue(requestBody,
                                                                                    dtoClass);
        applyId(uuid, groupDto);
        return useCaseInteractor.execute(putGroupUseCase,
                                         (Supplier<UpdateGroupUseCase.InputData>) () -> {
                                             Client client = getClient(user);
                                             DtoToEntityContext context = referenceResolver.loadIntoContext(client,
                                                                                                            groupDto.getReferences());
                                             return new UpdateGroupUseCase.InputData(
                                                     groupDto.toEntity(context), client, eTag,
                                                     user.getUsername());
                                         }, output -> {
                                             return FullEntityLayerSupertypeGroupDto.from(output.getGroup(),
                                                                                          EntityToDtoContext.getCompleteTransformationContext(referenceAssembler));
                                         });
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

    @Override
    @SuppressFBWarnings // ignore warning on call to method proxy factory
    protected String buildSearchUri(String id) {
        return linkTo(methodOn(GroupController.class).runSearch(ANY_AUTH, id)).withSelfRel()
                                                                              .getHref();
    }

    @GetMapping(value = "/searches/{searchId}")
    @Operation(summary = "Finds groups for the search.")
    public @Valid CompletableFuture<List<FullEntityLayerSupertypeGroupDto<?>>> runSearch(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String searchId) {
        // TODO VEO-38 replace this placeholder implementation with a search usecase:
        try {
            var query = SearchQueryDto.decodeFromSearchId(searchId);
            return getGroups(auth, query.getUnitId(), query.getGroupType());
        } catch (IOException e) {
            log.error(String.format("Could not decode search URL: %s", e.getLocalizedMessage()));
            return null;
        }
    }
}
