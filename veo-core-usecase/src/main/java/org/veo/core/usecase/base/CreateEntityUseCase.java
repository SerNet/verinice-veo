/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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

import java.util.UUID;

import javax.transaction.Transactional;
import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.Repository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.DesignatorService;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor
public abstract class CreateEntityUseCase<TEntity extends EntityLayerSupertype> implements
        TransactionalUseCase<CreateEntityUseCase.InputData<TEntity>, CreateEntityUseCase.OutputData<TEntity>> {
    private final UnitRepository unitRepository;
    private final Repository<TEntity, Key<UUID>> entityRepo;
    private final DesignatorService designatorService;

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public CreateEntityUseCase.OutputData<TEntity> execute(
            CreateEntityUseCase.InputData<TEntity> input) {
        var entity = input.getNewEntity();
        Unit unit = unitRepository.findById(entity.getOwner()
                                                  .getId())
                                  .orElseThrow(() -> new NotFoundException("Unit %s not found.",
                                          entity.getOwner()
                                                .getId()
                                                .uuidValue()));
        unit.checkSameClient(input.authenticatedClient);
        designatorService.assignDesignator(entity, input.authenticatedClient);
        return new CreateEntityUseCase.OutputData<>(entityRepo.save(entity));
    }

    @Valid
    @Value
    public static class InputData<TEntity> implements UseCase.InputData {
        TEntity newEntity;
        Client authenticatedClient;
    }

    @Valid
    @Value
    public static class OutputData<TEntity> implements UseCase.OutputData {
        @Valid
        TEntity entity;
    }
}
