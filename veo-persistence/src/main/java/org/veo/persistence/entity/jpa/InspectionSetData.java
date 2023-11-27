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

import org.hibernate.annotations.Type;

import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.inspection.Inspection;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
@Entity(name = "inspection_set")
public class InspectionSetData {
  @Id
  @GeneratedValue(strategy = SEQUENCE, generator = "seq_inspection_sets")
  @SequenceGenerator(name = "seq_inspection_sets")
  private Long id;

  @NotNull
  @Type(JsonType.class)
  private Map<String, Inspection> inspections = new HashMap<>();

  public void setInspections(Map<String, Inspection> inspections) {
    this.inspections.clear();
    this.inspections.putAll(inspections);
  }

  /**
   * @return {@code true} if new inspection was added, {@code false} if existing inspection was
   *     updated
   */
  public boolean apply(String key, Inspection value) {
    var created = !inspections.containsKey(key);
    inspections.put(key, value);
    return created;
  }

  public void remove(String inspectionKey) {
    if (!inspections.containsKey(inspectionKey)) {
      throw new NotFoundException("Inspection '%s' not found", inspectionKey);
    }
    inspections.remove(inspectionKey);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;
    if (this == o) return true;
    if (!(o instanceof InspectionSetData other)) return false;

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
