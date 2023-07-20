/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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

import static java.time.Instant.now;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.riskdefinition.RiskDefinition;

import lombok.Data;
import lombok.ToString;

@Entity(name = "domain")
@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class DomainData extends DomainBaseData implements Domain {
  @Column(name = "active")
  @ToString.Include
  private boolean active = true;

  @ManyToOne(targetEntity = DomainTemplateData.class, fetch = FetchType.LAZY)
  @Valid
  private DomainTemplate domainTemplate;

  // This enforces the composition association Client-Domain
  @ManyToOne(targetEntity = ClientData.class, optional = false, fetch = FetchType.LAZY)
  @Valid
  private Client owner;

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public boolean applyRiskDefinition(String riskDefinitionRef, RiskDefinition riskDefinition) {
    var isNewRiskDef = riskDefinitionSet.apply(riskDefinitionRef, riskDefinition);
    setUpdatedAt(now());
    return isNewRiskDef;
  }

  @Override
  public void removeRiskDefinition(RiskDefinitionRef riskDefinition) {
    riskDefinitionSet.remove(riskDefinition.getIdRef());
    setUpdatedAt(now());
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
