/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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
package org.veo.adapter.presenter.api.openapi;

import java.math.BigDecimal;

import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.Impact;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Impact", description = "A collection of impact values for a risk category.")
public interface ImpactSchema {

  @Schema(
      description =
          "The impact of a scenario in a particular circumstance. A scalar value that matches a valid impact level from a risk-definition.",
      example = "2")
  @PositiveOrZero
  BigDecimal getSpecificImpact();

  @Schema(
      description =
          "The effective impact where a specific impact takes precedence over a potential impact. A scalar value that matches a valid probability level from a risk-definition.",
      example = "3",
      accessMode = Schema.AccessMode.READ_ONLY)
  @PositiveOrZero
  BigDecimal getEffectiveImpact();

  @Schema(
      description = "A user-provided explanation for the choice of specific impact.",
      example =
          "While a fire will usually damage a computer in a serious way, our server cases are made out of asbestos.")
  @Size(max = Impact.EXPLANATION_MAX_LENGTH)
  String getSpecificImpactExplanation();

  @Schema(
      description =
          "A scalar value that matches a valid risk category from a risk-definition, such as confidentiality, integrity, availability...",
      example = "C")
  @Size(max = CategoryRef.MAX_ID_LENGTH)
  String getCategory();

  @Schema(
      description =
          "The potential impact value derived from the entity associated with "
              + "this risk. A scalar value that matches a valid impact level from a risk-definition.",
      example = "3",
      accessMode = Schema.AccessMode.READ_ONLY)
  @PositiveOrZero
  BigDecimal getPotentialImpact();
}
