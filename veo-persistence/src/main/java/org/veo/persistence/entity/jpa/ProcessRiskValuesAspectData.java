/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.vladmihalcea.hibernate.type.json.JsonType;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Key;
import org.veo.core.entity.aspects.RiskValuesAspect;
import org.veo.core.entity.exception.ReferenceTargetNotFoundException;
import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.DeterminedRisk;
import org.veo.core.entity.risk.DeterminedRiskImpl;
import org.veo.core.entity.risk.Impact;
import org.veo.core.entity.risk.ImpactImpl;
import org.veo.core.entity.risk.ProbabilityImpl;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.riskdefinition.RiskDefinition;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "riskvalues_aspect")
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Setter
@TypeDef(name = "json", typeClass = JsonType.class)
public class ProcessRiskValuesAspectData implements RiskValuesAspect {

  ProcessRiskValuesAspectData(
      Domain domain, ProcessRiskData owner, RiskDefinitionRef riskDefinition) {
    this.domain = domain;
    this.owner = owner;
    this.riskDefinition = riskDefinition;

    this.probability = new ProbabilityImpl();

    var domainRiskDefinition =
        domain
            .getRiskDefinition(riskDefinition.getIdRef())
            .orElseThrow(
                () ->
                    new ReferenceTargetNotFoundException(
                        format(
                            "Risk definition ID %s not found in domain ID %s.",
                            riskDefinition.getIdRef(), domain.getId())));

    var categoryRefs =
        domainRiskDefinition.getCategories().stream().map(CategoryRef::from).collect(toSet());

    this.impactCategories = categoryRefs.stream().map(ImpactImpl::new).toList();

    this.riskCategories = categoryRefs.stream().map(DeterminedRiskImpl::new).toList();
  }

  @Id
  @ToString.Include
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private String dbId;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = DomainTemplateData.class, optional = false)
  @JoinColumn(name = "domain_id")
  private DomainTemplate domain;

  @Setter(AccessLevel.NONE)
  @Column(length = RiskDefinition.MAX_ID_SIZE)
  private RiskDefinitionRef riskDefinition;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ProcessRiskData.class, optional = false)
  @Setter(AccessLevel.NONE)
  private ProcessRiskData owner;

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;

    if (this == o) return true;

    if (!(o instanceof ProcessRiskValuesAspectData)) return false;

    ProcessRiskValuesAspectData other = (ProcessRiskValuesAspectData) o;
    // Transient (unmanaged) entities have an ID of 'null'. Only managed
    // (persisted and detached) entities have an identity. JPA requires that
    // an entity's identity remains the same over all state changes.
    // Therefore a transient entity must never equal another entity.
    String dbId = getDbId();
    return dbId != null && dbId.equals(other.getDbId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Column(columnDefinition = "jsonb")
  @Type(type = "json")
  private ProbabilityImpl probability;

  @Column(columnDefinition = "jsonb")
  @Type(type = "json")
  private List<ImpactImpl> impactCategories = new ArrayList<>();

  @Column(columnDefinition = "jsonb")
  @Type(type = "json")
  private List<DeterminedRiskImpl> riskCategories = new ArrayList<>();

  @Override
  public Key<UUID> getDomainId() {
    return domain.getId();
  }

  @Override
  public Key<String> getRiskDefinitionId() {
    return new Key<>(riskDefinition.getIdRef());
  }

  public List<Impact> getImpactCategories() {
    return impactCategories.stream().map(Impact.class::cast).toList();
  }

  @Override
  public List<DeterminedRisk> getCategorizedRisks() {
    return riskCategories.stream().map(DeterminedRisk.class::cast).toList();
  }
}
