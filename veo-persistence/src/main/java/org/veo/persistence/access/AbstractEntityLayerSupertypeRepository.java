/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.persistence.access;

import static java.util.Collections.singleton;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Client;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.ElementRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ElementDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.entity.jpa.ElementData;
import org.veo.persistence.entity.jpa.IdentifiableVersionedData;
import org.veo.persistence.entity.jpa.ScopeData;
import org.veo.persistence.entity.jpa.ValidationService;

@Transactional(readOnly = true)
abstract class AbstractElementRepository<T extends Element, S extends ElementData>
        extends AbstractIdentifiableVersionedRepository<T, S> implements ElementRepository<T> {

    protected final ElementDataRepository<S> dataRepository;
    private final CustomLinkDataRepository linkDataRepository;

    final ScopeDataRepository scopeDataRepository;

    AbstractElementRepository(ElementDataRepository<S> dataRepository, ValidationService validation,
            CustomLinkDataRepository linkDataRepository, ScopeDataRepository scopeDataRepository) {
        super(dataRepository, validation);
        this.dataRepository = dataRepository;
        this.linkDataRepository = linkDataRepository;
        this.scopeDataRepository = scopeDataRepository;
    }

    @Override
    public ElementQuery<T> query(Client client) {
        return new ElementQueryImpl<>(dataRepository, client);
    }

    @Transactional
    public void deleteByUnit(Unit owner) {
        var elements = dataRepository.findByUnits(singleton(owner.getId()
                                                                 .uuidValue()));
        var entityIDs = elements.stream()
                                .map(IdentifiableVersionedData::getDbId)
                                .collect(Collectors.toSet());
        var links = linkDataRepository.findLinksByTargetIds(entityIDs);

        // using deleteAll() to utilize batching and optimistic locking:
        linkDataRepository.deleteAll(links);
        elements.forEach(e -> e.getLinks()
                               .clear());

        // remove the unit's elements from scope members:
        var scopes = scopeDataRepository.findDistinctByMembersIn(elements);
        scopes.forEach(scope -> scope.removeMembers(new HashSet<>(elements)));

        dataRepository.deleteAll(elements);
    }

    @Override
    @Transactional
    public void deleteById(Key<UUID> id) {
        // remove links to element:
        var links = linkDataRepository.findLinksByTargetIds(singleton(id.uuidValue()));
        linkDataRepository.deleteAll(links);

        // remove element from scope members:
        Set<Scope> scopes = scopeDataRepository.findDistinctByMemberIds(singleton(id.uuidValue()));
        scopes.stream()
              .map(ScopeData.class::cast)
              .forEach(scopeData -> scopeData.removeMemberById(id));

        dataRepository.deleteById(id.uuidValue());
    }

}
