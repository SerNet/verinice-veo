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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;

import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.TemplateItemAspects;
import org.veo.core.entity.risk.ControlRiskValues;
import org.veo.core.entity.risk.RiskDefinitionRef;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Entity(name = "control")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class ControlData extends ElementData implements Control {

  @ManyToMany(
      targetEntity = ControlData.class,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "control_parts",
      joinColumns = @JoinColumn(name = "composite_id"),
      inverseJoinColumns = @JoinColumn(name = "part_id"))
  @Valid
  @Getter
  private final Set<Control> parts = new HashSet<>();

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = ControlRiskValuesAspectData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  private final Set<ControlRiskValuesAspectData> riskValuesAspects = new HashSet<>();

  @ManyToMany(targetEntity = ControlData.class, mappedBy = "parts", fetch = FetchType.LAZY)
  @Getter
  private final Set<Control> composites = new HashSet<>();

  @Override
  public void setRiskValues(Domain domain, Map<RiskDefinitionRef, ControlRiskValues> riskValues) {
    if (riskValues.isEmpty()) {
      removeAspectByDomain(riskValuesAspects, domain);
      return;
    }
    var aspect =
        findAspectByDomain(riskValuesAspects, domain)
            .orElseGet(
                () -> {
                  var newAspect = new ControlRiskValuesAspectData(domain, this);
                  riskValuesAspects.add(newAspect);
                  return newAspect;
                });
    aspect.setValues(riskValues);
  }

  public Map<RiskDefinitionRef, ControlRiskValues> getRiskValues(Domain domain) {
    return findAspectByDomain(riskValuesAspects, domain)
        .map(ControlRiskValuesAspectData::getValues)
        .orElse(Map.of());
  }

  @Override
  public void transferToDomain(Domain oldDomain, Domain newDomain) {
    findAspectByDomain(riskValuesAspects, oldDomain).ifPresent(a -> a.setDomain(newDomain));
    super.transferToDomain(oldDomain, newDomain);
  }

  @Override
  public boolean removeRiskDefinition(RiskDefinitionRef riskDefinition, Domain domain) {
    return findAspectByDomain(riskValuesAspects, domain)
        .map(a -> a.values.remove(riskDefinition) != null)
        .orElse(false);
  }

  @Override
  protected void applyItemAspects(TemplateItemAspects itemAspects, Domain domain) {
    setRiskValues(
        domain, Optional.ofNullable(itemAspects.controlRiskValues()).orElse(new HashMap<>()));
  }

  @Override
  protected TemplateItemAspects mapAspectsToItem(Domain domain) {
    return new TemplateItemAspects(getRiskValues(domain), null, null);
  }
}
