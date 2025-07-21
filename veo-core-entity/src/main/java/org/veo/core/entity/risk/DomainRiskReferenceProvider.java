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

import static java.lang.String.format;

import java.math.BigDecimal;

import org.veo.core.entity.DomainBase;
import org.veo.core.entity.exception.RiskConsistencyException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DomainRiskReferenceProvider extends RiskReferenceProvider {

  @Getter private DomainBase domain;

  public static DomainRiskReferenceProvider referencesForDomain(DomainBase domain) {
    return new DomainRiskReferenceProvider(domain);
  }

  @Override
  public RiskRef getRiskRef(String riskDefinitionId, BigDecimal ordinalValue) {
    return domain
        .findRiskDefinition(riskDefinitionId)
        .orElseThrow()
        .getRiskValue(ordinalValue.intValue())
        .map(RiskRef::from)
        .orElseThrow(
            () ->
                new RiskConsistencyException(
                    format(
                        "Risk definition %s contains no risk value with ordinal value %s",
                        riskDefinitionId, ordinalValue)));
  }

  @Override
  public ProbabilityRef getProbabilityRef(String riskDefinitionId, BigDecimal probabilityId) {
    return domain
        .findRiskDefinition(riskDefinitionId)
        .orElseThrow()
        .getProbability()
        .getLevel(probabilityId.intValue())
        .map(ProbabilityRef::from)
        .orElseThrow(
            () ->
                new RiskConsistencyException(
                    format(
                        "Risk definition %s contains no probability with ordinal value %s",
                        riskDefinitionId, probabilityId)));
  }

  @Override
  public ImpactRef getImpactRef(String riskDefinitionId, String category, BigDecimal impactId) {
    return domain
        .findRiskDefinition(riskDefinitionId)
        .orElseThrow()
        .getCategory(category)
        .orElseThrow()
        .getLevel(impactId.intValue())
        .map(ImpactRef::from)
        .orElseThrow(
            () ->
                new RiskConsistencyException(
                    format(
                        "Risk definition %s contains no impact with ordinal value %s",
                        riskDefinitionId, impactId)));
  }

  @Override
  public CategoryRef getCategoryRef(String riskDefinitionId, String categoryId) {
    return domain
        .findRiskDefinition(riskDefinitionId)
        .orElseThrow()
        .getCategory(categoryId)
        .map(CategoryRef::from)
        .orElseThrow(
            () ->
                new RiskConsistencyException(
                    format(
                        "Risk definition %s contains no category with ID %s",
                        riskDefinitionId, categoryId)));
  }

  @Override
  public ImplementationStatusRef getImplementationStatus(
      String riskDefinitionId, int ordinalValue) {
    return domain
        .findRiskDefinition(riskDefinitionId)
        .flatMap(rd -> rd.getImplementationStateDefinition().getLevel(ordinalValue))
        .map(ImplementationStatusRef::from)
        .orElseThrow(
            () ->
                new RiskConsistencyException(
                    format(
                        "Risk definition %s contains no implementation status with ordinal value %d",
                        riskDefinitionId, ordinalValue)));
  }

  @Override
  public RiskDefinitionRef getRiskDefinitionRef(String riskDefinitionId) {
    return domain
        .findRiskDefinition(riskDefinitionId)
        .map(RiskDefinitionRef::from)
        .orElseThrow(
            () ->
                new RiskConsistencyException(
                    format(
                        "Domain %s %s contains no risk definition with ID %s",
                        domain.getName(), domain.getTemplateVersion(), riskDefinitionId)));
  }
}
