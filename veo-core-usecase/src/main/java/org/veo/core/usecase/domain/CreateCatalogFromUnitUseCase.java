/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler.
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
package org.veo.core.usecase.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.Profile;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.EmptyOutput;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateCatalogFromUnitUseCase
    extends AbstractCreateItemsFromUnitUseCase<CatalogItem, DomainBase>
    implements TransactionalUseCase<CreateCatalogFromUnitUseCase.InputData, EmptyOutput> {

  private final CatalogItemRepository catalogItemRepository;

  public CreateCatalogFromUnitUseCase(
      GenericElementRepository genericElementRepository,
      UnitRepository unitRepository,
      DomainRepository domainRepository,
      EntityFactory factory,
      CatalogItemRepository catalogItemRepository) {
    super(factory, domainRepository, genericElementRepository, unitRepository);
    this.catalogItemRepository = catalogItemRepository;
  }

  @Override
  public EmptyOutput execute(InputData input, UserAccessRights userAccessRights) {
    Domain domain = domainRepository.getById(input.domainId, userAccessRights.clientId());
    Set<CatalogItem> previousCatalogItems = Set.copyOf(domain.getCatalogItems());

    Client client = input.authenticatedClient;
    if (!client.equals(domain.getOwner())) {
      throw new ClientBoundaryViolationException(domain, client);
    }
    if (!domain.isActive()) {
      throw new NotFoundException("Domain is inactive.");
    }
    var unit = unitRepository.getById(input.unitId);
    unit.checkSameClient(client);
    Set<Element> elements = getElements(unit, domain);

    deleteObsoleteCatalogItems(domain, client, elements);
    Map<Element, CatalogItem> elementsToCatalogItems =
        elements.stream()
            .collect(Collectors.toMap(Function.identity(), e -> e.toCatalogItem(domain)));
    createTailorreferences(elementsToCatalogItems, domain);

    Set<CatalogItem> newCatalogItems =
        elementsToCatalogItems.values().stream()
            .filter(Predicate.not(previousCatalogItems::contains))
            .collect(Collectors.toSet());
    catalogItemRepository.saveAll(newCatalogItems);
    log.info(
        "new catalog in domain {} with {} elements created", domain.getName(), elements.size());
    domain.setUpdatedAt(Instant.now());
    return EmptyOutput.INSTANCE;
  }

  private Set<Element> getElements(Unit unit, Domain domain) {
    var query = genericElementRepository.query(unit.getClient());
    query.whereUnitIn(Set.of(unit));
    query.whereDomainsContain(domain);
    var elements = new HashSet<>(query.execute(PagingConfiguration.UNPAGED).getResultPage());
    return elements;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  private void deleteObsoleteCatalogItems(
      Domain domain, Client client, Collection<Element> newElements) {
    var relevantItems =
        newElements.stream().flatMap(e -> e.findAppliedCatalogItem(domain).stream()).toList();
    var obsoleteItems =
        domain.getCatalogItems().stream().filter(ci -> !relevantItems.contains(ci)).toList();
    var query = genericElementRepository.query(client);
    query.fetchAppliedCatalogItems();
    query.whereAppliedItemIn(obsoleteItems, domain);
    var incarnations = query.execute(PagingConfiguration.UNPAGED).getResultPage();

    log.info(
        "removing references to obsolete catalog items from {} incarnations", incarnations.size());
    incarnations.forEach(e -> e.setAppliedCatalogItem(domain, null));

    log.info("remove references to obsolete catalog items from profiles");
    domain.getProfiles().stream()
        .map(Profile::getItems)
        .flatMap(Collection::stream)
        .filter(i -> obsoleteItems.contains(i.getAppliedCatalogItem()))
        .forEach(i -> i.setAppliedCatalogItem(null));

    log.info("removing {} obsolete catalog items", obsoleteItems.size());
    obsoleteItems.forEach(TemplateItem::clearTailoringReferences);
    domain.getCatalogItems().removeAll(obsoleteItems);
  }

  @Valid
  public record InputData(UUID domainId, Client authenticatedClient, UUID unitId)
      implements UseCase.InputData {}
}
