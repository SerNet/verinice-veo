/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import java.util.Set;

import org.veo.core.entity.Client;
import org.veo.core.entity.Element;
import org.veo.core.entity.Unit;
import org.veo.core.entity.event.UnitImpactRecalculatedEvent;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.entity.state.ElementState;
import org.veo.core.entity.state.RiskState;
import org.veo.core.entity.state.UnitState;
import org.veo.core.repository.UnitRepository;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.base.DomainSensitiveElementValidator;
import org.veo.core.usecase.domain.ElementBatchCreator;
import org.veo.core.usecase.service.EntityStateMapper;
import org.veo.core.usecase.service.IdRefResolver;
import org.veo.core.usecase.service.RefResolverFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnitImportUseCase
    implements TransactionalUseCase<UnitImportUseCase.InputData, UnitImportUseCase.OutputData> {
  private final UnitRepository unitRepository;
  private final RefResolverFactory refResolverFactory;
  private final EntityStateMapper entityStateMapper;
  private final ElementBatchCreator elementBatchCreator;
  private final EventPublisher eventPublisher;

  @Override
  public OutputData execute(InputData input) {
    var resolver = refResolverFactory.db(input.client);
    var unit = resolver.injectNewEntity(TypedId.from(input.unit.getSelfId(), Unit.class));
    var elements =
        input.elements.stream()
            .map(
                e ->
                    (Element)
                        resolver.injectNewEntity(
                            TypedId.from(e.getSelfId(), e.getModelInterface())))
            .toList();

    unit.setClient(input.client);
    entityStateMapper.mapState(input.unit, unit, resolver);
    elements.forEach(e -> e.setOwner(unit));
    input.elements.forEach(e -> mapElement(e, resolver));
    input.risks.forEach(r -> entityStateMapper.mapState(r, resolver));

    elementBatchCreator.create(elements, unitRepository.save(unit));
    try {
      elements.forEach(DomainSensitiveElementValidator::validate);
    } catch (IllegalArgumentException illEx) {
      throw new UnprocessableDataException(illEx.getMessage());
    }
    eventPublisher.publish(UnitImpactRecalculatedEvent.from(unit, this));
    return new OutputData(unit);
  }

  private <T extends Element, TState extends ElementState<T>> void mapElement(
      TState source, IdRefResolver resolver) {
    var target = resolver.resolve(TypedId.from(source.getSelfId(), source.getModelInterface()));
    entityStateMapper.mapState(source, target, false, resolver);
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  public record InputData(
      Client client, UnitState unit, Set<ElementState<?>> elements, Set<RiskState<?, ?>> risks)
      implements UseCase.InputData {}

  public record OutputData(Unit unit) implements UseCase.OutputData {}
}
