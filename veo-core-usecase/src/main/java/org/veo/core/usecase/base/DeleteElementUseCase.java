/*
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
 */
package org.veo.core.usecase.base;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Asset;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Control;
import org.veo.core.entity.Element;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.event.InboundLinkEvent;
import org.veo.core.entity.event.PartsChangedEvent;
import org.veo.core.entity.event.RiskAffectingElementChangeEvent;
import org.veo.core.repository.ElementRepository;
import org.veo.core.repository.InspectionFindingRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.EmptyOutput;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteElementUseCase
    implements TransactionalUseCase<DeleteElementUseCase.InputData, EmptyOutput> {

  private final RepositoryProvider repositoryProvider;
  private final InspectionFindingRepository inspectionFindingRepository;
  private final EventPublisher eventPublisher;

  private static final Set<Class<?>> RELEVANT_CLASSES_FOR_RISK =
      Set.of(Process.class, Asset.class, Scope.class, Scenario.class, Control.class);

  @Override
  public EmptyOutput execute(InputData input, UserAccessRights userAccessRights) {
    ElementRepository<? extends Element> repository =
        repositoryProvider.getElementRepositoryFor(input.entityClass);
    Element entity = repository.getById(input.id, userAccessRights);
    Collection<PartsChangedEvent> compositeEvents =
        entity instanceof CompositeElement<?> c ? getCompositeEvents(c) : List.of();
    userAccessRights.checkElementWriteAccess(entity);
    entity.remove();
    repository.deleteById(entity.getId());
    inspectionFindingRepository.removeInspections(entity);
    if (RELEVANT_CLASSES_FOR_RISK.contains(input.entityClass)) {
      eventPublisher.publish(new RiskAffectingElementChangeEvent(entity, this));
    }
    // notify link targets
    entity
        .getLinks()
        .forEach(l ->
            eventPublisher.publish(
                new InboundLinkEvent(
                    l.getTarget(),
                    l.getDomain(),
                    l.getType(),
                    InboundLinkEvent.Operation.DELETION,
                    this)));
    // notify composites
    compositeEvents.forEach(eventPublisher::publish);
    return EmptyOutput.INSTANCE;
  }

  private Collection<PartsChangedEvent> getCompositeEvents(CompositeElement<?> element) {
    return element.getComposites().stream()
        .map(c -> new PartsChangedEvent(c, c.getPartsRecursively()))
        .toList();
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  public record InputData(Class<? extends Element> entityClass, UUID id)
      implements UseCase.InputData {}
}
