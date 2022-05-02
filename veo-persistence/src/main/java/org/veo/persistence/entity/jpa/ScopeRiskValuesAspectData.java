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

import javax.persistence.Column;
import javax.persistence.Entity;

import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Scope;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.riskdefinition.RiskDefinition;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Holds risk related info for a scope in a specific domain. */
@Entity(name = "scope_risk_values_aspect")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ScopeRiskValuesAspectData extends AspectData {

  public ScopeRiskValuesAspectData(DomainTemplate domain, Scope owner) {
    super(domain, owner);
  }

  @Getter
  @Setter
  @Column(name = "risk_definition", length = RiskDefinition.MAX_ID_SIZE)
  RiskDefinitionRef riskDefinitionRef;
}
