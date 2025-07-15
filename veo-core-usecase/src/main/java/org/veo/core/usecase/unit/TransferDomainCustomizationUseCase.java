/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Jonas Jordan
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
package org.veo.core.usecase.unit;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.veo.core.entity.Domain;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.repository.DomainRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * "Customizations" are modifications by normal users to a domain which was created from a domain
 * template. This use case transfers customizations from one domain to another.
 *
 * <p>This is more or less a three-way-merge between the two domains where the source domain's
 * template is considered the origin. The source domain takes precendence in case of conclifcts.
 */
@RequiredArgsConstructor
@Slf4j
public class TransferDomainCustomizationUseCase
    implements TransactionalUseCase<
        TransferDomainCustomizationUseCase.InputData, UseCase.EmptyOutput> {
  private final DomainRepository domainRepository;

  @Override
  public boolean isReadOnly() {
    return false;
  }

  // TODO #2338 remove @Transactional (currently, without this annotation there would be no
  // transaction when calling this from another use case)
  @Transactional(TxType.REQUIRES_NEW)
  @Override
  public EmptyOutput execute(InputData input) {
    var sourceDomain = domainRepository.getById(input.sourceDomainId, input.authentiatedClientId);
    var targetDomain = domainRepository.getById(input.targetDomainId, input.authentiatedClientId);
    // Copy all customized risk definitions to the new domain. This may overwrite risk definition
    // changes from the new domain template version.
    sourceDomain
        .getRiskDefinitions()
        .forEach(
            (id, riskDef) -> {
              var originalRiskDefinition =
                  Optional.ofNullable(sourceDomain.getDomainTemplate())
                      .flatMap(dt -> dt.getRiskDefinition(id))
                      .orElse(null);
              if (!riskDef.equals(originalRiskDefinition)) {
                log.debug(
                    "Copying customized risk definition {} from {} {} ({}) to new version {} ({})",
                    id,
                    sourceDomain.getName(),
                    sourceDomain.getTemplateVersion(),
                    sourceDomain.getIdAsString(),
                    targetDomain.getTemplateVersion(),
                    targetDomain.getIdAsString());
                targetDomain.applyRiskDefinition(id, migrate(riskDef, targetDomain));
              }
            });
    domainRepository.save(targetDomain);
    return EmptyOutput.INSTANCE;
  }

  private static RiskDefinition migrate(RiskDefinition riskDef, Domain targetDomain) {
    return riskDef.withImpactInheritingLinks(
        migrate(riskDef.getImpactInheritingLinks(), targetDomain));
  }

  private static Map<ElementType, List<String>> migrate(
      Map<ElementType, List<String>> impactInheritingLinks, Domain targetDomain) {
    return impactInheritingLinks.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e ->
                    e.getValue().stream()
                        .filter(
                            link ->
                                targetDomain
                                    .getElementTypeDefinition(e.getKey())
                                    .findLink(link)
                                    .isPresent())
                        .toList()))
        .entrySet()
        .stream()
        .filter(e -> !e.getValue().isEmpty())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public record InputData(UUID sourceDomainId, UUID targetDomainId, UUID authentiatedClientId)
      implements UseCase.InputData {}
}
