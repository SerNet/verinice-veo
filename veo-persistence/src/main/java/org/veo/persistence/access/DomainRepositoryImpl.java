/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.repository.DomainRepository;
import org.veo.persistence.access.jpa.DomainDataRepository;
import org.veo.persistence.entity.jpa.DomainData;
import org.veo.persistence.entity.jpa.ValidationService;

import lombok.NonNull;

@Repository
public class DomainRepositoryImpl extends
        AbstractIdentifiableVersionedRepository<Domain, DomainData> implements DomainRepository {

    private final DomainDataRepository dataRepository;

    public DomainRepositoryImpl(DomainDataRepository dataRepository, ValidationService validator) {
        super(dataRepository, validator);
        this.dataRepository = dataRepository;
    }

    @Override
    public Set<Domain> findAllByClient(Key<UUID> clientId) {
        return dataRepository.findAllByClient(clientId.uuidValue())
                             .stream()
                             .map(Domain.class::cast)
                             .collect(Collectors.toSet());
    }

    @Override
    public Set<Domain> findAllByTemplateId(Key<UUID> domainTemplateId) {
        return dataRepository.findAllByDomainTemplateId(domainTemplateId.uuidValue())
                             .stream()
                             .map(Domain.class::cast)
                             .collect(Collectors.toSet());
    }

    @Override
    public Optional<Domain> findByCatalogItem(CatalogItem catalogItem) {
        return dataRepository.findByCatalogsCatalogItemsId(catalogItem.getId()
                                                                      .uuidValue())
                             .map(Domain.class::cast);
    }

    @Override
    public Optional<Domain> findById(@NonNull Key<UUID> domainId, @NonNull Key<UUID> clientId) {
        return dataRepository.findById(domainId.uuidValue(), clientId.uuidValue());
    }

}
