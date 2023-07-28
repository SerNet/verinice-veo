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
package org.veo.persistence.access.query;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import org.veo.core.repository.QueryCondition;

public class QueryFunctions {
  static <T> Specification<T> andInIgnoringCase(
      String propertyName, QueryCondition<String> condition, Specification<T> spec) {
    return spec.and(
        (root, query, criteriaBuilder) ->
            criteriaBuilder.or(
                condition.getValues().stream()
                    .map(
                        str ->
                            criteriaBuilder.like(
                                criteriaBuilder.lower(root.get(propertyName)),
                                "%" + str.toLowerCase(Locale.GERMAN) + "%"))
                    .toArray(Predicate[]::new)));
  }

  static Predicate in(Path<Object> column, Collection<?> values, CriteriaBuilder criteriaBuilder) {
    if (values.stream().anyMatch(Objects::isNull)) {
      if (values.size() == 1) {
        return column.isNull();
      } else {
        return criteriaBuilder.or(
            column.in(values.stream().filter(Objects::nonNull).toList()), column.isNull());
      }
    } else {
      return criteriaBuilder.isTrue(column.in(values));
    }
  }

  static <T, V> Specification<T> andIn(
      Specification<T> spec, String propertyName, QueryCondition<V> condition) {
    return spec.and(
        (root, query, criteriaBuilder) ->
            QueryFunctions.in(root.get(propertyName), condition.getValues(), criteriaBuilder));
  }
}
