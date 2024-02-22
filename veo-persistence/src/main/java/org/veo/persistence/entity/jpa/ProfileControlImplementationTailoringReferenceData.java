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

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import org.veo.core.entity.ControlImplementationTailoringReference;
import org.veo.core.entity.ProfileItem;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** owner is risk-affected, target is control * */
@Entity(name = "profile_control_implementation_tailoring_reference")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class ProfileControlImplementationTailoringReferenceData
    extends ProfileTailoringReferenceData
    implements ControlImplementationTailoringReference<ProfileItem> {

  @ManyToOne(targetEntity = ProfileItemData.class)
  private ProfileItem responsible;

  private String description;
}
