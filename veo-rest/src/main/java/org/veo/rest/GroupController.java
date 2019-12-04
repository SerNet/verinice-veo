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

import static org.veo.commons.VeoException.Error.NOT_IMPLEMENTED;
import static org.veo.rest.ControllerConstants.PARENT_PARAM;
import static org.veo.rest.ControllerConstants.UUID_PARAM;
import static org.veo.rest.ControllerConstants.UUID_REGEX;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.veo.adapter.presenter.api.dto.GroupDto;
import org.veo.commons.VeoException;
import org.veo.rest.annotations.ParameterUuid;
import org.veo.rest.annotations.ParameterUuidParent;
import org.veo.rest.annotations.group.ParameterType;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST service which provides methods to manage groups.
 */
@RestController
@RequestMapping(GroupController.URL_BASE_PATH)
public class GroupController {

    public static final String URL_BASE_PATH = "/groups";

    protected static final String TYPE_PARAM = "type";

    @GetMapping
    @Operation(summary = "Loads all groups")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Groups loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = GroupDto.class))) })
    public @Valid Flux<GroupDto> getGroups(
            @ParameterUuidParent @RequestParam(value = PARENT_PARAM,
                                               required = false) String parentUuid,
            @ParameterType @RequestParam(value = TYPE_PARAM, required = false) String type) {
        throw new VeoException(NOT_IMPLEMENTED, "Not implemented yet");
    }

    @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Loads a group")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Group loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = GroupDto.class))),
            @ApiResponse(responseCode = "404", description = "Group not found") })
    public @Valid Mono<GroupDto> getGroup(@ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        throw new VeoException(NOT_IMPLEMENTED, "Not implemented yet");
    }

    @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}/children")
    @Operation(summary = "Loads the children of a group")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Children loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = GroupDto.class))),
            @ApiResponse(responseCode = "404", description = "Group not found") })
    public Flux<GroupDto> getChildren(@ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        throw new VeoException(NOT_IMPLEMENTED, "Not implemented yet");
    }

    @PostMapping()
    @Operation(summary = "Creates a group")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Group created") })
    public Mono<Resource> createGroup(@Valid @NotNull @RequestBody GroupDto groupData) {
        throw new VeoException(NOT_IMPLEMENTED, "Not implemented yet");
    }

    @PutMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Updates a group")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Group updated"),
            @ApiResponse(responseCode = "404", description = "Group not found") })
    public Mono<Resource> updateGroup(@ParameterUuid @PathVariable(UUID_PARAM) String uuid,
            @Valid @NotNull @RequestBody GroupDto groupData) {
        throw new VeoException(NOT_IMPLEMENTED, "Not implemented yet");
    }

    @DeleteMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Deletes a group")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Group deleted"),
            @ApiResponse(responseCode = "404", description = "Group not found") })
    public Mono<Resource> deleteGroup(@ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        throw new VeoException(NOT_IMPLEMENTED, "Not implemented yet");
    }

}
