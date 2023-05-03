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

import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.ProcessRisk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/** Risk values to be used as input for a use case. */
@Data
@AllArgsConstructor
@Builder
public class RiskValues implements RiskValuesProvider {

  @Valid private Probability probability;

  @Valid private List<Impact> impactCategories;

  @Valid private List<DeterminedRisk> categorizedRisks;

  @Valid private Key<String> riskDefinitionId;

  @Valid private Key<UUID> domainId;

  public static RiskValues from(ProcessRisk risk, RiskDefinitionRef riskDef, Domain domain) {
    return builder()
        .probability(risk.getProbabilityProvider(riskDef, domain))
        .impactCategories(risk.getImpactProvider(riskDef, domain).getCategorizedImpacts())
        .categorizedRisks(risk.getRiskProvider(riskDef, domain).getCategorizedRisks())
        .domainId(domain.getId())
        .riskDefinitionId(new Key<>(riskDef.getIdRef()))
        .build();
  }
}
