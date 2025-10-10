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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Identifiable;
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
  private final EntityManager em;

  public DomainRepositoryImpl(
      DomainDataRepository dataRepository, EntityManager em, ValidationService validator) {
    super(dataRepository, validator);
    this.dataRepository = dataRepository;
    this.em = em;
  }

  @Override
  public Set<Domain> findAllActiveByClient(UUID clientId) {
    return dataRepository.findAllActiveByClient(clientId).stream()
        .map(Domain.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Domain> findActiveDomainsWithProfilesAndRiskDefinitions(UUID clientId) {
    return dataRepository.findActiveDomainsWithProfilesAndRiskDefinitions(clientId).stream()
        .map(Domain.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Domain> findActiveByIdsAndClientWithEntityTypeDefinitionsAndRiskDefinitions(
      Collection<UUID> domainIds, UUID clientId) {
    return dataRepository
        .findActiveByIdsAndClientWithEntityTypeDefinitionsAndRiskDefinitions(
            domainIds.stream().toList(), clientId)
        .stream()
        .map(Domain.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<UUID> findIdsByTemplateId(UUID domainTemplateId) {
    return dataRepository.findIdsByDomainTemplateId(domainTemplateId).stream()
        .map(UUID::fromString)
        .collect(Collectors.toSet());
  }

  @Override
  public Optional<Domain> findById(@NonNull UUID domainId, @NonNull UUID clientId) {
    return dataRepository.findById(domainId, clientId);
  }

  @Override
  public Domain getActiveById(UUID domainId, UUID clientId) {
    var domain = getById(domainId, clientId);
    if (!domain.isActive()) {
      throw new NotFoundException("Domain is inactive.");
    }
    return domain;
  }

  @Override
  public Domain getById(@NonNull UUID domainId, @NonNull UUID clientId) {
    return dataRepository
        .findById(domainId, clientId)
        .orElseThrow(() -> new NotFoundException(domainId, Domain.class));
  }

  @Override
  public Set<Domain> getByIds(Set<UUID> ids, @NonNull UUID clientId) {
    Set<Domain> result = dataRepository.findAllByIdInAndOwnerIdIs(ids, clientId);
    if (result.size() < ids.size()) {
      List<UUID> foundIds = result.stream().map(Identifiable::getId).toList();
      List<String> unfoundIds =
          ids.stream().filter(Predicate.not(foundIds::contains)).map(UUID::toString).toList();
      throw new NotFoundException(
          format(
              "%s %s not found",
              unfoundIds.size() == 1 ? "Domain" : "Domains", String.join(", ", unfoundIds)));
    }
    return result;
  }

  @Override
  public Optional<Domain> findByIdWithProfilesAndRiskDefinitions(UUID id, UUID clientId) {
    return dataRepository
        .findByIdWithProfilesAndRiskDefinitions(id, clientId)
        .map(Domain.class::cast);
  }

  @Override
  public boolean nameExistsInClient(String name, Client client) {
    return dataRepository.nameExistsInClient(name, client);
  }

  @Override
  public Domain getByIdWithDecisionsAndInspections(UUID domainId, UUID clientId) {
    return dataRepository
        .findByIdWithDecisionsAndInspections(domainId, clientId)
        .map(Domain.class::cast)
        .orElseThrow(() -> new NotFoundException(domainId, Domain.class));
  }
}
