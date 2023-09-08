/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Alexander Koderman
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

import static java.util.Objects.isNull;
import static org.veo.core.entity.Constraints.DEFAULT_DESCRIPTION_MAX_LENGTH;
import static org.veo.core.entity.compliance.ImplementationStatus.UNKNOWN;
import static org.veo.core.entity.compliance.Origination.SYSTEM_SPECIFIC;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.NaturalId;

import org.veo.core.entity.Control;
import org.veo.core.entity.Person;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.compliance.ImplementationStatus;
import org.veo.core.entity.compliance.Origination;
import org.veo.core.entity.compliance.RequirementImplementation;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString(onlyExplicitlyIncluded = true)
@Data
@Entity(name = "requirement_implementation")
@Table(
    name = "requirement_implementation",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_requirement_implementation_id",
          columnNames = {"id"}),
      @UniqueConstraint(
          columnNames = {"origin_db_id", "control_id"},
          name = "UK_origin_control")
    })
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class RequirementImplementationData implements RequirementImplementation {

  /** A surrogate ID used by the persistence layer. */
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_req_impl")
  @SequenceGenerator(name = "seq_req_impl", allocationSize = 10)
  private Long dbId;

  /** The ID used by the domain logic. */
  @NotNull
  @NonNull
  @NaturalId
  @Column(nullable = false, updatable = false)
  private UUID id;

  /** The asset/process/scope that shows its implementation via this implementedRequirement. */
  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ElementData.class, optional = false)
  @JoinColumn(name = "origin_db_id", foreignKey = @ForeignKey(name = "FK_origin"))
  @NotNull
  @NonNull
  RiskAffected<?, ?> origin;

  /** The control whose requirements are being implemented by this. */
  @NotNull
  @NonNull
  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ControlData.class)
  @JoinColumn(name = "control_id", foreignKey = @ForeignKey(name = "FK_control"))
  Control control;

  /**
   * Can be system-speceific or - if the owner is an underlying system - inherited by the actual
   * system that is required top show its compliance with a control.
   */
  @Enumerated(EnumType.STRING)
  Origination origination = SYSTEM_SPECIFIC;

  /** A responsible person usually the system owner. */
  @ManyToOne(fetch = FetchType.LAZY, targetEntity = PersonData.class)
  @JoinColumn(name = "person_id", foreignKey = @ForeignKey(name = "FK_responsible"))
  Person responsible;

  /**
   * Used by some standards - could otherwise be one of multiple different {@code setParameters}
   * (see below).
   */
  @Enumerated(EnumType.STRING)
  ImplementationStatus status = UNKNOWN;

  /** A description of this control implementation. */
  @Column(length = DEFAULT_DESCRIPTION_MAX_LENGTH)
  String implementationStatement;

  public static RequirementImplementationData createNew(Control control) {
    var ri = new RequirementImplementationData();
    ri.id = UUID.randomUUID();
    ri.control = control;
    return ri;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RequirementImplementationData other = (RequirementImplementationData) o;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public boolean isUnedited() {
    return isNull(implementationStatement)
        && origination == SYSTEM_SPECIFIC
        && responsible == null
        && status == UNKNOWN;
  }
}
