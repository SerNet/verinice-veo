/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.TailoringReference;
import org.veo.core.entity.TailoringReferenceType;

import lombok.Data;

@Entity(name = "profile_tailoring_reference")
@Data
public class ProfileTailoringReferenceData extends ProfileReferenceData
    implements TailoringReference<ProfileItem, Profile> {
  @Column(name = "reference_type", columnDefinition = "int4")
  private TailoringReferenceType referenceType;

  @Override
  public void remove() {
    getOwner().getTailoringReferences().remove(this);
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
