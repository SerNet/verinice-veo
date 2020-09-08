/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.usecase.group;

import java.util.UUID;

import javax.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.GroupType;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelGroup;
import org.veo.core.entity.Unit;
import org.veo.core.entity.Versioned.Lifecycle;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.EntityLayerSupertypeRepository;
import org.veo.core.usecase.repository.RepositoryProvider;
import org.veo.core.usecase.repository.UnitRepository;

import lombok.Value;

public class CreateGroupUseCase<R>
        extends UseCase<CreateGroupUseCase.InputData, CreateGroupUseCase.OutputData, R> {

    private final UnitRepository unitRepository;
    private final RepositoryProvider repositoryProvider;
    private final EntityFactory entityFactoty;

    public CreateGroupUseCase(UnitRepository unitRepository, RepositoryProvider repositoryProvider,
            EntityFactory entityFactoty) {
        this.unitRepository = unitRepository;
        this.repositoryProvider = repositoryProvider;
        this.entityFactoty = entityFactoty;
    }

    @Override
    public OutputData execute(InputData input) {
        Unit unit = unitRepository.findById(input.getUnitId())
                                  .orElseThrow(() -> new NotFoundException("Unit %s not found.",
                                          input.getUnitId()
                                               .uuidValue()));
        checkSameClient(input.authenticatedClient, unit, unit);

        ModelGroup<?> group = entityFactoty.createGroup(input.groupType);

        group.setId(Key.newUuid());
        group.setName(input.getName());
        group.setOwner(unit);
        group.setState(Lifecycle.CREATING);

        EntityLayerSupertypeRepository repository = repositoryProvider.getEntityLayerSupertypeRepositoryFor(input.groupType.entityClass);

        return new OutputData((ModelGroup<?>) repository.save(group));

    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        Key<UUID> unitId;
        String name;
        GroupType groupType;
        Client authenticatedClient;

    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        ModelGroup<?> group;
    }
}
