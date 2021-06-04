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
package org.veo.persistence.access;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Unit;
import org.veo.core.repository.EntityLayerSupertypeQuery;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.QueryCondition;
import org.veo.persistence.access.jpa.EntityLayerSupertypeDataRepository;
import org.veo.persistence.entity.jpa.EntityLayerSupertypeData;
import org.veo.persistence.entity.jpa.UnitData;

/**
 * Implements {@link EntityLayerSupertypeQuery} using {@link Specification} API.
 */
public class EntityLayerSupertypeQueryImpl<TInterface extends EntityLayerSupertype, TDataClass extends EntityLayerSupertypeData>
        implements EntityLayerSupertypeQuery<TInterface> {

    private final EntityLayerSupertypeDataRepository<TDataClass> dataRepository;
    protected Specification<TDataClass> mySpec;

    public EntityLayerSupertypeQueryImpl(EntityLayerSupertypeDataRepository<TDataClass> repo,
            Client client) {
        this.dataRepository = repo;
        mySpec = createSpecification(client);
    }

    @Override
    public EntityLayerSupertypeQuery<TInterface> whereAppliedItemsContains(CatalogItem item) {
        mySpec = mySpec.and((root, query,
                criteriaBuilder) -> criteriaBuilder.equal(root.join("appliedCatalogItems",
                                                                    JoinType.LEFT),
                                                          item));
        return this;
    }

    @Override
    public EntityLayerSupertypeQuery<TInterface> whereOwnerIs(Unit unit) {
        mySpec = mySpec.and((root, query,
                criteriaBuilder) -> criteriaBuilder.equal(root.join("owner"), unit));
        return this;
    }

    @Override
    public EntityLayerSupertypeQuery<TInterface> whereUnitIn(Set<Unit> units) {
        mySpec = mySpec.and((root, query, criteriaBuilder) -> in(root.get("owner"), units,
                                                                 criteriaBuilder));
        return this;
    }

    @Override
    public EntityLayerSupertypeQuery<TInterface> whereSubTypeMatches(
            QueryCondition<String> condition) {
        mySpec = mySpec.and((root, query,
                criteriaBuilder) -> in(root.join("subTypeAspects", JoinType.LEFT)
                                           .get("subType"),
                                       condition.getValues(), criteriaBuilder));
        return this;
    }

    @Override
    public EntityLayerSupertypeQuery<TInterface> whereDisplayNameMatchesIgnoringCase(
            QueryCondition<String> condition) {
        inIgnoringCase(condition, "displayName");
        return this;
    }

    @Override
    public EntityLayerSupertypeQuery<TInterface> whereDescriptionMatchesIgnoreCase(
            QueryCondition<String> condition) {
        inIgnoringCase(condition, "description");
        return this;
    }

    @Override
    public EntityLayerSupertypeQuery<TInterface> whereDesignatorMatchesIgnoreCase(
            QueryCondition<String> condition) {
        inIgnoringCase(condition, "designator");
        return this;
    }

    @Override
    public EntityLayerSupertypeQuery<TInterface> whereNameMatchesIgnoreCase(
            QueryCondition<String> condition) {
        inIgnoringCase(condition, "name");
        return this;
    }

    @Override
    public EntityLayerSupertypeQuery<TInterface> whereUpdatedByContainsIgnoreCase(
            QueryCondition<String> condition) {
        inIgnoringCase(condition, "updatedBy");
        return this;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<TInterface> execute(PagingConfiguration pagingConfiguration) {
        Page<TDataClass> items = dataRepository.findAll(mySpec, toPageable(pagingConfiguration));
        List<String> ids = items.stream()
                                .map(EntityLayerSupertypeData::getDbId)
                                .collect(Collectors.toList());
        List<TDataClass> fullyLoadedItems = dataRepository.findAllById(ids);
        fullyLoadedItems.sort(Comparator.comparingInt(item -> ids.indexOf(item.getDbId())));

        return new PagedResult<>(pagingConfiguration, (List<TInterface>) fullyLoadedItems,
                items.getTotalElements(), items.getTotalPages());
    }

    private Specification<TDataClass> createSpecification(Client client) {
        return (root, query, criteriaBuilder) -> {
            Path<UnitData> unit = criteriaBuilder.treat(root.join("owner"), UnitData.class);
            return criteriaBuilder.equal(unit.get("client"), client);
        };
    }

    protected static Predicate in(Path<Object> column, Set<?> values,
            CriteriaBuilder criteriaBuilder) {
        if (values.stream()
                  .anyMatch(Objects::isNull)) {
            return criteriaBuilder.or(column.in(values), column.isNull());
        } else {
            return criteriaBuilder.isTrue(column.in(values));
        }
    }

    private void inIgnoringCase(QueryCondition<String> condition, String propertyName) {
        mySpec = mySpec.and((root, query,
                criteriaBuilder) -> criteriaBuilder.or(condition.getValues()
                                                                .stream()
                                                                .map(str -> criteriaBuilder.like(criteriaBuilder.upper(root.get(propertyName)),
                                                                                                 "%" + str.toUpperCase(Locale.GERMAN)
                                                                                                         + "%"))
                                                                .toArray(Predicate[]::new)));
    }

    protected static Pageable toPageable(PagingConfiguration pagingConfiguration) {
        String inputSortColumn = pagingConfiguration.getSortColumn();
        String[] sortColumns;
        if ("designator".equals(inputSortColumn)) {
            // The designator is usually of the form [PREFIX]-[COUNTER], i.e.
            // PER-1, PER-2, PER-100, etc. The counter is a number and the
            // sorting should honor that (i.e. 100 should come after 2). We use
            // the fact that longer numbers (in their "normalized" form) are
            // larger than shorter ones.
            sortColumns = new String[] { "designatorLength", "designator" };
        } else {
            sortColumns = new String[] { inputSortColumn };
        }
        return PageRequest.of(pagingConfiguration.getPageNumber(),
                              pagingConfiguration.getPageSize(),
                              pagingConfiguration.getSortOrder() == PagingConfiguration.SortOrder.ASCENDING
                                      ? Direction.ASC
                                      : Direction.DESC,
                              sortColumns);
    }
}
