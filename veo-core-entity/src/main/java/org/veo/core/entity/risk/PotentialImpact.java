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
package org.veo.core.entity.risk;

import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The potential impact on one of the information security properties of an asset (or a
 * process/scope).
 *
 * <p>This is taken from the risk-affected entity (i.e. process) and does not yet take individual
 * scenarios into account. It is solely based on evaluation the data processed by/on the process/the
 * asset.
 */
public interface PotentialImpact {

  @Schema(
      description =
          "The potential impact value derived from the entity associated with "
              + "this risk. A scalar value that matches a valid impact level from a risk-definition.",
      example = "3",
      accessMode = Schema.AccessMode.READ_ONLY)
  ImpactRef getPotentialImpact();

  void setPotentialImpact(ImpactRef potential);

  @Schema(
      description =
          "A scalar value that matches a valid risk category from a risk-definition, such as confidentiality, integrity, availability...",
      example = "C")
  @Size(max = CategoryRef.MAX_ID_LENGTH)
  CategoryRef getCategory();
}
