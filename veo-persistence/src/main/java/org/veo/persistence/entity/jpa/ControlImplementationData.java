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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.Type;

import org.veo.core.entity.Constraints;
import org.veo.core.entity.Control;
import org.veo.core.entity.Person;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.entity.compliance.ReqImplRef;
import org.veo.core.entity.compliance.RequirementImplementation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@ToString(onlyExplicitlyIncluded = true)
@Data
@Entity(name = "control_implementation")
@Table(
    name = "control_implementation",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_control_implementation_id",
          columnNames = {"id"}),
      @UniqueConstraint(
          columnNames = {"owner_db_id", "control_id"},
          name = "UK_owner_control")
    })
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class ControlImplementationData implements ControlImplementation {

  /** A surrogate ID used by the persistence layer. */
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_control_impl")
  @SequenceGenerator(name = "seq_control_impl", allocationSize = 10)
  private Long dbId;

  /** The ID used by the domain logic. */
  @NotNull
  @NonNull
  @NaturalId
  @Column(nullable = false, updatable = false)
  @SuppressFBWarnings("NP") // ID is generated, no need to complain
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = RiskAffectedData.class, optional = false)
  @JoinColumn(name = "owner_db_id", foreignKey = @ForeignKey(name = "FK_owner"))
  RiskAffected<?, ?> owner;

  @NotNull
  @NonNull
  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ControlData.class)
  @JoinColumn(name = "control_id", foreignKey = @ForeignKey(name = "FK_control"))
  Control control;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = PersonData.class)
  @JoinColumn(name = "person_id", foreignKey = @ForeignKey(name = "FK_responsible"))
  Person responsible;

  @Column(length = Constraints.DEFAULT_DESCRIPTION_MAX_LENGTH)
  String description;

  @Column(columnDefinition = "jsonb")
  @Type(JsonType.class)
  Set<ReqImplRef> requirementImplementations = new HashSet<>();

  private ControlImplementationData(Control control) {
    this.control = control;
  }

  private void initializeRequirements(RiskAffectedData<?, ?> elmt, Control control) {
    var controls = control.getPartsRecursively();
    controls.add(control);

    var existingControls =
        elmt.getRequirementImplementations().stream()
            .map(RequirementImplementation::getControl)
            .collect(Collectors.toSet());
    controls.removeAll(existingControls);

    var requirementImplementations =
        controls.stream().map(RequirementImplementationData::createNew).collect(Collectors.toSet());

    requirementImplementations.forEach(elmt::addRequirementImplementation);
    setRequirementImplementations(
        requirementImplementations.stream()
            .map(RequirementImplementation.class::cast)
            .collect(Collectors.toSet()));
  }

  private void setRequirementImplementations(Set<RequirementImplementation> implementations) {
    this.requirementImplementations.clear();
    this.requirementImplementations.addAll(
        implementations.stream().map(ReqImplRef::from).collect(Collectors.toSet()));
  }

  public static ControlImplementationData createNew(RiskAffectedData<?, ?> elmt, Control control) {
    var newCI = new ControlImplementationData(control);
    newCI.id = UUID.randomUUID();

    newCI.initializeRequirements(elmt, control);
    return newCI;
  }

  public void remove() {
    owner = null;
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public void disassociateFromOwner() {
    owner.disassociateControl(control);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ControlImplementationData other = (ControlImplementationData) o;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public void remove(RequirementImplementation ri) {
    requirementImplementations.remove(ReqImplRef.from(ri));
  }

  @Override
  public void addRequirement(Control control) {
    // create new RI for the control:
    var reqImpl = RequirementImplementationData.createNew(control);
    // associated it with the owner of this CI - or recover existing RI:
    reqImpl = ((RiskAffectedData<?, ?>) owner).addRequirementImplementation(reqImpl);
    // store the reference to it:
    requirementImplementations.add(ReqImplRef.from(reqImpl));
  }
}
