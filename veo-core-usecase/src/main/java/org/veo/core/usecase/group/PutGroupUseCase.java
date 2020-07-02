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

import java.time.Instant;
import java.util.Optional;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.impl.BaseModelGroup;
import org.veo.core.entity.transform.TransformContextProvider;
import org.veo.core.entity.transform.TransformTargetToEntityContext;
import org.veo.core.usecase.repository.Repository;
import org.veo.core.usecase.repository.RepositoryProvider;

public class PutGroupUseCase extends UpdateGroupUseCase {

    public PutGroupUseCase(RepositoryProvider repositoryProvider,
            TransformContextProvider transformContextProvider) {
        super(repositoryProvider, transformContextProvider);
    }

    @Override
    protected BaseModelGroup<?> update(InputData input) {
        TransformTargetToEntityContext dataTargetToEntityContext = transformContextProvider.createTargetToEntityContext()
                                                                                           .partialDomain()
                                                                                           .partialClient();
        BaseModelGroup group = input.getGroup();
        group.setVersion(group.getVersion() + 1);
        group.setValidFrom(Instant.now());
        Repository repository = repositoryProvider.getRepositoryFor(input.getGroup()
                                                                         .getClass());
        Optional existingGroup = repository.findById(group.getId());
        if (existingGroup.isEmpty()) {
            throw new NotFoundException("Group %s was not found.", group.getId()
                                                                        .uuidValue());
        }

        Client authenticatedClient = input.getAuthenticatedClient();
        checkSameClient(authenticatedClient, (EntityLayerSupertype) existingGroup.get());

        return (BaseModelGroup<?>) repository.save(group, null, dataTargetToEntityContext);
    }

}
