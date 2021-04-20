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

import static java.lang.String.format;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.specification.SameClientSpecification;
import org.veo.core.repository.Repository;
import org.veo.core.repository.RepositoryProvider;

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
     * Resolves the given reference by fetching the target entity from a cache or a
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
        return resolve(Collections.singleton(objectReference)).iterator()
                                                              .next();
    }

    /**
     * Resolves the given references by fetching the target entities from a cache or
     * a repository.
     *
     * @param objectReferences
     *            referencing the desired entity
     * @param <TEntity>
     *            target entity type
     * @throws NotFoundException
     *             when one or more references cannot be resolved from the
     *             repository.
     * @throws org.veo.core.entity.specification.ClientBoundaryViolationException
     *             when one or more entities do not belong to this resolver's
     *             client.
     */
    public <TEntity extends ModelObject> Set<TEntity> resolve(
            Set<ModelObjectReference<TEntity>> objectReferences) {
        if (objectReferences.isEmpty()) {
            return Collections.emptySet();
        }
        HashSet<TEntity> result = new HashSet<>(objectReferences.size());
        HashSet<ModelObjectReference<TEntity>> copyOfReferences = new HashSet<>(objectReferences);
        Iterator<ModelObjectReference<TEntity>> it = copyOfReferences.iterator();
        while (it.hasNext()) {
            ModelObjectReference<TEntity> ref = it.next();
            ModelObject cachedEntry = cache.get(ref);
            if (cachedEntry != null) {
                result.add((TEntity) cachedEntry);
                it.remove();
            }
        }
        if (copyOfReferences.isEmpty()) {
            return result;
        }

        Class<TEntity> entityType = objectReferences.iterator()
                                                    .next()
                                                    .getType();

        Repository<? extends ModelObject, Key<UUID>> entityRepository = repositoryProvider.getRepositoryFor(entityType);

        Set<? extends ModelObject> entities = entityRepository.getByIds(copyOfReferences.stream()
                                                                                        .map(ModelObjectReference::getId)
                                                                                        .map(Key::uuidFrom)
                                                                                        .collect(Collectors.toSet()));

        Map<String, ModelObjectReference<TEntity>> copyOfReferencesById = copyOfReferences.stream()
                                                                                          .collect(Collectors.toMap(ModelObjectReference::getId,
                                                                                                                    Function.identity()));

        entities.forEach(entity -> {
            if (entity instanceof Unit) {
                ((Unit) entity).checkSameClient(client);
            }
            if (entity instanceof EntityLayerSupertype) {
                ((EntityLayerSupertype) entity).checkSameClient(client);
            }
            if (entity instanceof Domain) {
                if (!(new SameClientSpecification<>(
                        client)).isSatisfiedBy(((Domain) entity).getOwner()))
                    throw new ClientBoundaryViolationException(
                            format("The client boundary would be violated by the attempted operation "
                                    + "on element: %s",
                                   entity));
            }
            result.add((TEntity) entity);
            ModelObjectReference<TEntity> reference = copyOfReferencesById.get(entity.getId()
                                                                                     .uuidValue());
            cache.put(reference, entity);
            copyOfReferences.remove(reference);
        });
        if (!copyOfReferences.isEmpty()) {
            throw new NotFoundException(
                    "Unable to resolve references of type %s to objects: missing IDs: %s",
                    entityType, copyOfReferences.stream()
                                                .map(ModelObjectReference::getId)
                                                .collect(Collectors.joining(", ")));
        }
        return result;
    }

}
