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

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.RiskTailoringReference;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** owner is risk-affected, target is scenario * */
@Entity(name = "profile_risk_tailoring_reference")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class ProfileRiskTailoringReferenceData extends ProfileTailoringReferenceData
    implements RiskTailoringReference<ProfileItem> {

  @ManyToOne(targetEntity = ProfileItemData.class)
  private ProfileItem riskOwner;

  @ManyToOne(targetEntity = ProfileItemData.class)
  private ProfileItem mitigation;
}
