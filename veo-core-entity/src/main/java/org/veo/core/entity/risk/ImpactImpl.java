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

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Valid
public class ImpactImpl implements Impact {

  @Setter(AccessLevel.NONE)
  @NonNull
  private CategoryRef category;

  private ImpactRef potentialImpact;

  private ImpactRef specificImpact;

  @Setter(AccessLevel.NONE)
  private ImpactRef effectiveImpact;

  @Size(max = Impact.EXPLANATION_MAX_LENGTH)
  private String specificImpactExplanation;

  @Override
  public void setPotentialImpact(ImpactRef potentialImpact) {
    this.potentialImpact = potentialImpact;
    updateEffectiveImpact();
  }

  @Override
  public void setSpecificImpact(ImpactRef specificImpact) {
    this.specificImpact = specificImpact;
    updateEffectiveImpact();
  }

  private void updateEffectiveImpact() {
    if (specificImpact != null) effectiveImpact = specificImpact;
    else effectiveImpact = potentialImpact;
  }
}
