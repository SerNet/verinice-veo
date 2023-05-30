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
package org.veo.adapter.presenter.api.io.mapper;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.veo.adapter.presenter.api.dto.RiskDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.full.RiskValuesDto;
import org.veo.core.entity.Key;
import org.veo.core.entity.risk.RiskValues;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class CategorizedRiskValueMapper {

  /**
   * Converts data from DTOs to a format required by the use-case.
   *
   * <p>Source: a map of domain-IDs and domain-associations. Domain-associations include a Map of
   * risk-definition-IDs to risk values.
   *
   * <p>Target: a Set of risk values as required by a use case.
   */
  public static Set<RiskValues> map(Map<String, RiskDomainAssociationDto> domainsWithRiskValues) {
    return domainsWithRiskValues.entrySet().stream()
        .flatMap(e -> toRiskValues(e.getKey(), e.getValue().getRiskDefinitions()))
        .collect(Collectors.toSet());
  }

  public static Stream<RiskValues> toRiskValues(
      String domainId, Map<String, RiskValuesDto> riskDefinitions) {
    return riskDefinitions.entrySet().stream()
        .map(e -> toRiskValues(domainId, e.getKey(), e.getValue()));
  }

  private static RiskValues toRiskValues(
      String domainId, String riskDefinitionId, RiskValuesDto dto) {
    return RiskValues.builder()
        .probability(dto.getProbability())
        .impactCategories(dto.getImpactValues())
        .categorizedRisks(dto.getRiskValues())
        .domainId(Key.uuidFrom(domainId))
        .riskDefinitionId(new Key<>(riskDefinitionId))
        .build();
  }
}
