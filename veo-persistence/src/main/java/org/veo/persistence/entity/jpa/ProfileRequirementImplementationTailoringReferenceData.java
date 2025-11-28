/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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

import static org.veo.core.entity.Constraints.DEFAULT_DESCRIPTION_MAX_LENGTH;
import static org.veo.core.entity.compliance.ImplementationStatus.UNKNOWN;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.RequirementImplementationTailoringReference;
import org.veo.core.entity.compliance.ImplementationStatus;

import lombok.Data;

/** owner is risk-affected, target is control * */
@Entity(name = "profile_requirement_implementation_tailoring_reference")
@Data
public class ProfileRequirementImplementationTailoringReferenceData
    extends ProfileTailoringReferenceData
    implements RequirementImplementationTailoringReference<ProfileItem, Profile> {

  @ManyToOne(targetEntity = ProfileItemData.class)
  private ProfileItem responsible;

  @Enumerated(EnumType.STRING)
  @NotNull
  ImplementationStatus status = UNKNOWN;

  @Column(length = DEFAULT_DESCRIPTION_MAX_LENGTH)
  String implementationStatement;

  private LocalDate implementationUntil;

  private Integer cost;

  private LocalDate implementationDate;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ProfileItemData.class)
  private ProfileItem implementedBy;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ProfileItemData.class)
  private ProfileItem document;

  private LocalDate lastRevisionDate;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ProfileItemData.class)
  private ProfileItem lastRevisionBy;

  private LocalDate nextRevisionDate;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ProfileItemData.class)
  private ProfileItem nextRevisionBy;

  private LocalDate assessmentDate;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ProfileItemData.class)
  private ProfileItem assessmentBy;

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
