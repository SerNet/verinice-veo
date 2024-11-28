/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Urs Zeidler
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
package org.veo.listeners;

import static org.veo.core.usecase.unit.CreateUnitUseCase.DEFAULT_MAX_UNITS;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Profile;
import org.veo.core.entity.event.ClientChangedEvent;
import org.veo.core.entity.event.ClientEvent.ClientChangeType;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.unit.DeleteUnitUseCase;
import org.veo.service.DefaultDomainCreator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Component
@Slf4j
public class ClientChangedEventListener {
  private final ClientRepository repository;
  private final DeleteUnitUseCase deleteUnitUseCase;
  private final UnitRepository unitRepository;
  private final DomainRepository domainRepository;
  private final EntityFactory entityFactory;
  private final DefaultDomainCreator defaultDomainCreator;

  @EventListener()
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(ClientChangedEvent event) {
    log.info("ClientChangedEventListener ----> {}", event);
    UUID clientId = event.getClientId();
    if (event.getType() == ClientChangedEvent.ClientChangeType.CREATION) {
      createClient(event);
      return;
    }

    Client client =
        repository
            .findById(clientId)
            .orElseThrow(() -> new AmqpRejectAndDontRequeueException("Client not found"));
    try {
      client.updateState(event.getType());
    } catch (IllegalStateException illEx) {
      throw new AmqpRejectAndDontRequeueException(illEx);
    }
    switch (event.getType()) {
      case ACTIVATION -> log.info("client {} activated", client.getIdAsString());
      case DEACTIVATION -> log.info("client {} deactivated", client.getIdAsString());
      case DELETION -> deleteClient(client);
      case MODIFICATION -> modifyClient(client, event);
      default -> throw new NotImplementedException("Unexpected value: " + event.getType());
    }
  }

  private void createClient(ClientChangedEvent event) {
    log.info("create new client {}", event);
    if (repository.exists(event.getClientId())) {
      throw new AmqpRejectAndDontRequeueException("Client allready exist!");
    }

    Client client = entityFactory.createClient(event.getClientId(), event.getName());
    if (event.getMaxUnits() != null) {
      client.setMaxUnits(event.getMaxUnits());
    } else {
      client.setMaxUnits(DEFAULT_MAX_UNITS);
    }
    client.updateState(ClientChangeType.ACTIVATION);
    addDomains(client, event.getDomainProducts(), false);
    addProfiles(client, event.getDomainProducts(), false);
    repository.save(client);
  }

  private void modifyClient(Client client, ClientChangedEvent event) {
    Integer maxUnits = event.getMaxUnits();
    if (maxUnits != null) {
      log.info("Modify max units for client {} {}", client, maxUnits);
      client.setMaxUnits(maxUnits);
    }
    addDomains(client, event.getDomainProducts(), true);
    addProfiles(client, event.getDomainProducts(), true);
    repository.save(client);
  }

  private void addDomains(Client client, Map<String, List<String>> domainProducts, boolean save) {
    if (domainProducts != null && !domainProducts.isEmpty()) {
      domainProducts
          .keySet()
          .forEach(
              dn -> {
                if (client.getDomains().stream()
                    .filter(hasDomainTemplate())
                    .noneMatch(referenceDomainTemplate(dn))) {
                  log.info("create Domain {} for client {}", dn, client.getName());
                  defaultDomainCreator.addDomain(client, dn, false);
                  if (save) {
                    repository.save(client);
                  }
                }
              });
    }
  }

  private void addProfiles(Client client, Map<String, List<String>> products, boolean save) {
    if (products != null && !products.isEmpty()) {
      client.getDomains().stream()
          .filter(hasDomainTemplate())
          .forEach(
              domain -> {
                products.getOrDefault(domain.getName(), Collections.emptyList()).stream()
                    .distinct()
                    .forEach(
                        productId -> {
                          domain.getDomainTemplate().getProfiles().stream()
                              .filter(isProfile(productId))
                              .forEach(profile -> copyProfileToDomain(save, profile, domain));
                        });
              });
    }
  }

  private void copyProfileToDomain(boolean save, Profile profile, Domain domain) {
    defaultDomainCreator.copyProfileToDomain(profile, domain);
    if (save) {
      domainRepository.save(domain);
    }
  }

  private void deleteClient(Client client) {
    log.info("Delete data for client {}", client);
    unitRepository
        .findByClient(client)
        .forEach(
            unit ->
                deleteUnitUseCase.execute(new DeleteUnitUseCase.InputData(unit.getId(), client)));

    // Reload the client since the persistence context was cleared
    repository.delete(repository.getById(client.getId()));
  }

  private Predicate<? super Domain> referenceDomainTemplate(String templateName) {
    return domain -> domain.getDomainTemplate().getName().equals(templateName);
  }

  private Predicate<? super Profile> isProfile(String productId) {
    return profile -> productId.equals(profile.getProductId());
  }

  private Predicate<? super Domain> hasDomainTemplate() {
    return d -> d.getDomainTemplate() != null;
  }
}
