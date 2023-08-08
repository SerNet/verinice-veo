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

import static java.lang.String.format;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.DomainRepository;
import org.veo.persistence.access.jpa.DomainDataRepository;
import org.veo.persistence.entity.jpa.DomainData;
import org.veo.persistence.entity.jpa.ValidationService;

import lombok.NonNull;

@Repository
public class DomainRepositoryImpl
    extends AbstractIdentifiableVersionedRepository<Domain, DomainData>
    implements DomainRepository {

  private final DomainDataRepository dataRepository;

  public DomainRepositoryImpl(DomainDataRepository dataRepository, ValidationService validator) {
    super(dataRepository, validator);
    this.dataRepository = dataRepository;
  }

  @Override
  public Set<Domain> findAllActiveByClient(Key<UUID> clientId) {
    return dataRepository.findAllActiveByClient(clientId.uuidValue()).stream()
        .map(Domain.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Domain> findActiveDomainsWithProfilesAndRiskDefinitions(Key<UUID> clientId) {
    return dataRepository
        .findActiveDomainsWithProfilesAndRiskDefinitions(clientId.uuidValue())
        .stream()
        .map(Domain.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Domain> findAllByClientWithEntityTypeDefinitionsAndRiskDefinitions(
      Key<UUID> clientId) {
    return dataRepository
        .findAllByClientWithEntityTypeDefinitionsAndRiskDefinitions(clientId.uuidValue())
        .stream()
        .map(Domain.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Key<UUID>> findIdsByTemplateId(Key<UUID> domainTemplateId) {
    return dataRepository.findIdsByDomainTemplateId(domainTemplateId.uuidValue()).stream()
        .map(Key::uuidFrom)
        .collect(Collectors.toSet());
  }

  @Override
  public Optional<Domain> findByCatalogItem(CatalogItem catalogItem) {
    return dataRepository
        .findByCatalogsCatalogItemsId(catalogItem.getId().uuidValue())
        .map(Domain.class::cast);
  }

  @Override
  public Optional<Domain> findById(@NonNull Key<UUID> domainId, @NonNull Key<UUID> clientId) {
    return dataRepository.findById(domainId.uuidValue(), clientId.uuidValue());
  }

  @Override
  public Domain getActiveById(Key<UUID> domainId, Key<UUID> clientId) {
    var domain = getById(domainId, clientId);
    if (!domain.isActive()) {
      throw new NotFoundException("Domain is inactive.");
    }
    return domain;
  }

  @Override
  public Domain getById(@NonNull Key<UUID> domainId, @NonNull Key<UUID> clientId) {
    return dataRepository
        .findById(domainId.uuidValue(), clientId.uuidValue())
        .orElseThrow(() -> new NotFoundException(domainId, Domain.class));
  }

  @Override
  public Set<Domain> findByIds(Set<Key<UUID>> ids, @NonNull Key<UUID> clientId) {
    var idStrings = ids.stream().map(Key::uuidValue).toList();
    return StreamSupport.stream(
            dataRepository
                .findAllByDbIdInAndOwnerDbIdIs(idStrings, clientId.uuidValue())
                .spliterator(),
            false)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Domain> getByIds(Set<Key<UUID>> ids, @NonNull Key<UUID> clientId) {
    Set<Domain> result = findByIds(ids, clientId);
    if (result.size() < ids.size()) {
      List<Key<UUID>> foundIds = result.stream().map(Identifiable::getId).toList();
      List<String> unfoundIds =
          ids.stream().filter(Predicate.not(foundIds::contains)).map(Key::uuidValue).toList();
      throw new NotFoundException(
          format(
              "%s %s not found",
              unfoundIds.size() == 1 ? "Domain" : "Domains", String.join(", ", unfoundIds)));
    }
    return result;
  }

  @Override
  public Optional<Domain> findByIdWithProfilesAndRiskDefinitions(Key<UUID> id, Key<UUID> clientId) {
    return dataRepository
        .findByIdWithProfilesAndRiskDefinitions(id.uuidValue(), clientId.uuidValue())
        .map(Domain.class::cast);
  }

  @Override
  public boolean nameExistsInClient(String name, Client client) {
    return dataRepository.nameExistsInClient(name, client);
  }

  @Override
  public Domain getByIdWithDecisions(Key<UUID> domainId, Key<UUID> clientId) {
    return dataRepository
        .findByIdWithDecisions(domainId.uuidValue(), clientId.uuidValue())
        .map(Domain.class::cast)
        .orElseThrow(() -> new NotFoundException(domainId, Domain.class));
  }
}
