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

import java.math.BigDecimal;
import java.util.Optional;

import org.veo.core.entity.DomainTemplate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DomainRiskReferenceProvider extends RiskReferenceProvider {

  @Getter private DomainTemplate domain;

  public static DomainRiskReferenceProvider referencesForDomain(DomainTemplate domain) {
    return new DomainRiskReferenceProvider(domain);
  }

  @Override
  public Optional<RiskRef> getRiskRef(String riskDefinitionId, BigDecimal ordinalValue) {
    return domain
        .getRiskDefinition(riskDefinitionId)
        .orElseThrow()
        .getRiskValue(ordinalValue.intValue())
        .map(RiskRef::from);
  }

  @Override
  public Optional<ProbabilityRef> getProbabilityRef(
      String riskDefinitionId, BigDecimal probabilityId) {
    return domain
        .getRiskDefinition(riskDefinitionId)
        .orElseThrow()
        .getProbability()
        .getLevel(probabilityId.intValue())
        .map(ProbabilityRef::from);
  }

  @Override
  public Optional<ImpactRef> getImpactRef(
      String riskDefinitionId, String category, BigDecimal impactId) {
    return domain
        .getRiskDefinition(riskDefinitionId)
        .orElseThrow()
        .getCategory(category)
        .orElseThrow()
        .getLevel(impactId.intValue())
        .map(ImpactRef::from);
  }

  @Override
  public Optional<CategoryRef> getCategoryRef(String riskDefinitionId, String categoryId) {
    return domain
        .getRiskDefinition(riskDefinitionId)
        .orElseThrow()
        .getCategory(categoryId)
        .map(CategoryRef::from);
  }

  @Override
  public Optional<ImplementationStatusRef> getImplementationStatus(
      String riskDefinitionId, int ordinalValue) {
    return domain
        .getRiskDefinition(riskDefinitionId)
        .flatMap(rd -> rd.getImplementationStateDefinition().getLevel(ordinalValue))
        .map(ImplementationStatusRef::from);
  }

  @Override
  public Optional<RiskDefinitionRef> getRiskDefinitionRef(String riskDefinitionId) {
    return domain.getRiskDefinition(riskDefinitionId).map(RiskDefinitionRef::from);
  }
}
