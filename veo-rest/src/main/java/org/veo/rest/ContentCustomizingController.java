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

import static org.veo.rest.ControllerConstants.UUID_DESCRIPTION;
import static org.veo.rest.ControllerConstants.UUID_EXAMPLE;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.RiskDefinitionEvaluationDto;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinitionChange;
import org.veo.core.usecase.domain.EvaluateRiskDefinitionUseCase;
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
  public static final Set<Class<? extends RiskDefinitionChange>> ALLOWED_CHANGES =
      Set.of(
          RiskDefinitionChange.ColorDiff.class,
          RiskDefinitionChange.NewRiskDefinition.class,
          RiskDefinitionChange.RiskMatrixAdd.class,
          RiskDefinitionChange.RiskMatrixDiff.class,
          RiskDefinitionChange.RiskMatrixRemove.class,
          RiskDefinitionChange.RiskMatrixResize.class,
          RiskDefinitionChange.ImpactListResize.class,
          RiskDefinitionChange.CategoryListAdd.class,
          RiskDefinitionChange.CategoryListRemove.class,
          RiskDefinitionChange.ProbabilityListResize.class,
          RiskDefinitionChange.RiskValueListResize.class,
          RiskDefinitionChange.TranslationDiff.class);

  private final SaveRiskDefinitionUseCase saveRiskDefinitionUseCase;
  private final EvaluateRiskDefinitionUseCase evaluateRiskDefinitionUseCase;

  @PostMapping("/domains/{domainId}/risk-definitions/{riskDefinitionId}/evaluation")
  @Operation(
      summary =
          "EXPERIMENTAL API - Evaluates a new or modified risk definition, without persisting anything. Compares given risk definition with the currently persisted version (if any) and returns the transient risk definition with auto-resized risk matrices, validation messages and the predicted effects of saving that risk definition.")
  @ApiResponse(responseCode = "200", description = "Risk definition evaluated")
  @ApiResponse(
      responseCode = "422",
      description = "Requested risk definition modification is not supported yet")
  public CompletableFuture<ResponseEntity<RiskDefinitionEvaluationDto>> evaluateRiskDefinition(
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
        evaluateRiskDefinitionUseCase,
        new EvaluateRiskDefinitionUseCase.InputData(
            UUID.fromString(user.getClientId()),
            domainId,
            riskDefinitionId,
            riskDefinition,
            ALLOWED_CHANGES),
        out ->
            ResponseEntity.ok(
                new RiskDefinitionEvaluationDto(
                    out.riskDefinition(), out.detectedChanges(), out.message(), out.effects())));
  }

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
            ALLOWED_CHANGES),
        out ->
            out.newRiskDefinition()
                ? RestApiResponse.created(
                    request.getRequest().getRequestURI(), "Risk definition created")
                : RestApiResponse.ok("Risk definition updated"));
  }
}
