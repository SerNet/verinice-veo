/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Urs Zeidler
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
package org.veo.adapter.presenter.api.dto;

import java.util.List;

import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinitionChange;
import org.veo.core.entity.riskdefinition.RiskDefinitionChangeEffect;
import org.veo.core.usecase.domain.EvaluateRiskDefinitionUseCase.ValidationMessage;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An evaluated riskdefinition with some diagnostic.")
public record RiskDefinitionEvaluationDto(
    @Schema(description = "The Riskdefinition in a semantically correct state.")
        RiskDefinition riskDefinition,
    @Schema(description = "The set of changes to the deployed RD")
        List<RiskDefinitionChange> changes,
    List<ValidationMessage> validationMessages,
    List<RiskDefinitionChangeEffect> effects) {}
