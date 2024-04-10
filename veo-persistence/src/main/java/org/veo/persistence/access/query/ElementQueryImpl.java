/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.persistence.access.query;

import static org.veo.persistence.access.query.QueryFunctions.andInIgnoringCase;
import static org.veo.persistence.access.query.QueryFunctions.in;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import org.apache.commons.collections4.ListUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Asset;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.Control;
import org.veo.core.entity.Document;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Entity;
import org.veo.core.entity.Incident;
import org.veo.core.entity.Key;
import org.veo.core.entity.Person;
import org.veo.core.entity.Process;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.Unit;
import org.veo.core.repository.ElementQuery;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.QueryCondition;
import org.veo.core.repository.SingleValueQueryCondition;
import org.veo.persistence.access.jpa.AssetDataRepository;
import org.veo.persistence.access.jpa.CompositeEntityDataRepository;
import org.veo.persistence.access.jpa.CompositeRiskAffectedDataRepository;
import org.veo.persistence.access.jpa.ControlDataRepository;
import org.veo.persistence.access.jpa.DocumentDataRepository;
import org.veo.persistence.access.jpa.ElementDataRepository;
import org.veo.persistence.access.jpa.IncidentDataRepository;
import org.veo.persistence.access.jpa.PersonDataRepository;
import org.veo.persistence.access.jpa.ProcessDataRepository;
import org.veo.persistence.access.jpa.ScenarioDataRepository;
import org.veo.persistence.access.jpa.ScopeDataRepository;
import org.veo.persistence.entity.jpa.ElementData;
import org.veo.persistence.entity.jpa.RiskAffectedData;

/** Implements {@link ElementQuery} using {@link Specification} API. */
class ElementQueryImpl<TInterface extends Element, TDataClass extends ElementData>
    implements ElementQuery<TInterface> {

  private final ElementDataRepository<TDataClass> dataRepository;
  private final AssetDataRepository assetDataRepository;
  private final ControlDataRepository controlDataRepository;
  private final DocumentDataRepository documentDataRepository;
  private final IncidentDataRepository incidentDataRepository;
  private final PersonDataRepository personDataRepository;
  private final ProcessDataRepository processDataRepository;
  private final ScenarioDataRepository scenarioDataRepository;
  private final ScopeDataRepository scopeDataRepository;
  protected Specification<TDataClass> mySpec;
  private boolean fetchAppliedCatalogItems;
  private boolean fetchScopesAndScopeMembers;
  private boolean fetchRisks;
  private boolean fetchRiskValuesAspects;
  private boolean fetchParts;
  private boolean fetchCompositesAndCompositeParts;
  private boolean fetchMembers;
  private boolean fetchControlImplementations;
  private boolean fetchRequirementImplementations;

  public ElementQueryImpl(
      ElementDataRepository<TDataClass> repo,
      AssetDataRepository assetDataRepository,
      ControlDataRepository controlDataRepository,
      DocumentDataRepository documentDataRepository,
      IncidentDataRepository incidentDataRepository,
      PersonDataRepository personDataRepository,
      ProcessDataRepository processDataRepository,
      ScenarioDataRepository scenarioDataRepository,
      ScopeDataRepository scopeDataRepository,
      Client client) {
    this.assetDataRepository = assetDataRepository;
    this.controlDataRepository = controlDataRepository;
    this.documentDataRepository = documentDataRepository;
    this.incidentDataRepository = incidentDataRepository;
    this.personDataRepository = personDataRepository;
    this.processDataRepository = processDataRepository;
    this.scenarioDataRepository = scenarioDataRepository;
    this.scopeDataRepository = scopeDataRepository;
    this.dataRepository = repo;
    mySpec = createSpecification(client);
  }

  @Override
  public void whereAppliedItemsContain(Collection<CatalogItem> items) {
    mySpec =
        mySpec.and(
            (root, query, criteriaBuilder) ->
                root.join("appliedCatalogItems", JoinType.LEFT).in(items));
  }

  @Override
  public void whereOwnerIs(Unit unit) {
    mySpec =
        mySpec.and(
            (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.join("owner"), unit));
  }

  @Override
  public void whereUnitIn(Set<Unit> units) {
    mySpec =
        mySpec.and((root, query, criteriaBuilder) -> in(root.get("owner"), units, criteriaBuilder));
  }

  @Override
  public void whereIdIn(QueryCondition<String> ids) {
    mySpec =
        mySpec.and(
            (root, query, criteriaBuilder) -> in(root.get("id"), ids.getValues(), criteriaBuilder));
  }

  @Override
  public void whereElementTypeMatches(QueryCondition<String> elementType) {
    mySpec =
        mySpec.and(
            (root, query, criteriaBuilder) ->
                in(root.get("elementType"), elementType.getValues(), criteriaBuilder));
  }

  @Override
  public void whereSubTypeMatches(QueryCondition<String> condition) {
    mySpec =
        mySpec.and(
            (root, query, criteriaBuilder) ->
                in(
                    root.join("subTypeAspects", JoinType.LEFT).get("subType"),
                    condition.getValues(),
                    criteriaBuilder));
  }

  @Override
  public void whereChildElementIn(QueryCondition<Key<UUID>> condition) {
    var childIdsAsString =
        condition.getValues().stream().map(Key::uuidValue).collect(Collectors.toSet());
    mySpec =
        mySpec.and(
            (root, query, criateriaBuilder) ->
                in(
                    root.join(getChildAttributeName(), JoinType.INNER).get("dbId"),
                    childIdsAsString,
                    criateriaBuilder));
  }

  @Override
  public void whereChildElementsPresent(boolean present) {
    mySpec =
        mySpec.and(
            (root, query, criteriaBuilder) ->
                checkNull(
                    root.join(getChildAttributeName(), JoinType.LEFT).get("dbId"),
                    !present,
                    criteriaBuilder));
  }

  @Override
  public void whereParentElementPresent(boolean present) {
    var parentSpec =
        (Specification<TDataClass>)
            (root, query, criteriaBuilder) ->
                checkNull(
                    root.join("scopes", JoinType.LEFT).get("dbId"), !present, criteriaBuilder);
    if (dataRepository instanceof CompositeEntityDataRepository) {
      var compositeSpec =
          (Specification<TDataClass>)
              (root, query, criteriaBuilder) ->
                  checkNull(
                      root.join("composites", JoinType.LEFT).get("dbId"),
                      !present,
                      criteriaBuilder);

      if (present) {
        // scope present or composite present
        parentSpec = parentSpec.or(compositeSpec);
      } else {
        // no scope present and no composite present
        parentSpec = parentSpec.and(compositeSpec);
      }
    }

    mySpec = mySpec.and(parentSpec);
  }

  @Override
  public void whereStatusMatches(QueryCondition<String> condition) {
    mySpec =
        mySpec.and(
            (root, query, criteriaBuilder) ->
                in(
                    root.join("subTypeAspects", JoinType.LEFT).get("status"),
                    condition.getValues(),
                    criteriaBuilder));
  }

  @Override
  public void whereDisplayNameMatchesIgnoringCase(QueryCondition<String> condition) {
    inIgnoringCase(condition, "displayName");
  }

  @Override
  public void whereDescriptionMatchesIgnoreCase(QueryCondition<String> condition) {
    inIgnoringCase(condition, "description");
  }

  @Override
  public void whereDesignatorMatchesIgnoreCase(QueryCondition<String> condition) {
    inIgnoringCase(condition, "designator");
  }

  @Override
  public void whereNameMatchesIgnoreCase(QueryCondition<String> condition) {
    inIgnoringCase(condition, "name");
  }

  @Override
  public void whereAbbreviationMatchesIgnoreCase(QueryCondition<String> condition) {
    inIgnoringCase(condition, "abbreviation");
  }

  @Override
  public void whereUpdatedByContainsIgnoreCase(QueryCondition<String> condition) {
    inIgnoringCase(condition, "updatedBy");
  }

  @Override
  public void whereDomainsContain(Domain domain) {
    mySpec =
        mySpec.and(
            (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.join("domains"), domain));
  }

  @Override
  public void whereScopesContain(SingleValueQueryCondition<Key<UUID>> condition) {
    mySpec =
        mySpec.and(
            (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                    root.join("scopes", JoinType.LEFT).get("dbId"),
                    condition.getValue().uuidValue()));
  }

  @Override
  public void fetchAppliedCatalogItems() {
    fetchAppliedCatalogItems = true;
  }

  @Override
  public void fetchParentsAndChildrenAndSiblings() {
    fetchScopesAndScopeMembers = true;
    fetchParts = true;
    fetchCompositesAndCompositeParts = true;
    fetchMembers = true;
  }

  @Override
  public void fetchControlImplementations() {
    fetchControlImplementations = true;
  }

  @Override
  public void fetchRequirementImplementations() {
    fetchRequirementImplementations = true;
  }

  @Override
  public void fetchRisks() {
    fetchRisks = true;
  }

  @Override
  public void fetchChildren() {
    fetchParts = true;
    fetchMembers = true;
  }

  @Override
  public void fetchRiskValuesAspects() {
    fetchRiskValuesAspects = true;
  }

  @Override
  @Transactional(readOnly = true)
  public PagedResult<TInterface> execute(PagingConfiguration pagingConfiguration) {
    Page<TDataClass> items = dataRepository.findAll(mySpec, toPageable(pagingConfiguration));
    List<String> ids = items.stream().map(ElementData::getDbId).toList();
    fullyLoadItems(ids);

    return new PagedResult<>(
        pagingConfiguration,
        items.stream().map(it -> (TInterface) it).toList(),
        items.getTotalElements(),
        items.getTotalPages());
  }

  private void fullyLoadItems(List<String> ids) {
    var items =
        ListUtils.partition(ids, 10000).stream()
            .flatMap(
                batch -> {
                  var chunk = dataRepository.findAllWithDomainsLinksDecisionsByDbIdIn(batch);
                  dataRepository.findAllWithCustomAspectsByDbIdIn(batch);
                  dataRepository.findAllWithSubtypeAspectsByDbIdIn(batch);

                  if (fetchAppliedCatalogItems) {
                    dataRepository.findAllWithAppliedCatalogItemsByDbIdIn(batch);
                  }
                  if (fetchScopesAndScopeMembers) {
                    dataRepository.findAllWithScopesAndScopeMembersByDbIdIn(batch);
                  }
                  return chunk.stream();
                });
    items.collect(Collectors.groupingBy(Element::getModelInterface)).forEach(this::fullyLoadItems);
  }

  private void fullyLoadItems(Class<? extends Entity> type, List<TDataClass> items) {
    var ids = items.stream().map(Element::getIdAsString).toList();
    ListUtils.partition(ids, 10000)
        .forEach(
            batch -> {
              if (type.equals(Asset.class)) {
                fetchCompositeRiskAffected(batch, assetDataRepository);
                if (fetchRiskValuesAspects) {
                  assetDataRepository.findAllWithRiskValuesAspectsByDbIdIn(batch);
                }
              } else if (type.equals(Control.class)) {
                fetchComposites(batch, controlDataRepository);
                if (fetchRiskValuesAspects) {
                  controlDataRepository.findAllWithRiskValuesAspectsByDbIdIn(batch);
                }
              } else if (type.equals(Document.class)) {
                fetchComposites(batch, documentDataRepository);
              } else if (type.equals(Incident.class)) {
                fetchComposites(batch, incidentDataRepository);
              } else if (type.equals(Process.class)) {
                fetchCompositeRiskAffected(batch, processDataRepository);
                if (fetchRiskValuesAspects) {
                  processDataRepository.findAllWithRiskValuesAspectsByDbIdIn(batch);
                }
              } else if (type.equals(Person.class)) {
                fetchComposites(batch, personDataRepository);
              } else if (type.equals(Scenario.class)) {
                fetchComposites(batch, scenarioDataRepository);
                if (fetchRiskValuesAspects) {
                  scenarioDataRepository.findAllWithRiskValuesAspectsByDbIdIn(batch);
                }
              } else if (type.equals(Scope.class)) {
                if (fetchMembers) {
                  scopeDataRepository.findAllWithMembersByDbIdIn(batch);
                }
                if (fetchRiskValuesAspects) {
                  scopeDataRepository.findAllWithRiskValuesAspectsByDbIdIn(batch);
                }
                if (fetchRisks) {
                  scopeDataRepository.findAllWithRisksByDbIdIn(batch);
                }
                if (fetchRequirementImplementations || fetchControlImplementations) {
                  scopeDataRepository.findAllWithCIsAndRIs(batch);
                }
              }
            });
  }

  private <
          T extends RiskAffected<T, TRisk> & CompositeElement<T>,
          TData extends RiskAffectedData<T, TRisk> & CompositeElement<T>,
          TRisk extends AbstractRisk<T, TRisk>>
      void fetchCompositeRiskAffected(
          List<String> ids, CompositeRiskAffectedDataRepository<TData> repo) {
    fetchComposites(ids, repo);
    if (fetchRisks) {
      repo.findAllWithRisksByDbIdIn(ids);
    }
    if (fetchRequirementImplementations || fetchControlImplementations) {
      repo.findAllWithCIsAndRIs(ids);
    }
  }

  private void fetchComposites(
      List<String> ids, CompositeEntityDataRepository<? extends ElementData> repo) {
    if (fetchParts) {
      repo.findAllWithPartsByDbIdIn(ids);
    }
    if (fetchCompositesAndCompositeParts) {
      repo.findAllWithCompositesAndCompositesPartsByDbIdIn(ids);
    }
  }

  private Specification<TDataClass> createSpecification(Client client) {
    return (root, query, criteriaBuilder) -> {
      query.distinct(true);
      return criteriaBuilder.equal(root.join("owner").get("client"), client);
    };
  }

  private void inIgnoringCase(QueryCondition<String> condition, String propertyName) {
    mySpec = andInIgnoringCase(propertyName, condition, mySpec);
  }

  private Predicate checkNull(
      Path<Object> pathExpression, boolean assertNull, CriteriaBuilder criteriaBuilder) {
    if (assertNull) {
      return criteriaBuilder.isNull(pathExpression);
    }
    return criteriaBuilder.isNotNull(pathExpression);
  }

  private String getChildAttributeName() {
    if (dataRepository instanceof CompositeEntityDataRepository) {
      return "parts";
    }
    if (dataRepository instanceof ScopeDataRepository) {
      return "members";
    }
    throw new UnsupportedOperationException("Cannot filter by child elements");
  }

  private static Pageable toPageable(PagingConfiguration pagingConfiguration) {
    return PageRequest.of(
        pagingConfiguration.getPageNumber(),
        pagingConfiguration.getPageSize(),
        pagingConfiguration.getSortOrder() == PagingConfiguration.SortOrder.ASCENDING
            ? Direction.ASC
            : Direction.DESC,
        pagingConfiguration.getSortColumn());
  }
}
