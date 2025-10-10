/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.Client;
import org.veo.core.repository.ClientRepository;
import org.veo.persistence.access.jpa.ClientDataRepository;
import org.veo.persistence.entity.jpa.ClientData;
import org.veo.persistence.entity.jpa.ValidationService;

@Repository
public class ClientRepositoryImpl
    extends AbstractIdentifiableVersionedRepository<Client, ClientData>
    implements ClientRepository {
  private final ClientDataRepository clientDataRepository;
  private final EntityManager entityManager;

  public ClientRepositoryImpl(
      ClientDataRepository dataRepository,
      ValidationService validator,
      EntityManager entityManager) {
    super(dataRepository, validator);
    clientDataRepository = dataRepository;
    this.entityManager = entityManager;
  }

  @Override
  public Optional<Client> findByIdFetchTranslations(UUID id) {
    return clientDataRepository.findWithTranslationsById(id).map(Client.class::cast);
  }

  @Override
  public Set<Client> findAllActiveWhereDomainTemplateNotApplied(UUID domainTemplateId) {
    return clientDataRepository
        .findAllActiveWhereDomainTemplateNotApplied(domainTemplateId)
        .stream()
        .map(Client.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Client> findAllActiveWhereDomainTemplateNotAppliedAndWithDomainTemplateOfName(
      UUID domainTemplateId, String name) {
    return clientDataRepository
        .findAllActiveWhereDomainTemplateNotAppliedAndWithDomainTemplateOfName(
            domainTemplateId, name)
        .stream()
        .map(Client.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public void delete(Client client) {
    clientDataRepository.prepareForClientDeletion(client.getId());
    entityManager.clear();
    super.delete(client);
  }
}
