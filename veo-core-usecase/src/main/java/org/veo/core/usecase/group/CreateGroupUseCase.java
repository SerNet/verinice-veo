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

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import lombok.Value;

import org.veo.core.entity.Client;
import org.veo.core.entity.GroupType;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject.Lifecycle;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.RuntimeModelException;
import org.veo.core.entity.impl.BaseModelGroup;
import org.veo.core.entity.transform.TransformContextProvider;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.EntityLayerSupertypeRepository;
import org.veo.core.usecase.repository.RepositoryProvider;
import org.veo.core.usecase.repository.UnitRepository;

public class CreateGroupUseCase extends UseCase<CreateGroupUseCase.InputData, BaseModelGroup<?>> {

    private final UnitRepository unitRepository;
    private final TransformContextProvider transformContextProvider;
    private final RepositoryProvider repositoryProvider;

    public CreateGroupUseCase(UnitRepository unitRepository, RepositoryProvider repositoryProvider,
            TransformContextProvider transformContextProvider) {
        this.unitRepository = unitRepository;
        this.repositoryProvider = repositoryProvider;
        this.transformContextProvider = transformContextProvider;
    }

    @Override
    @Transactional(TxType.REQUIRED)
    public BaseModelGroup<?> execute(InputData input) {
        TransformTargetToEntityContext dataTargetToEntityContext = transformContextProvider.createTargetToEntityContext()
                                                                                           .partialClient()
                                                                                           .partialDomain();

        Unit unit = unitRepository.findById(input.getUnitId(), dataTargetToEntityContext)
                                  .orElseThrow(() -> new NotFoundException("Unit %s not found.",
                                          input.getUnitId()
                                               .uuidValue()));
        checkSameClient(input.authenticatedClient, unit, unit);

        try {
            BaseModelGroup group = input.groupType.groupClass.getDeclaredConstructor()
                                                             .newInstance();

            group.setId(Key.newUuid());
            group.setName(input.getName());
            group.setOwner(unit);
            group.setState(Lifecycle.CREATING);

            EntityLayerSupertypeRepository repository = repositoryProvider.getEntityLayerSupertypeRepositoryFor(input.groupType.entityClass);

            return (BaseModelGroup<?>) repository.save(group);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new RuntimeModelException("Error creating group", e);

        }

    }

    @Valid
    @Value
    public static class InputData {
        private final Key<UUID> unitId;
        private final String name;
        private final GroupType groupType;
        private final Client authenticatedClient;

    }
}
