/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.core.usecase.decision;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Client;
import org.veo.core.entity.Element;
import org.veo.core.entity.decision.DecisionRef;
import org.veo.core.entity.decision.DecisionResult;
import org.veo.core.entity.inspection.Finding;
import org.veo.core.entity.state.ElementState;
import org.veo.core.entity.transform.IdentifiableFactory;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.inspection.Inspector;
import org.veo.core.usecase.service.EntityStateMapper;
import org.veo.core.usecase.service.RefResolverFactory;

import lombok.RequiredArgsConstructor;

/**
 * Evaluates decisions and inspections for a transient element and returns the results. Does not
 * persist any changes. This must NOT run in a read-write transaction, so JPA does not automatically
 * persist anything.
 */
@RequiredArgsConstructor
public class EvaluateElementUseCase
    implements TransactionalUseCase<
        EvaluateElementUseCase.InputData, EvaluateElementUseCase.OutputData> {
  private final RefResolverFactory refResolverFactory;
  private final IdentifiableFactory identifiableFactory;
  private final EntityStateMapper entityStateMapper;
  private final DomainRepository domainRepository;
  private final GenericElementRepository elementRepository;
  private final Decider decider;
  private final Inspector inspector;

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    var domain =
        domainRepository.getByIdWithDecisionsAndInspections(
            input.domainId, userAccessRights.getClientId());
    var element = fetchOrCreateElement(input.element, userAccessRights, domain.getOwner());
    element.setDecisionResults(decider.decide(element, domain), domain);
    var findings = inspector.inspect(element, domain);
    return new OutputData(element.getDecisionResults(domain), findings);
  }

  private <T extends Element> T fetchOrCreateElement(
      ElementState<T> source, UserAccessRights user, Client client) {
    Optional<UUID> sourceId = Optional.ofNullable(source.getId());
    var element =
        sourceId
            .map(id -> elementRepository.getById(id, source.getModelInterface(), user))
            .orElseGet(() -> identifiableFactory.create(source.getModelInterface()));
    entityStateMapper.mapState(source, element, false, refResolverFactory.db(client));
    return element;
  }

  @Valid
  public record InputData(UUID domainId, ElementState<?> element) implements UseCase.InputData {}

  @Valid
  public record OutputData(
      Map<DecisionRef, DecisionResult> decisionResults, Set<Finding> inspectionFindings)
      implements UseCase.OutputData {}
}
