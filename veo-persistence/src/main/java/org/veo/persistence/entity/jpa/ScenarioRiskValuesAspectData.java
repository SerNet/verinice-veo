/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Daniel Murygin
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

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.vladmihalcea.hibernate.type.json.JsonType;

import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.risk.PotentialProbabilityImpl;
import org.veo.core.entity.risk.RiskDefinitionRef;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Holds references to potential probabilities of a scenario defined in a risk definition. */
@Entity(name = "scenario_risk_values_aspect")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@TypeDef(name = "json", typeClass = JsonType.class, defaultForType = Map.class)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class ScenarioRiskValuesAspectData extends AspectData {

  public ScenarioRiskValuesAspectData(DomainTemplate domain, Scenario owner) {
    super(domain, owner);
  }

  @Getter
  @Setter
  @NotNull
  @Column(columnDefinition = "jsonb", name = "probability")
  @Type(type = "json")
  Map<RiskDefinitionRef, PotentialProbabilityImpl> potentialProbability;
}
