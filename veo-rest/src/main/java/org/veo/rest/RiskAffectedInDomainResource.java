/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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

import static org.veo.rest.ControllerConstants.PAGE_NUMBER_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.PAGE_NUMBER_PARAM;
import static org.veo.rest.ControllerConstants.PAGE_SIZE_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.PAGE_SIZE_PARAM;
import static org.veo.rest.ControllerConstants.SORT_COLUMN_PARAM;
import static org.veo.rest.ControllerConstants.SORT_ORDER_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.SORT_ORDER_PARAM;
import static org.veo.rest.ControllerConstants.SORT_ORDER_PATTERN;
import static org.veo.rest.ControllerConstants.UUID_DESCRIPTION;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.dto.RequirementImplementationDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

public interface RiskAffectedInDomainResource {

  @GetMapping("/{riskAffectedId}/control-implementations/{controlId}/requirement-implementations")
  @Operation(summary = "Retrieve all requirement implementations for an implemented control")
  @ApiResponse(responseCode = "200", description = "Requirement implementations loaded")
  @ApiResponse(
      responseCode = "404",
      description =
          "Risk-affected element or control not found, or control not implemented or not associated with domain")
  Future<PageDto<RequirementImplementationDto>> getRequirementImplementations(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, description = UUID_DESCRIPTION) @PathVariable UUID domainId,
      @Parameter(required = true, description = UUID_DESCRIPTION) @PathVariable UUID riskAffectedId,
      @Parameter(required = true, description = UUID_DESCRIPTION) @PathVariable UUID controlId,
      @RequestParam(value = "controlCustomAspects", required = false)
          List<String> controlCustomAspectKeys,
      @RequestParam(
              value = PAGE_SIZE_PARAM,
              required = false,
              defaultValue = PAGE_SIZE_DEFAULT_VALUE)
          @Min(1)
          Integer pageSize,
      @RequestParam(
              value = PAGE_NUMBER_PARAM,
              required = false,
              defaultValue = PAGE_NUMBER_DEFAULT_VALUE)
          Integer pageNumber,
      @RequestParam(value = SORT_COLUMN_PARAM, required = false, defaultValue = "status")
          @Parameter(
              schema =
                  @Schema(
                      allowableValues = {
                        "status",
                        "implementationStatement",
                        "control.abbreviation"
                      }))
          String sortColumn,
      @RequestParam(
              value = SORT_ORDER_PARAM,
              required = false,
              defaultValue = SORT_ORDER_DEFAULT_VALUE)
          @Pattern(regexp = SORT_ORDER_PATTERN)
          String sortOrder);
}
