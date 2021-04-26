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
package org.veo.persistence.access;

import static java.util.Collections.singleton;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;
import org.veo.core.usecase.repository.EntityLayerSupertypeQuery;
import org.veo.core.usecase.repository.EntityLayerSupertypeRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.EntityLayerSupertypeDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.entity.jpa.BaseModelObjectData;
import org.veo.persistence.entity.jpa.EntityLayerSupertypeData;
import org.veo.persistence.entity.jpa.ModelObjectValidation;
import org.veo.persistence.entity.jpa.ScopeData;

@Transactional(readOnly = true)
abstract class AbstractEntityLayerSupertypeRepository<T extends EntityLayerSupertype, S extends EntityLayerSupertypeData>
        extends AbstractModelObjectRepository<T, S> implements EntityLayerSupertypeRepository<T> {

    protected final EntityLayerSupertypeDataRepository<S> dataRepository;
    private final CustomLinkDataRepository linkDataRepository;

    final ScopeDataRepository scopeDataRepository;

    AbstractEntityLayerSupertypeRepository(EntityLayerSupertypeDataRepository<S> dataRepository,
            ModelObjectValidation validation, CustomLinkDataRepository linkDataRepository,
            ScopeDataRepository scopeDataRepository) {
        super(dataRepository, validation);
        this.dataRepository = dataRepository;
        this.linkDataRepository = linkDataRepository;
        this.scopeDataRepository = scopeDataRepository;
    }

    @Override
    public EntityLayerSupertypeQuery<T> query(Client client) {
        return new EntityLayerSupertypeQueryImpl<>(dataRepository, client);
    }

    @Transactional
    public void deleteByUnit(Unit owner) {
        var entities = dataRepository.findByUnits(singleton(owner.getDbId()));
        var entityIDs = entities.stream()
                                .map(BaseModelObjectData::getDbId)
                                .collect(Collectors.toSet());
        var links = linkDataRepository.findLinksByTargetIds(entityIDs);

        // remove the unit's entities from scope members:
        var scopes = scopeDataRepository.findDistinctByMembersIn(entities);
        scopes.forEach(scope -> scope.removeMembers(new HashSet<>(entities)));

        // using deleteAll() to utilize batching and optimistic locking:
        linkDataRepository.deleteAll(links);
        dataRepository.deleteAll(entities);
    }

    @Override
    @Transactional
    public void deleteById(Key<UUID> id) {
        // remove links to element:
        var links = linkDataRepository.findLinksByTargetIds(singleton(id.uuidValue()));
        linkDataRepository.deleteAll(links);

        // remove element from scope members:
        Set<Scope> scopes = scopeDataRepository.findDistinctByMembers_DbId_In(singleton(id.uuidValue()));
        scopes.stream()
              .map(ScopeData.class::cast)
              .forEach(scopeData -> scopeData.removeMemberById(id));

        dataRepository.deleteById(id.uuidValue());
    }

}
