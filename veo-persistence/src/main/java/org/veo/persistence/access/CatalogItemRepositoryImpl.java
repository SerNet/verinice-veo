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

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.TailoringReference;
import org.veo.core.repository.CatalogItemQuery;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.SubTypeCount;
import org.veo.persistence.access.jpa.CatalogItemDataRepository;
import org.veo.persistence.access.query.CatalogItemQueryImpl;
import org.veo.persistence.entity.jpa.CatalogItemData;
import org.veo.persistence.entity.jpa.DomainData;
import org.veo.persistence.entity.jpa.ValidationService;

@Repository
public class CatalogItemRepositoryImpl
    extends AbstractIdentifiableVersionedRepository<CatalogItem, CatalogItemData>
    implements CatalogItemRepository {

  private final CatalogItemDataRepository catalogItemDataRepository;

  public CatalogItemRepositoryImpl(
      CatalogItemDataRepository dataRepository, ValidationService validator) {
    super(dataRepository, validator);
    catalogItemDataRepository = dataRepository;
  }

  @Override
  public Set<CatalogItem> findAllByIdsFetchDomainAndTailoringReferences(
      Set<Key<UUID>> ids, Client client) {
    var idStrings = ids.stream().map(Key::uuidValue).toList();
    return StreamSupport.stream(
            catalogItemDataRepository
                .findAllByIdsFetchDomainAndTailoringReferences(idStrings, client)
                .spliterator(),
            false)
        .map(CatalogItem.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<CatalogItem> findAllByIdsFetchDomain(Set<Key<UUID>> ids, Client client) {
    return catalogItemDataRepository
        .findAllByIdsFetchDomain(ids.stream().map(Key::uuidValue).toList(), client)
        .stream()
        .map(CatalogItem.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<TailoringReference<CatalogItem>> findTailoringReferencesByIds(
      Set<Key<UUID>> ids, Client client) {
    return catalogItemDataRepository
        .findTailoringReferencesByIds(
            ids.stream().map(Key::uuidValue).collect(Collectors.toSet()), client)
        .stream()
        .map(tr -> (TailoringReference<CatalogItem>) tr)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<CatalogItem> findAllByDomain(Domain domain) {
    return catalogItemDataRepository.findAllByDomain((DomainData) domain);
  }

  @Override
  public Set<SubTypeCount> getCountsBySubType(Domain domain) {
    return catalogItemDataRepository.getCountsBySubType(domain.getIdAsString());
  }

  @Override
  public CatalogItemQuery query(Domain domain) {
    return new CatalogItemQueryImpl(catalogItemDataRepository, domain);
  }
}
