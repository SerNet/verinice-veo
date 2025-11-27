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
package org.veo.persistence.entity.jpa;

import static jakarta.persistence.GenerationType.SEQUENCE;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.veo.core.entity.decision.Decision;
import org.veo.core.entity.exception.NotFoundException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
@Entity(name = "decision_set")
public class DecisionSetData {
  @Id
  @GeneratedValue(strategy = SEQUENCE, generator = "seq_decision_sets")
  @SequenceGenerator(name = "seq_decision_sets")
  private Long id;

  @NotNull
  @JdbcTypeCode(SqlTypes.JSON)
  Map<String, Decision> decisions = new HashMap<>();

  public void setDecisions(Map<String, Decision> decisions) {
    this.decisions.clear();
    this.decisions.putAll(decisions);
  }

  /**
   * @return {@code true} if new decision was added, {@code false} if existing decision was updated
   */
  public boolean applyDecision(String key, Decision value) {
    var created = !decisions.containsKey(key);
    decisions.put(key, value);
    return created;
  }

  public void removeDecision(String decisionKey) {
    if (!decisions.containsKey(decisionKey)) {
      throw new NotFoundException("Decision '%s' not found", decisionKey);
    }
    decisions.remove(decisionKey);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;
    if (this == o) return true;
    if (!(o instanceof DecisionSetData other)) return false;

    // Transient (unmanaged) entities have an ID of 'null'. Only managed
    // (persisted and detached) entities have an identity. JPA requires that
    // an entity's identity remains the same over all state changes.
    // Therefore, a transient entity must never equal another entity.
    return id != null && id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
