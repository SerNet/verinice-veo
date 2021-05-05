/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.full.ProcessRiskDto;
import org.veo.rest.security.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@SecurityRequirement(name = RestApplication.SECURITY_SCHEME_OAUTH)
public interface ProcessRiskResource {

    String RESOURCE_NAME = "risks";

    String RELPATH = "/{processId}/" + RESOURCE_NAME;

    @GetMapping(value = RELPATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Returns all risks for this process")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Risks returned",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) })
    @Valid
    CompletableFuture<List<ProcessRiskDto>> getRisks(@Parameter(hidden = true) ApplicationUser user,
            @PathVariable String processId);

    @GetMapping(value = RELPATH + "/{scenarioId}")
    @Operation(summary = "Retrieves an process risk")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Risk retrieved",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ProcessRiskDto.class))),
            @ApiResponse(responseCode = "404", description = "Risk not found") })
    @Valid
    CompletableFuture<ResponseEntity<ProcessRiskDto>> getRisk(
            @Parameter(hidden = true) ApplicationUser user, @PathVariable String processId,
            @PathVariable String scenarioId);

    @Operation(summary = "Creates a risk")
    @PostMapping(value = RELPATH)
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Risk created") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> createRisk(
            @Parameter(hidden = true) ApplicationUser user,
            @Valid @NotNull @RequestBody ProcessRiskDto dto, @PathVariable String processId);

    @PutMapping(value = RELPATH + "/{scenarioId}")
    @Operation(summary = "Updates a risk")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Risk updated"),
            @ApiResponse(responseCode = "404", description = "Risk not found") })
    public @Valid CompletableFuture<ResponseEntity<ProcessRiskDto>> updateRisk(
            @Parameter(hidden = true) ApplicationUser user, @PathVariable String processId,
            @PathVariable String scenarioId, @Valid @NotNull @RequestBody ProcessRiskDto processDto,
            @RequestHeader(ControllerConstants.IF_MATCH_HEADER) @NotBlank String eTag);

    @DeleteMapping(value = RELPATH + "/{scenarioId}")
    @Operation(summary = "Removes a risk")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Risk deleted"),
            @ApiResponse(responseCode = "404", description = "Risk not found") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteRisk(
            @Parameter(hidden = true) ApplicationUser user, @PathVariable String processId,
            @PathVariable String scenarioId);

}
