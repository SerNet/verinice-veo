/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.persistence.entity.jpa;

import java.util.Map;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.decision.DecisionRef;
import org.veo.core.entity.decision.DecisionResult;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity(name = "decision_results_aspect")
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DecisionResultsAspectData extends AspectData {
  public DecisionResultsAspectData(
      Domain domain, Element owner, Map<DecisionRef, DecisionResult> results) {
    super(domain, owner);
    this.results = Map.copyOf(results);
  }

  @Access(value = AccessType.PROPERTY)
  @Column(columnDefinition = "jsonb")
  @Getter
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<DecisionRef, DecisionResult> results;

  void setResults(Map<DecisionRef, DecisionResult> results) {
    this.results = Map.copyOf(results);
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
