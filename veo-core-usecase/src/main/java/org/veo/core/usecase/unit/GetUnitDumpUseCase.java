/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.AccountProvider;
import org.veo.core.entity.Asset;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Process;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetUnitDumpUseCase
    implements TransactionalUseCase<GetUnitDumpUseCase.InputData, GetUnitDumpUseCase.OutputData> {
  private final AccountProvider accountProvider;
  private final GenericElementRepository genericElementRepository;
  private final UnitRepository unitRepository;
  private final DomainRepository domainRepository;

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    var unit =
        accountProvider.getCurrentUserAccount().isAdmin()
            ? unitRepository.getById(input.unitId)
            : unitRepository.getById(input.unitId, userAccessRights);
    var client = accountProvider.getCurrentUserAccount().getClient();
    return new OutputData(
        unit,
        getElements(
            unit,
            Optional.ofNullable(input.domainId)
                .map(id -> domainRepository.getById(id, client.getId()))
                .orElse(null)));
  }

  private Collection<Element> getElements(Unit unit, Domain domain) {
    var query = genericElementRepository.query(unit.getClient());
    query.whereUnitIn(Set.of(unit));
    query.fetchControlImplementations();
    query.fetchRequirementImplementations();
    query.fetchChildren();
    query.fetchRisks();
    query.fetchRiskValuesAspects();
    if (domain != null) {
      query.whereDomainsContain(domain);
    }
    var elements = query.execute(PagingConfiguration.UNPAGED).getResultPage();
    if (domain != null) {
      // remove data from other domains
      elements.forEach(
          e -> {
            new HashSet<>(e.getDomains())
                .stream().filter(d -> !domain.equals(d)).forEach(e::removeFromDomains);

            // remove risks for scenarios that are not contained in the dump
            if (e instanceof Process process) {
              filterRisks(process, domain);
            } else if (e instanceof Asset asset) {
              filterRisks(asset, domain);
            } else if (e instanceof Scope scope) {
              filterRisks(scope, domain);
            }
            // remove parts that are not contained in the dump
            if (e instanceof CompositeElement<?> composite) {
              composite.getParts().removeIf(part -> !part.isAssociatedWithDomain(domain));
            }
            // remove members that are not contained in the dump
            if (e instanceof Scope scope) {
              scope.getMembers().removeIf(member -> !member.isAssociatedWithDomain(domain));
            }
          });
    }
    return elements;
  }

  private <
          TElement extends RiskAffected<TElement, TRisk>,
          TRisk extends AbstractRisk<TElement, TRisk>>
      void filterRisks(TElement e, Domain domain) {
    e.getRisks().removeIf(r -> !r.getScenario().isAssociatedWithDomain(domain));
    e.getRisks()
        .forEach(
            r -> {
              if (r.getMitigation() != null && !r.getMitigation().isAssociatedWithDomain(domain)) {
                r.mitigate(null);
              }
              if (r.getRiskOwner() != null && !r.getRiskOwner().isAssociatedWithDomain(domain)) {
                r.appoint(null);
              }
            });
  }

  public record InputData(
      @NonNull UUID unitId,
      /**
       * If a domain ID is set, only elements associated with that domain are exported and aspects
       * belonging to other domains are not included in the elements' representations.
       */
      UUID domainId)
      implements UseCase.InputData {}

  public record OutputData(Unit unit, Collection<Element> elements) implements UseCase.OutputData {}
}
