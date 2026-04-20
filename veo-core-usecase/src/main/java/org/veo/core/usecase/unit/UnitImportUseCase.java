/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.core.usecase.unit;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Unit;
import org.veo.core.entity.event.UnitImpactRecalculatedEvent;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.entity.ref.TypedSymbolicId;
import org.veo.core.entity.state.ElementState;
import org.veo.core.entity.state.RiskState;
import org.veo.core.entity.state.UnitState;
import org.veo.core.repository.CatalogItemRepository;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.service.EventPublisher;
import org.veo.core.usecase.RetryableUseCase;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.base.DomainSensitiveElementValidator;
import org.veo.core.usecase.domain.ElementBatchCreator;
import org.veo.core.usecase.service.EntityStateMapper;
import org.veo.core.usecase.service.IdRefResolver;
import org.veo.core.usecase.service.LocalRefResolver;
import org.veo.core.usecase.service.RefResolverFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnitImportUseCase
    implements TransactionalUseCase<UnitImportUseCase.InputData, UnitImportUseCase.OutputData>,
        RetryableUseCase {
  private final ClientRepository clientRepository;
  private final UnitRepository unitRepository;
  private final DomainRepository domainRepository;
  private final CatalogItemRepository catalogItemRepository;
  private final RefResolverFactory refResolverFactory;
  private final EntityStateMapper entityStateMapper;
  private final ElementBatchCreator elementBatchCreator;
  private final EventPublisher eventPublisher;

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    userAccessRights.checkUnitCreateAllowed();
    var client = clientRepository.getActiveById(userAccessRights.getClientId());
    client.incrementTotalUnits(userAccessRights.getMaxUnits());

    var domainIdMapping = buildDomainMapping(input.domainMetadata, client.getId());
    var resolver = refResolverFactory.local();
    domainIdMapping.forEach(
        (exportedId, clientDomainId) -> {
          var domain = domainRepository.getById(clientDomainId, client.getId());
          var catalogItems = catalogItemRepository.findAllByDomain(domain);
          injectDomainWithCatalogItems(resolver, domain, catalogItems, exportedId);
        });

    var unit = resolver.injectNewEntity(TypedId.from(input.unit.getId(), Unit.class));
    var elements =
        input.elements.stream()
            .map(
                e ->
                    (Element)
                        resolver.injectNewEntity(TypedId.from(e.getId(), e.getModelInterface())))
            .toList();

    unit.setClient(client);
    entityStateMapper.mapState(input.unit, unit, resolver);
    elements.forEach(e -> e.setOwner(unit));
    input.elements.stream()
        .sorted(Comparator.comparing(e -> !e.getModelInterface().equals(Control.class)))
        .forEach(e -> mapElement(e, resolver));
    input.risks.forEach(r -> entityStateMapper.mapState(r, resolver, domainIdMapping));

    elementBatchCreator.create(elements, unitRepository.save(unit));
    try {
      elements.forEach(DomainSensitiveElementValidator::validate);
    } catch (IllegalArgumentException illEx) {
      throw new UnprocessableDataException(illEx.getMessage());
    }
    eventPublisher.publish(UnitImpactRecalculatedEvent.from(unit, this));
    return new OutputData(unit);
  }

  private Map<UUID, UUID> buildDomainMapping(Set<DomainMetadata> domainMetadatas, UUID clientId) {
    if (domainMetadatas.isEmpty()) {
      return Map.of();
    }

    var clientDomains = domainRepository.findAllActiveByClient(clientId);
    var clientDomainById = clientDomains.stream().collect(Collectors.toMap(Domain::getId, d -> d));

    var domainIdMapping = new HashMap<UUID, UUID>();

    for (var domainMetadata : domainMetadatas) {
      var clientDomain = clientDomainById.get(domainMetadata.id());

      if (clientDomain == null) {
        clientDomain = getClientDomainByMetadata(clientDomains, domainMetadata);
      }
      domainIdMapping.put(domainMetadata.id(), clientDomain.getId());
    }

    return domainIdMapping;
  }

  private Domain getClientDomainByMetadata(
      Set<Domain> clientDomains, DomainMetadata exportedDomain) {
    var candidatesByVersion =
        clientDomains.stream()
            .filter(
                d ->
                    d.getName().equals(exportedDomain.name())
                        && d.getAuthority().equals(exportedDomain.authority()))
            .collect(Collectors.groupingBy(d -> d.getTemplateVersion().toString()));
    if (candidatesByVersion.isEmpty()) {
      throw new UnprocessableDataException(
          "Domain '%s' (authority: %s) is not available in the target client."
              .formatted(exportedDomain.name(), exportedDomain.authority()));
    }
    if (candidatesByVersion.containsKey(exportedDomain.templateVersion)) {
      var candidatesWithVersion = candidatesByVersion.get(exportedDomain.templateVersion);
      if (candidatesWithVersion.size() > 1) {
        throw new UnprocessableDataException(
            "Cannot perform cross-client import: the target client has multiple domains named '%s' (authority: %s, version: %s)."
                .formatted(
                    exportedDomain.name(),
                    exportedDomain.authority(),
                    exportedDomain.templateVersion()));
      }
      return candidatesWithVersion.get(0);
    }

    throw new UnprocessableDataException(
        "Domain '%s' (authority: %s) exists in the target client but not in version %s. Available versions: %s."
            .formatted(
                exportedDomain.name(),
                exportedDomain.authority(),
                exportedDomain.templateVersion(),
                String.join(", ", candidatesByVersion.keySet().stream().sorted().toList())));
  }

  private void injectDomainWithCatalogItems(
      LocalRefResolver resolver,
      Domain domain,
      Set<CatalogItem> catalogItems,
      UUID exportedDomainId) {
    resolver.inject(domain, TypedId.from(exportedDomainId, Domain.class));
    for (CatalogItem catalogItem : catalogItems) {
      var aliasRef =
          TypedSymbolicId.from(
              catalogItem.getSymbolicId(),
              CatalogItem.class,
              TypedId.from(exportedDomainId, Domain.class));
      resolver.inject(catalogItem, aliasRef);
    }
  }

  private <T extends Element, TState extends ElementState<T>> void mapElement(
      TState source, IdRefResolver resolver) {
    var target = resolver.resolve(TypedId.from(source.getId(), source.getModelInterface()));
    entityStateMapper.mapState(source, target, true, false, false, resolver);
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public int getMaxAttempts() {
    return 5;
  }

  public record DomainMetadata(UUID id, String name, String authority, String templateVersion) {}

  public record InputData(
      UnitState unit,
      Set<ElementState<?>> elements,
      Set<RiskState<?, ?>> risks,
      Set<DomainMetadata> domainMetadata)
      implements UseCase.InputData {}

  public record OutputData(Unit unit) implements UseCase.OutputData {}
}
