/*
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade
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
 */
package org.veo.persistence.entity.jpa;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Person;
import org.veo.core.entity.TranslatedText;
import org.veo.core.entity.Unit;
import org.veo.core.entity.inspection.Severity;

import lombok.Data;
import lombok.ToString;

@Entity
@Data
public class InspectionFindingData {

  @Id
  @ToString.Include
  @GeneratedValue(generator = "UUID")
  @UuidGenerator
  @Column(name = "db_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ElementData.class, optional = false)
  @JoinColumn(name = "element_id")
  private Element element;

  @ManyToOne(
      fetch = FetchType.LAZY,
      targetEntity = DomainData.class,
      optional = false,
      cascade = CascadeType.PERSIST)
  @JoinColumn(name = "domain_id")
  private Domain domain;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = UnitData.class, optional = false)
  @JoinColumn(name = "unit_id")
  private Unit unit;

  private String inspectionId;

  @Enumerated
  @JdbcType(PostgreSQLEnumJdbcType.class)
  private @NotNull Severity severity;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = PersonData.class)
  @JoinColumn(name = "responsible_person_id")
  private Person responsible;

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private TranslatedText taskSummary;

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;
    if (this == o) return true;
    if (!(o instanceof InspectionFindingData other)) return false;

    // Transient (unmanaged) entities have an ID of 'null'. Only managed
    // (persisted and detached) entities have an identity. JPA requires that
    // an entity's identity remains the same over all state changes.
    // Therefore, a transient entity must never equal another entity.
    return id != null && id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
