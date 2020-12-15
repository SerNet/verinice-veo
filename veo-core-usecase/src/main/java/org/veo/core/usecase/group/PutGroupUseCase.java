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

import org.veo.core.entity.Key;
import org.veo.core.entity.ModelGroup;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.usecase.repository.EntityGroupRepository;

public class PutGroupUseCase extends UpdateGroupUseCase {

    public PutGroupUseCase(EntityGroupRepository entityGroupRepository) {
        super(entityGroupRepository);
    }

    @Override
    protected ModelGroup<?> update(InputData input) {
        Optional<ModelGroup<?>> existingGroup = entityGroupRepository.findById(Key.uuidFrom(input.getUuid()));

        if (existingGroup.isEmpty()) {
            throw new NotFoundException("Group %s was not found.", input.getUuid());
        }
        ModelGroup<?> groupInDb = existingGroup.get();

        ModelGroup<?> group = input.getGroupMapper()
                                   .apply(groupInDb.getClass());
        group.setCreatedAt(Instant.now());

        checkETag(groupInDb, input);
        group.version(input.username, existingGroup.get());
        group.setVersion(groupInDb.getVersion());
        groupInDb.checkSameClient(input.getAuthenticatedClient());
        return entityGroupRepository.save(group);
    }

}
