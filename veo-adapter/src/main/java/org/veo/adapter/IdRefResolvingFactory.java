/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.adapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.service.domaintemplate.SyntheticIdRef;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.transform.IdentifiableFactory;

import lombok.RequiredArgsConstructor;

/**
 * Combined {@link IdRefResolver} and {@link IdentifiableFactory} implementation
 * for creating new entities and resolving references while mapping a deep DTO
 * hierarchy where nested entities may reference each other.
 *
 * When mapping a DTO hierarchy it is possible that a nested DTO is mapped to an
 * entity first and references to that entity must be resolved later. However,
 * it is also possible that references to an entity must be resolved before the
 * referenced entity itself was even created, depending on the order in which
 * the nested DTOs happen to be mapped. This implementation solves both problems
 * by using the same logic when resolving a reference and when creating a new
 * entity: If the desired entity is already known return the existing entity or
 * else create a new entity and add it the registry, so it may be resolved
 * later.
 *
 * Although all entities created by this factory are resolvable by their ID (as
 * used in the DTO), they are created as new entities, i.e. their ID properties
 * are left null (to avoid conflicts with existing entities in the DB).
 */
@RequiredArgsConstructor
public class IdRefResolvingFactory implements IdRefResolver, IdentifiableFactory {

    private final IdentifiableFactory factory;
    private final Map<IdRef<?>, Identifiable> registry = new HashMap<>();
    private Key<UUID> globalDomainTemplateId;

    @Override
    public <TEntity extends Identifiable> TEntity resolve(IdRef<TEntity> objectReference)
            throws NotFoundException {
        return create(objectReference.getType(), Key.uuidFrom(objectReference.getId()));
    }

    @Override
    public <TEntity extends Identifiable> Set<TEntity> resolve(
            Set<IdRef<TEntity>> objectReferences) {
        return objectReferences.stream()
                               .map(this::resolve)
                               .collect(Collectors.toSet());
    }

    @Override
    public <T extends Identifiable> T create(Class<T> type, Key<UUID> id) {
        if (globalDomainTemplateId != null
                && (type == Domain.class || type == DomainTemplate.class)) {
            return findOrCreate((Class<T>) DomainTemplate.class, globalDomainTemplateId);
        }
        return findOrCreate(type, id);
    }

    private <T extends Identifiable> T findOrCreate(Class<T> type, Key<UUID> id) {
        if (id == null) {
            return factory.create(type, null);
        }

        return (T) registry.computeIfAbsent(SyntheticIdRef.from(id.uuidValue(), type), idRef -> {
            // Do not set the ID on the actual entity itself, so it is treated as a new
            // entity. This avoids conflicts with existing entities with the same ID in the
            // DB. The ID will only be used for resolving.
            return factory.create(type, null);
        });
    }

    /**
     * Define a global domain template ID. When this is set, any reference to a
     * domain or a domain template will be redirected to given domain template.
     */
    public void setGlobalDomainTemplate(String domainTemplateId) {
        globalDomainTemplateId = Key.uuidFrom(domainTemplateId);
    }
}
