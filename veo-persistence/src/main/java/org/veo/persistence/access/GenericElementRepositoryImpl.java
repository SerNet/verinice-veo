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
package org.veo.persistence.access;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Asset;
import org.veo.core.entity.AssetRisk;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Document;
import org.veo.core.entity.Element;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Key;
import org.veo.core.entity.Person;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.ScopeRisk;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.GenericElementRepository;
import org.veo.persistence.access.jpa.AssetDataRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ElementDataRepository;
import org.veo.persistence.access.jpa.ProcessDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.access.query.ElementQueryFactory;
import org.veo.persistence.entity.jpa.ElementData;
import org.veo.persistence.entity.jpa.ScenarioData;
import org.veo.persistence.entity.jpa.ScopeData;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class GenericElementRepositoryImpl implements GenericElementRepository {
  private final ElementQueryFactory elementQueryFactory;
  private final ElementDataRepository<ElementData> dataRepository;
  private final AssetDataRepository assetDataRepository;
  private final ProcessDataRepository processDataRepository;
  private final ScopeDataRepository scopeDataRepository;
  private final CustomLinkDataRepository linkDataRepository;

  @Override
  public ElementQuery<Element> query(Client client) {
    return elementQueryFactory.queryElements(client);
  }

  @Override
  @Transactional
  public void deleteAll(Collection<Element> elements) {
    Set<Key<UUID>> elementKeys = elements.stream().map(Element::getId).collect(Collectors.toSet());
    Set<String> elementUUIDs = elementKeys.stream().map(Key::uuidValue).collect(Collectors.toSet());
    deleteLinksByTargets(elementUUIDs);
    elements.forEach(e -> e.getLinks().clear());

    Set<ScenarioData> scenarios =
        elements.stream()
            .filter(it -> it.getModelInterface().equals(Scenario.class))
            .map(ScenarioData.class::cast)
            .collect(Collectors.toSet());
    if (!scenarios.isEmpty()) {
      removeRisks(scenarios);
    }

    // remove elements from scope members:
    Set<Scope> scopes = scopeDataRepository.findDistinctOthersByMemberIds(elementUUIDs);
    scopes.stream()
        .map(ScopeData.class::cast)
        .forEach(scopeData -> scopeData.removeMembersById(elementKeys));
    elements.forEach(Element::remove);
    // First remove the owning side of bi-directional associations
    // (members/risks), then delete the other elements in a predictable order
    List<Class<? extends Object>> deletionOrder =
        List.of(
            Scope.class,
            Process.class,
            Asset.class,
            Scenario.class,
            Control.class,
            Document.class,
            Incident.class,
            Person.class);

    dataRepository.deleteAll(
        elements.stream()
            .sorted(
                Comparator.<Element, Integer>comparing(
                    it -> deletionOrder.indexOf(it.getModelInterface())))
            .map(ElementData.class::cast)
            .toList());
  }

  private void removeRisks(Set<ScenarioData> scenarios) {
    // remove risks associated with these scenarios:
    var assets = assetDataRepository.findDistinctByRisks_ScenarioIn(scenarios);
    assets.forEach(
        assetData ->
            scenarios.forEach(
                scenario -> assetData.getRisk(scenario).ifPresent(AssetRisk::remove)));

    var processes = processDataRepository.findRisksWithValue(scenarios);
    processes.forEach(
        processData ->
            scenarios.forEach(
                scenario -> processData.getRisk(scenario).ifPresent(ProcessRisk::remove)));

    var scopes = scopeDataRepository.findRisksWithValue(scenarios);
    scopes.forEach(
        scopeData ->
            scenarios.forEach(
                scenario -> scopeData.getRisk(scenario).ifPresent(ScopeRisk::remove)));
  }

  private void deleteLinksByTargets(Set<String> targetElementIds) {
    // using deleteAll() to utilize batching and optimistic locking:
    var links = linkDataRepository.findLinksFromOtherElementsByTargetIds(targetElementIds);
    linkDataRepository.deleteAll(links);
    links.forEach(CustomLink::remove);
  }
}
