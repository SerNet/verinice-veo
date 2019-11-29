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

import org.veo.adapter.presenter.api.dto.UnitDto;
import org.veo.commons.VeoException;
import org.veo.rest.annotations.ParameterUuid;
import org.veo.rest.annotations.ParameterUuidParent;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST service which provides methods to manage units.
 */
@RestController
@RequestMapping(UnitController.URL_BASE_PATH)
public class UnitController {

    public static final String URL_BASE_PATH = "/units";

    @GetMapping
    @Operation(summary = "Loads all units")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Units loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = UnitDto.class))) })
    public @Valid Flux<UnitDto> getUnits(
            @ParameterUuidParent @RequestParam(value = PARENT_PARAM,
                                               required = false) String parentUuid) {
        throw new VeoException(NOT_IMPLEMENTED, "Not implemented yet");
    }

    @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Loads an unit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Unit loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = UnitDto.class))),
            @ApiResponse(responseCode = "404", description = "Unit not found") })
    public @Valid Mono<UnitDto> getUnit(@ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        throw new VeoException(NOT_IMPLEMENTED, "Not implemented yet");
    }

    @PostMapping()
    @Operation(summary = "Creates an unit")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Unit created") })
    public Mono<Resource> createUnit(@Valid @RequestBody UnitDto unitData) {
        throw new VeoException(NOT_IMPLEMENTED, "Not implemented yet");
    }

    @PutMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Updates an unit")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Unit updated"),
            @ApiResponse(responseCode = "404", description = "Unit not found") })
    public Mono<Resource> updateUnit(@ParameterUuid @PathVariable(UUID_PARAM) String uuid,
            @Valid @RequestBody UnitDto unitData) {
        throw new VeoException(NOT_IMPLEMENTED, "Not implemented yet");
    }

    @DeleteMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Deletes an unit")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Unit deleted"),
            @ApiResponse(responseCode = "404", description = "Unit not found") })
    public Mono<Resource> deleteUnit(@ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        throw new VeoException(NOT_IMPLEMENTED, "Not implemented yet");
    }

}
