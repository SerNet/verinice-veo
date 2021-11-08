/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade
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
package org.veo.service;

import java.util.Optional;

import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
import org.veo.core.entity.Versioned;
import org.veo.core.repository.IdentifiableVersionedRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.common.ETag;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EtagService {

    private final RepositoryProvider repositoryProvider;

    public <T extends Identifiable & Versioned> Optional<String> getEtag(Class<T> entityClass,
            String id) {
        IdentifiableVersionedRepository<T> repo = repositoryProvider.getVersionedIdentifiableRepositoryFor(entityClass);
        return repo.getVersion(Key.uuidFrom(id))
                   .map(version -> ETag.from(id, version));
    }

}
