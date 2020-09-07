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

import java.util.Collection;
import java.util.UUID;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.usecase.repository.Repository;
import org.veo.core.usecase.repository.RepositoryProvider;

public class ModelObjectReferenceResolver {
    private final EntityFactory entityFactory;
    private final RepositoryProvider repositoryProvider;

    public ModelObjectReferenceResolver(EntityFactory entityFactory,
            RepositoryProvider repositoryProvider) {
        this.entityFactory = entityFactory;
        this.repositoryProvider = repositoryProvider;
    }

    /**
     * Resolves model references by loading the models from the DB and adding them
     * to a transformation context.
     *
     * @param client
     *            Authenticated client
     * @param references
     *            Model object references to be loaded
     * @return A new transformation context that contains all resolved models
     */
    public DtoToEntityContext loadIntoContext(Client client,
            Collection<ModelObjectReference<? extends ModelObject>> references) {
        DtoToEntityContext context = new DtoToEntityContext(entityFactory);

        for (Domain d : client.getDomains()) {
            context.addEntity(d);
        }
        for (ModelObjectReference<? extends ModelObject> objectReference : references) {
            if (objectReference.getType()
                               .equals(Domain.class)) {
                continue;// skip domains as we get them from the client
            }
            Repository<? extends ModelObject, Key<UUID>> entityRepository = repositoryProvider.getRepositoryFor(objectReference.getType());
            context.addEntity(entityRepository.findById(Key.uuidFrom(objectReference.getId()))
                                              .orElseThrow(() -> new NotFoundException(
                                                      "ref not found %s %s",
                                                      objectReference.getId(),
                                                      objectReference.getType())));
        }
        return context;
    }
}
