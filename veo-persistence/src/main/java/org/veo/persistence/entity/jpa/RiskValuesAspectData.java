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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import org.veo.core.entity.Domain;
import org.veo.core.entity.RiskTailoringReferenceValues;
import org.veo.core.entity.aspects.RiskValuesAspect;
import org.veo.core.entity.exception.ReferenceTargetNotFoundException;
import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.DeterminedRisk;
import org.veo.core.entity.risk.DeterminedRiskImpl;
import org.veo.core.entity.risk.Impact;
import org.veo.core.entity.risk.ImpactImpl;
import org.veo.core.entity.risk.ProbabilityImpl;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.riskdefinition.CategoryDefinition;
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
public class RiskValuesAspectData implements RiskValuesAspect {

  RiskValuesAspectData(
      Domain domain, AbstractRiskData<?, ?> owner, RiskDefinitionRef riskDefinition) {
    this.domain = domain;
    this.owner = owner;
    this.riskDefinition = riskDefinition;

    this.probability = new ProbabilityImpl();

    var domainRiskDefinition =
        domain
            .findRiskDefinition(riskDefinition.getIdRef())
            .orElseThrow(
                () ->
                    new ReferenceTargetNotFoundException(
                        format(
                            "Risk definition ID %s not found in domain ID %s.",
                            riskDefinition.getIdRef(), domain.getId())));

    var categoryRefs =
        domainRiskDefinition.getCategories().stream()
            .filter(CategoryDefinition::isRiskValuesSupported)
            .map(CategoryRef::from)
            .collect(toSet());

    this.impactCategories = categoryRefs.stream().map(ImpactImpl::new).toList();

    this.riskCategories = categoryRefs.stream().map(DeterminedRiskImpl::new).toList();
  }

  @Id
  @ToString.Include
  @GeneratedValue(generator = "UUID")
  @UuidGenerator
  @Column(name = "db_id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = DomainData.class, optional = false)
  @JoinColumn(name = "domain_id")
  private Domain domain;

  @Setter(AccessLevel.NONE)
  @Column(length = RiskDefinition.MAX_ID_SIZE)
  private RiskDefinitionRef riskDefinition;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = AbstractRiskData.class, optional = false)
  @Setter(AccessLevel.NONE)
  private AbstractRiskData<?, ?> owner;

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;

    if (this == o) return true;

    if (!(o instanceof RiskValuesAspectData)) return false;

    RiskValuesAspectData other = (RiskValuesAspectData) o;
    // Transient (unmanaged) entities have an ID of 'null'. Only managed
    // (persisted and detached) entities have an identity. JPA requires that
    // an entity's identity remains the same over all state changes.
    // Therefore a transient entity must never equal another entity.
    UUID dbId = getId();
    return dbId != null && dbId.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private ProbabilityImpl probability;

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private List<ImpactImpl> impactCategories = new ArrayList<>();

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private List<DeterminedRiskImpl> riskCategories = new ArrayList<>();

  @Override
  public UUID getDomainId() {
    return domain.getId();
  }

  @Override
  public String getRiskDefinitionId() {
    return riskDefinition.getIdRef();
  }

  @Override
  public List<Impact> getImpactCategories() {
    return impactCategories.stream().map(Impact.class::cast).toList();
  }

  @Override
  public List<DeterminedRisk> getCategorizedRisks() {
    return riskCategories.stream().map(DeterminedRisk.class::cast).toList();
  }

  public void setValues(RiskTailoringReferenceValues values) {
    setSpecificProbability(values.specificProbability());
    setSpecificProbabilityExplanation(values.specificProbabilityExplanation());
    values
        .categories()
        .forEach(
            (category, categoryValues) -> {
              setSpecificImpact(category, categoryValues.specificImpact());
              setSpecificImpactExplanation(category, categoryValues.specificImpactExplanation());
              setUserDefinedResidualRisk(category, categoryValues.userDefinedResidualRisk());
              setResidualRiskExplanation(category, categoryValues.residualRiskExplanation());
              setRiskTreatments(category, categoryValues.riskTreatments());
              setRiskTreatmentExplanation(category, categoryValues.riskTreatmentExplanation());
            });
  }

  public boolean removeCategory(CategoryRef category) {
    boolean impactValuesRemoved = impactCategories.removeIf(c -> c.getCategory().equals(category));
    boolean riskValuesRemoved = riskCategories.removeIf(c -> c.getCategory().equals(category));
    return impactValuesRemoved || riskValuesRemoved;
  }

  public void addCategory(CategoryRef category) {
    impactCategories.add(new ImpactImpl(category));
    riskCategories.add(new DeterminedRiskImpl(category));
  }

  public void copyAspectData(RiskValuesAspectData oldAspect) {
    domain
        .findRiskDefinition(oldAspect.riskDefinition.getIdRef())
        .ifPresent(
            rd -> {
              List<CategoryRef> newCats =
                  rd.getCategories().stream().map(CategoryRef::from).toList();
              probability = oldAspect.probability;
              impactCategories =
                  oldAspect.impactCategories.stream() // TODO: verince-veo#3274 do not filter
                      .filter(ic -> newCats.contains(ic.getCategory()))
                      .toList();
              riskDefinition = oldAspect.riskDefinition;
              riskCategories =
                  oldAspect.riskCategories.stream()
                      .filter(dr -> newCats.contains(dr.getCategory()))
                      .toList();
            });
  }
}
