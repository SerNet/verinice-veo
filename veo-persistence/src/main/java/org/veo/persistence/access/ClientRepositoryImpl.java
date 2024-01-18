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

import static java.util.stream.StreamSupport.stream;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.repository.ClientRepository;
import org.veo.persistence.access.jpa.ClientDataRepository;
import org.veo.persistence.access.jpa.DomainDataRepository;
import org.veo.persistence.access.jpa.UserConfigurationDataRepository;
import org.veo.persistence.entity.jpa.ClientData;
import org.veo.persistence.entity.jpa.ValidationService;

@Repository
public class ClientRepositoryImpl
    extends AbstractIdentifiableVersionedRepository<Client, ClientData>
    implements ClientRepository {
  private final ClientDataRepository clientDataRepository;
  private final DomainDataRepository domainDataRepository;
  private final UserConfigurationDataRepository userConfigurationDataRepository;

  public ClientRepositoryImpl(
      ClientDataRepository dataRepository,
      DomainDataRepository domainDataRepository,
      UserConfigurationDataRepository userConfigurationDataRepository,
      ValidationService validator) {
    super(dataRepository, validator);
    clientDataRepository = dataRepository;
    this.domainDataRepository = domainDataRepository;
    this.userConfigurationDataRepository = userConfigurationDataRepository;
  }

  @Override
  public Optional<Client> findByIdFetchCatalogsAndItems(Key<UUID> id) {
    return clientDataRepository
        .findWithCatalogsAndItemsByDbId(id.uuidValue())
        .filter(IS_CLIENT_ACTIVE)
        .map(Client.class::cast);
  }

  @Override
  public Optional<Client> findByIdFetchCatalogsAndItemsAndTailoringReferences(Key<UUID> id) {
    return clientDataRepository
        .findWithCatalogsAndItemsAndTailoringReferencesByDbId(id.uuidValue())
        .filter(IS_CLIENT_ACTIVE)
        .map(Client.class::cast);
  }

  @Override
  public Optional<Client> findByIdFetchTranslations(Key<UUID> id) {
    return clientDataRepository.findWithTranslationsByDbId(id.uuidValue()).map(Client.class::cast);
  }

  @Override
  public List<Client> findAll() {
    return stream(clientDataRepository.findAll().spliterator(), false)
        .map(Client.class::cast)
        .toList();
  }

  @Override
  public Set<Client> findAllActiveWhereDomainTemplateNotApplied(Key<UUID> domainTemplateId) {
    return clientDataRepository
        .findAllActiveWhereDomainTemplateNotApplied(domainTemplateId.uuidValue())
        .stream()
        .map(Client.class::cast)
        .collect(Collectors.toSet());
  }

  @Override
  public void delete(Client client) {
    userConfigurationDataRepository.deleteAll(
        userConfigurationDataRepository.findUserConfigurationsByClient(client.getIdAsString()));
    domainDataRepository.deleteAll(domainDataRepository.findAllByClient(client.getIdAsString()));
    client.setDomains(Set.of());
    super.delete(client);
  }
}
