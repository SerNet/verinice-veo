/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
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
package org.veo.adapter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.usecase.repository.Repository;
import org.veo.core.usecase.repository.RepositoryProvider;

/**
 * Resolves {@link ModelObjectReference}s by fetching the target entity from a
 * repository. Instances of this class should NOT be long-lived, because it uses
 * an entity cache.
 */
public class ModelObjectReferenceResolver {
    private final RepositoryProvider repositoryProvider;
    private final Client client;
    private final Map<ModelObjectReference<?>, ModelObject> cache = new HashMap<>();

    public ModelObjectReferenceResolver(RepositoryProvider repositoryProvider, Client client) {
        this.repositoryProvider = repositoryProvider;
        this.client = client;
    }

    /**
     * Resolves given reference by fetching the target entity from a cache or a
     * repository.
     *
     * @param objectReference
     *            referencing the desired entity
     * @param <TEntity>
     *            target entity type
     * @throws NotFoundException
     *             when entity does not exist in the repository.
     * @throws org.veo.core.entity.specification.ClientBoundaryViolationException
     *             when entity does not belong to this resolver's client.
     */
    public <TEntity extends ModelObject> TEntity resolve(
            ModelObjectReference<TEntity> objectReference) throws NotFoundException {
        var entity = cache.get(objectReference);
        if (entity != null) {
            return (TEntity) entity;
        }

        Repository<? extends ModelObject, Key<UUID>> entityRepository = repositoryProvider.getRepositoryFor(objectReference.getType());
        entity = entityRepository.findById(Key.uuidFrom(objectReference.getId()))
                                 .orElseThrow(() -> new NotFoundException("ref not found %s %s",
                                         objectReference.getId(), objectReference.getType()));
        if (entity instanceof Unit) {
            ((Unit) entity).checkSameClient(client);
        }
        if (entity instanceof EntityLayerSupertype) {
            ((EntityLayerSupertype) entity).checkSameClient(client);
        }
        cache.put(objectReference, entity);
        return (TEntity) entity;
    }

    // TODO VEO-344 BULK RESOLVE
}
