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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.UserAccessRights;
import org.veo.core.VeoConstants;
import org.veo.core.entity.Asset;
import org.veo.core.entity.AssetRisk;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.ScopeRisk;
import org.veo.core.entity.Unit;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.LinkQuery;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.SubTypeStatusCount;
import org.veo.persistence.access.jpa.AssetDataRepository;
import org.veo.persistence.access.jpa.ControlImplementationDataRepository;
import org.veo.persistence.access.jpa.CustomLinkDataRepository;
import org.veo.persistence.access.jpa.ElementDataRepository;
import org.veo.persistence.access.jpa.ProcessDataRepository;
import org.veo.persistence.access.jpa.RequirementImplementationDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.access.query.ElementQueryFactory;
import org.veo.persistence.access.query.LinkQueryImpl;
import org.veo.persistence.entity.jpa.ControlImplementationData;
import org.veo.persistence.entity.jpa.ElementData;
import org.veo.persistence.entity.jpa.RequirementImplementationData;
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

  private final ControlImplementationDataRepository ciRepository;

  private final RequirementImplementationDataRepository riRepository;
  private final EntityManager em;

  @Override
  public ElementQuery<Element> query(Client client) {
    return elementQueryFactory.queryElements(client);
  }

  @Override
  public Set<SubTypeStatusCount> getCountsBySubType(Unit u, Domain d) {
    return dataRepository.getCountsBySubType(u.getId(), d.getId());
  }

  @Override
  public <T extends Element> Optional<T> findById(
      UUID elementId, Class<T> elementType, UUID clientId) {
    return dataRepository
        .findById(elementId, clientId)
        .filter(e -> e.getModelInterface() == elementType)
        .map(e -> (T) e);
  }

  @Override
  public <T extends Element> Optional<T> findById(
      UUID elementId, Class<? extends Element> elementType, UserAccessRights userRights) {
    return dataRepository
        .findById(
            elementId,
            userRights.clientId(),
            userRights.isUnitAccessResticted(),
            userRights.getReadableUnitIds())
        .map(e -> (T) e);
  }

  @Override
  public LinkQuery queryLinks(Element element, Domain domain) {
    return new LinkQueryImpl(em, dataRepository, element, domain);
  }

  @Override
  @Transactional
  public Collection<Element> saveAll(Collection<Element> entities) {
    return dataRepository.saveAll(entities.stream().map(ElementData.class::cast).toList()).stream()
        .map(Element.class::cast)
        .toList();
  }

  @Override
  @Transactional
  public void deleteAll(Collection<Element> elements) {
    List<UUID> elementUUIDs = elements.stream().map(Element::getId).toList();
    ListUtils.partition(elementUUIDs, VeoConstants.DB_QUERY_CHUNK_SIZE).stream()
        .forEach(batch -> deleteLinksByTargets(Set.copyOf(batch)));

    elements.forEach(e -> e.getLinks().clear());

    // remove risks for all scenarios that are to be deleted
    List<ScenarioData> scenarios =
        elements.stream()
            .filter(it -> it.getModelInterface().equals(Scenario.class))
            .map(ScenarioData.class::cast)
            .toList();
    ListUtils.partition(scenarios, VeoConstants.DB_QUERY_CHUNK_SIZE).stream()
        .forEach(this::removeRisks);

    // remove control and requirement implementations for all risk affected elements that are to be
    // deleted
    var riskAffecteds =
        elements.stream()
            .filter(RiskAffected.class::isInstance)
            .map(it -> (RiskAffected<?, ?>) it)
            .collect(Collectors.toSet());
    if (!riskAffecteds.isEmpty()) {
      removeCIsAndRIsFrom(riskAffecteds);
    }

    // remove elements from scope members:
    Set<Scope> scopes =
        ListUtils.partition(elementUUIDs, VeoConstants.DB_QUERY_CHUNK_SIZE).stream()
            .flatMap(
                batch ->
                    scopeDataRepository.findDistinctOthersByMemberIds(Set.copyOf(batch)).stream())
            .collect(Collectors.toSet());
    scopes.stream()
        .map(ScopeData.class::cast)
        .forEach(scopeData -> scopeData.removeMembersById(elementUUIDs));
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

    List<ElementData> sortedElements =
        elements.stream()
            .sorted(
                Comparator.<Element, Integer>comparing(
                    it -> deletionOrder.indexOf(it.getModelInterface())))
            .map(ElementData.class::cast)
            .toList();
    ListUtils.partition(sortedElements, VeoConstants.DB_QUERY_CHUNK_SIZE).stream()
        .forEach(dataRepository::deleteAll);
  }

  private void removeCIsAndRIsFrom(Set<? extends RiskAffected<?, ?>> riskAffecteds) {
    riskAffecteds.forEach(
        ra -> {
          removeRIs(ra);
          removeCIs(ra);

          Set.copyOf(ra.getControlImplementations())
              .forEach(ci -> ci.getOwner().disassociateControl(ci.getControl()));
          ra.getRequirementImplementations()
              .forEach(ri -> ri.getOrigin().removeRequirementImplementation(ri.getControl()));
        });
  }

  @Override
  @Transactional
  public void deleteByUnit(Unit unit) {
    var unitId = unit.getId();
    em.flush();
    Stream.of(
            "delete from requirement_implementation where origin_db_id in (select db_id from element where owner_id = ?1)",
            "delete from control_implementation where owner_db_id in (select db_id from element where owner_id = ?1)",
            "delete from customlink where source_id in (select db_id from element where owner_id = ?1)",
            "delete from custom_aspect where owner_db_id in (select db_id from element where owner_id = ?1)",
            "delete from decision_results_aspect where owner_db_id in (select db_id from element where owner_id = ?1)",
            "delete from element_domain_association where owner_db_id in (select db_id from element where owner_id = ?1)",
            "delete from scenario_risk_values_aspect where owner_db_id in (select db_id from element where dtype = 'SCENARIO' and owner_id = ?1)",
            "delete from impact_values_aspect where owner_db_id in (select db_id from element where dtype in ('ASSET', 'PROCESS', 'SCOPE') and owner_id = ?1)",
            "delete from scope_risk_values_aspect where owner_db_id in (select db_id from element where dtype = 'SCOPE' and owner_id = ?1)",
            "delete from asset_parts where composite_id in (select db_id from element where dtype = 'ASSET' and owner_id = ?1)",
            "delete from control_parts where composite_id in (select db_id from element where dtype = 'CONTROL' and owner_id = ?1)",
            "delete from document_parts where composite_id in (select db_id from element where dtype = 'DOCUMENT' and owner_id = ?1)",
            "delete from incident_parts where composite_id in (select db_id from element where dtype = 'INCIDENT' and owner_id = ?1)",
            "delete from person_parts where composite_id in (select db_id from element where dtype = 'PERSON' and owner_id = ?1)",
            "delete from process_parts where composite_id in (select db_id from element where dtype = 'PROCESS' and owner_id = ?1)",
            "delete from scenario_parts where composite_id in (select db_id from element where dtype = 'SCENARIO' and owner_id = ?1)",
            "delete from scope_members where scope_id in (select db_id from element where dtype = 'SCOPE' and owner_id = ?1)",
            "delete from riskvalues_aspect a where a.owner_db_id in (select r.db_id from abstractriskdata r where r.entity_db_id in (select db_id from element where dtype in ('ASSET', 'PROCESS', 'SCOPE') and owner_id = ?1))")
        .forEach(
            statement -> em.createNativeQuery(statement).setParameter(1, unitId).executeUpdate());
    em.clear();

    ElementQuery<Element> query = query(unit.getClient());
    query.whereOwnerIs(unit);
    query.fetchAppliedCatalogItems();
    query.fetchParentsAndChildrenAndSiblings();
    query.fetchRisks();
    query.fetchRiskValuesAspects();
    query.fetchControlImplementations();
    query.fetchRequirementImplementations();
    List<Element> elements = query.execute(PagingConfiguration.UNPAGED).getResultPage();

    elements.forEach(
        element -> {
          if (element instanceof RiskAffected<?, ?> ra) {
            ra.getRisks().forEach(em::remove);
          }
        });
    dataRepository.deleteAll(elements.stream().map(ElementData.class::cast).toList());
  }

  private void removeCIs(RiskAffected<?, ?> ra) {
    ListUtils.partition(
            List.copyOf(ra.getControlImplementations()), VeoConstants.DB_QUERY_CHUNK_SIZE)
        .stream()
        .forEach(
            chunk ->
                ciRepository.deleteAll(
                    chunk.stream().map(ControlImplementationData.class::cast).toList()));
  }

  private void removeRIs(RiskAffected<?, ?> ra) {
    ListUtils.partition(
            List.copyOf(ra.getRequirementImplementations()), VeoConstants.DB_QUERY_CHUNK_SIZE)
        .stream()
        .forEach(
            chunk ->
                riRepository.deleteAll(
                    chunk.stream().map(RequirementImplementationData.class::cast).toList()));
  }

  private void removeRisks(Collection<ScenarioData> scenarios) {
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

  private void deleteLinksByTargets(Set<UUID> targetElementIds) {
    // using deleteAll() to utilize batching and optimistic locking:
    var links = linkDataRepository.findLinksFromOtherElementsByTargetIds(targetElementIds);
    linkDataRepository.deleteAll(links);
    links.forEach(CustomLink::remove);
  }
}
