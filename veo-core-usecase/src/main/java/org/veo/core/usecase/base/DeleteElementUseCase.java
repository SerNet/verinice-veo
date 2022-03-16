/*******************************************************************************
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
 ******************************************************************************/
package org.veo.core.usecase.base;

import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.Process;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.event.RiskComponentChangeEvent;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.ElementRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.EmptyOutput;

import lombok.Value;

public class DeleteElementUseCase
        implements TransactionalUseCase<DeleteElementUseCase.InputData, EmptyOutput> {

    private final RepositoryProvider repositoryProvider;
    private final EventPublisher eventPublisher;

    private static final Set<Class<?>> RELEVANT_CLASSES_FOR_RISK = Set.of(Process.class,
                                                                          Scenario.class,
                                                                          Control.class);

    public DeleteElementUseCase(RepositoryProvider repositoryProvider,
            EventPublisher eventPublisher) {
        this.repositoryProvider = repositoryProvider;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public EmptyOutput execute(InputData input) {
        ElementRepository<? extends Element> repository = repositoryProvider.getElementRepositoryFor(input.entityClass);
        Element entity = repository.findById(input.getId())
                                   .orElseThrow(() -> new NotFoundException("%s %s was not found.",
                                           input.entityClass.getSimpleName(), input.getId()
                                                                                   .uuidValue()));
        entity.checkSameClient(input.authenticatedClient);
        entity.remove();
        repository.deleteById(entity.getId());
        if (RELEVANT_CLASSES_FOR_RISK.contains(input.getEntityClass())) {
            eventPublisher.publish(new RiskComponentChangeEvent(entity));
        }
        return EmptyOutput.INSTANCE;
    }

    @Value
    public static class InputData implements UseCase.InputData {
        Class<? extends Element> entityClass;
        Key<UUID> id;
        Client authenticatedClient;

    }

}
