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

import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.CATEGORY_LIST_RESIZE;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.IMPACT_LINKS;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.IMPACT_LIST_RESIZE;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.IMPLEMENTATION_STATE_LIST_RESIZE;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.PROBABILITY_LIST_RESIZE;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.RISK_MATRIX_DIFF;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.RISK_MATRIX_RESIZE;
import static org.veo.core.entity.riskdefinition.RiskDefinitionChangeType.RISK_VALUE_LIST_RESIZE;
import static org.veo.rest.ControllerConstants.UUID_DESCRIPTION;
import static org.veo.rest.ControllerConstants.UUID_EXAMPLE;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.usecase.domain.SaveRiskDefinitionUseCase;
import org.veo.rest.common.RestApiResponse;
import org.veo.rest.security.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping(ContentCustomizingController.URL_BASE_PATH)
@RequiredArgsConstructor
public class ContentCustomizingController extends AbstractVeoController {
  public static final String URL_BASE_PATH = "/content-customizing";

  private final SaveRiskDefinitionUseCase saveRiskDefinitionUseCase;

  @PutMapping("/domains/{domainId}/risk-definitions/{riskDefinitionId}")
  @Operation(summary = "Create or update a risk definition with given ID")
  @ApiResponse(responseCode = "200", description = "Risk definition updated")
  @ApiResponse(responseCode = "201", description = "Risk definition created")
  @ApiResponse(
      responseCode = "422",
      description = "Requested risk definition modification is not supported yet")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> saveRiskDefinition(
      @Parameter(hidden = true) ApplicationUser user,
      @Parameter(hidden = true) ServletWebRequest request,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(
              required = true,
              example = "DSRA",
              description = "Risk definition identifier - unique within this domain")
          @PathVariable
          String riskDefinitionId,
      @RequestBody @Valid @NotNull RiskDefinition riskDefinition) {
    riskDefinition.setId(riskDefinitionId);
    return useCaseInteractor.execute(
        saveRiskDefinitionUseCase,
        new SaveRiskDefinitionUseCase.InputData(
            UUID.fromString(user.getClientId()),
            domainId,
            riskDefinitionId,
            riskDefinition,
            Set.of(
                PROBABILITY_LIST_RESIZE,
                RISK_MATRIX_DIFF,
                RISK_VALUE_LIST_RESIZE,
                IMPACT_LINKS,
                IMPACT_LIST_RESIZE,
                IMPLEMENTATION_STATE_LIST_RESIZE,
                CATEGORY_LIST_RESIZE,
                RISK_MATRIX_RESIZE)),
        out ->
            out.newRiskDefinition()
                ? RestApiResponse.created(
                    request.getRequest().getRequestURI(), "Risk definition created")
                : RestApiResponse.ok("Risk definition updated"));
  }
}
