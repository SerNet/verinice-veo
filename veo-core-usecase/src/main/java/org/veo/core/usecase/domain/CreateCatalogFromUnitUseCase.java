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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.LinkTailoringReference;
import org.veo.core.entity.Scope;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.EmptyOutput;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateCatalogFromUnitUseCase
    implements TransactionalUseCase<CreateCatalogFromUnitUseCase.InputData, EmptyOutput> {

  private final GenericElementRepository genericElementRepository;

  private final UnitRepository unitRepository;
  private final DomainRepository domainRepository;
  private final EntityFactory factory;

  public CreateCatalogFromUnitUseCase(
      GenericElementRepository genericElementRepository,
      UnitRepository unitRepository,
      DomainRepository domainRepository,
      EntityFactory factory) {
    super();
    this.genericElementRepository = genericElementRepository;
    this.unitRepository = unitRepository;
    this.domainRepository = domainRepository;
    this.factory = factory;
  }

  public EmptyOutput execute(InputData input) {
    Domain domain =
        domainRepository.getById(input.getDomainId(), input.authenticatedClient.getId());
    Client client = input.getAuthenticatedClient();
    if (!client.equals(domain.getOwner())) {
      throw new ClientBoundaryViolationException(domain, client);
    }
    if (!domain.isActive()) {
      throw new NotFoundException("Domain is inactive.");
    }
    var unit = unitRepository.getById(input.unitId);
    unit.checkSameClient(client);

    cleanCatalogItems(domain, client);
    Set<Element> elements = getElements(unit, domain);
    Map<Element, CatalogItem> elementsToCatalogItems =
        getElements(unit, domain).stream()
            .collect(Collectors.toMap(Function.identity(), e -> e.toCatalogItem(domain)));
    createTailorreferences(elementsToCatalogItems, domain);

    domainRepository.save(domain);
    log.info(
        "new catalog in domain {} with {} elements created", domain.getName(), elements.size());
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

  private void cleanCatalogItems(Domain domain, Client client) {
    var query = genericElementRepository.query(client);
    query.fetchAppliedCatalogItems();
    Set<CatalogItem> list = domain.getCatalogItems();
    query.whereAppliedItemsContain(list);
    var incarnations = query.execute(PagingConfiguration.UNPAGED).getResultPage();
    log.info("number of applied items {}", incarnations.size());

    incarnations.forEach(e -> e.getAppliedCatalogItems().removeAll(list));
    list.forEach(
        ci -> {
          ci.getTailoringReferences().forEach(tr -> tr.setOwner(null));
          ci.getTailoringReferences().clear();
        });
    domain.getCatalogItems().clear();
  }

  private void createTailorreferences(
      Map<Element, CatalogItem> elementsToCatalogItems, Domain domain) {
    elementsToCatalogItems.forEach(
        (element, source) -> {
          element
              .getLinks(domain)
              .forEach(
                  link -> {
                    createLinkReference(
                        link,
                        source,
                        elementsToCatalogItems.get(link.getTarget()),
                        TailoringReferenceType.LINK);
                    createLinkReference(
                        link,
                        elementsToCatalogItems.get(link.getTarget()),
                        source,
                        TailoringReferenceType.LINK_EXTERNAL);
                  });

          if (element instanceof CompositeElement<?> composite) {
            composite
                .getParts()
                .forEach(
                    target -> {
                      CatalogItem partCatalogItem = elementsToCatalogItems.get(target);
                      createTailoringReference(
                          partCatalogItem, source, TailoringReferenceType.COMPOSITE);
                    });
            composite
                .getComposites()
                .forEach(
                    target -> {
                      CatalogItem compositeCatalogItem = elementsToCatalogItems.get(target);
                      createTailoringReference(
                          compositeCatalogItem, source, TailoringReferenceType.PART);
                    });
          } else if (element instanceof Scope scope) {
            if (!scope.getMembers().isEmpty())
              log.info("Skip {} members of: {}", scope.getMembers().size(), scope.getDisplayName());
          }
        });
  }

  private void createTailoringReference(
      CatalogItem source, CatalogItem target, TailoringReferenceType type) {
    TailoringReference tailoringReference = factory.createTailoringReference(source, type);
    tailoringReference.setCatalogItem(target);
  }

  private LinkTailoringReference createLinkReference(
      CustomLink link, CatalogItem source, CatalogItem target, TailoringReferenceType type) {
    LinkTailoringReference reference = factory.createLinkTailoringReference(source, type);
    reference.setAttributes(link.getAttributes());
    reference.setCatalogItem(target);
    reference.setLinkType(link.getType());
    return reference;
  }

  @Valid
  @Value
  public static class InputData implements UseCase.InputData {
    Key<UUID> domainId;
    Client authenticatedClient;
    Key<UUID> unitId;
  }
}
