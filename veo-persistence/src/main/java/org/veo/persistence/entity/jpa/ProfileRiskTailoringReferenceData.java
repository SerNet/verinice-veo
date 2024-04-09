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

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.Type;

import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.RiskTailoringReference;
import org.veo.core.entity.RiskTailoringReferenceValues;
import org.veo.core.entity.risk.RiskDefinitionRef;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** owner is risk-affected, target is scenario * */
@Entity(name = "profile_risk_tailoring_reference")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class ProfileRiskTailoringReferenceData extends ProfileTailoringReferenceData
    implements RiskTailoringReference<ProfileItem, Profile> {

  @ManyToOne(targetEntity = ProfileItemData.class)
  private ProfileItem riskOwner;

  @ManyToOne(targetEntity = ProfileItemData.class)
  private ProfileItem mitigation;

  @Column(columnDefinition = "jsonb")
  @Type(JsonType.class)
  private Map<RiskDefinitionRef, RiskTailoringReferenceValues> riskDefinitions;

  public void setRiskDefinitions(
      Map<RiskDefinitionRef, RiskTailoringReferenceValues> riskDefinitions) {
    this.riskDefinitions = new HashMap<>(riskDefinitions);
  }
}
