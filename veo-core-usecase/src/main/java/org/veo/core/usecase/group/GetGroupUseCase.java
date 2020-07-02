/*******************************************************************************
 * Copyright (c) 2019 Jochen Kemnade.
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

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import lombok.Value;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.GroupType;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.impl.BaseModelGroup;
import org.veo.core.entity.transform.TransformContextProvider;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.repository.RepositoryProvider;

public class GetGroupUseCase
        extends UseCase<GetGroupUseCase.InputData, GetGroupUseCase.OutputData> {

    private final RepositoryProvider repositoryProvider;
    private final TransformContextProvider transformContextProvider;

    public GetGroupUseCase(RepositoryProvider repositoryProvider,
            TransformContextProvider transformContextProvider) {
        this.repositoryProvider = repositoryProvider;
        this.transformContextProvider = transformContextProvider;
    }

    @Override
    @Transactional(TxType.REQUIRED)
    public OutputData execute(InputData input) {
        TransformTargetToEntityContext dataTargetToEntityContext = transformContextProvider.createTargetToEntityContext()
                                                                                           .partialDomain()
                                                                                           .partialClient();
        EntityLayerSupertype group = repositoryProvider.getRepositoryFor(input.groupType.entityClass)
                                                       .findById(input.getId(),
                                                                 dataTargetToEntityContext)
                                                       .orElseThrow(() -> new NotFoundException(
                                                               input.getId()
                                                                    .uuidValue()));
        checkSameClient(input.authenticatedClient, group);
        return new OutputData((BaseModelGroup<?>) group);
    }

    @Valid
    @Value
    public static class InputData implements UseCase.InputData {
        private final Key<UUID> id;
        private final GroupType groupType;
        private final Client authenticatedClient;
    }

    @Valid
    @Value
    public static class OutputData implements UseCase.OutputData {
        @Valid
        private final BaseModelGroup<?> group;
    }
}
