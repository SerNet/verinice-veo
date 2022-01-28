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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.vladmihalcea.hibernate.type.json.JsonType;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.aspects.Aspect;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.risk.CategorizedImpactValueProvider;
import org.veo.core.entity.risk.CategorizedRiskValueProvider;
import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.DeterminedRisk;
import org.veo.core.entity.risk.DeterminedRiskImpl;
import org.veo.core.entity.risk.Impact;
import org.veo.core.entity.risk.ImpactImpl;
import org.veo.core.entity.risk.ImpactRef;
import org.veo.core.entity.risk.ProbabilityImpl;
import org.veo.core.entity.risk.ProbabilityRef;
import org.veo.core.entity.risk.ProbabilityValueProvider;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.risk.RiskRef;
import org.veo.core.entity.risk.RiskTreatmentOption;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "riskvalue_aspect")
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Setter
@TypeDef(name = "json", typeClass = JsonType.class)
public class ProcessRiskValueAspectData implements Aspect, ProbabilityValueProvider,
        CategorizedImpactValueProvider, CategorizedRiskValueProvider {

    ProcessRiskValueAspectData(Domain domain, ProcessRiskData owner,
            RiskDefinitionRef riskDefinition) {
        this.domain = domain;
        this.owner = owner;
        this.riskDefinition = riskDefinition;

        this.probability = new ProbabilityImpl();

        var domainRiskDefinition = domain.getRiskDefinition(riskDefinition.getIdRef())
                                         .orElseThrow(() -> new NotFoundException("Risk "
                                                 + "definition ID %s not found in domain ID %s.",
                                                 riskDefinition.getIdRef(), domain.getId()));

        var categories = domainRiskDefinition.getCategories();
        var categoryRefs = categories.stream()
                                     .map(CategoryRef::from)
                                     .collect(toSet());

        this.impactCategories = categoryRefs.stream()
                                            .map(ImpactImpl::new)
                                            .collect(toList());

        this.riskCategories = categoryRefs.stream()
                                          .map(DeterminedRiskImpl::new)
                                          .collect(toList());
    }

    @Id
    @ToString.Include
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String dbId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = DomainTemplateData.class, optional = false)
    @JoinColumn(name = "domain_id")
    private DomainTemplate domain;

    @Setter(AccessLevel.NONE)
    private RiskDefinitionRef riskDefinition;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ProcessRiskData.class, optional = false)
    private ProcessRiskData owner;

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (this == o)
            return true;

        if (!(o instanceof ProcessRiskValueAspectData))
            return false;

        ProcessRiskValueAspectData other = (ProcessRiskValueAspectData) o;
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

    void setRiskCategories(List<DeterminedRiskImpl> riskCategories) {
        // TODO check if valid acc. to risk definition
        this.riskCategories.clear();
        this.riskCategories.addAll(riskCategories);
    }

    void setImpactCategories(List<ImpactImpl> impactCategories) {
        // TODO check if valid acc. to risk definition
        this.impactCategories.clear();
        this.impactCategories.addAll(impactCategories);
    }

    public List<Impact> getCategorizedImpacts() {
        return impactCategories.stream()
                               .map(Impact.class::cast)
                               .collect(toList());
    }

    @Override
    public List<CategoryRef> getAvailableCategories() {
        return impactCategories.stream()
                               .map(ImpactImpl::getCategory)
                               .collect(toUnmodifiableList());
    }

    public List<DeterminedRisk> getCategorizedRisks() {
        return riskCategories.stream()
                             .map(DeterminedRisk.class::cast)
                             .collect(toList());
    }

    @Override
    public ProbabilityRef getPotentialProbability() {
        return probability.getPotentialProbability();
    }

    @Override
    public ProbabilityRef getSpecificProbability() {
        return probability.getSpecificProbability();
    }

    @Override
    public ProbabilityRef getEffectiveProbability() {
        return probability.getEffectiveProbability();
    }

    @Override
    public String getSpecificProbabilityExplanation() {
        return probability.getSpecificProbabilityExplanation();
    }

    @Override
    public void setPotentialProbability(ProbabilityRef potential) {
        // TODO check if valid acc. to risk definition
        probability.setPotentialProbability(potential);
    }

    @Override
    public void setSpecificProbability(ProbabilityRef specific) {
        // TODO check if valid acc. to risk definition
        probability.setSpecificProbability(specific);
    }

    @Override
    public void setSpecificProbabilityExplanation(String explanation) {
        // TODO check if valid acc. to risk definition
        probability.setSpecificProbabilityExplanation(explanation);
    }

    @Override
    public void setRiskTreatmentExplanation(CategoryRef impactCategory,
            String riskTreatmentExplanation) {
        riskCategoryById(impactCategory).setRiskTreatmentExplanation(riskTreatmentExplanation);
    }

    void setInherentRisk(CategoryRef impactCategory, RiskRef inherentRisk) {
        // TODO check if valid acc. to risk definition
        riskCategoryById(impactCategory).setInherentRisk(inherentRisk);
    }

    public void setSpecificImpact(CategoryRef impactCategory, ImpactRef specific) {
        // TODO check if valid acc. to risk definition
        impactCategoryById(impactCategory).setSpecificImpact(specific);
    }

    @Override
    public ImpactRef getPotentialImpact(CategoryRef impactCategory) {
        return impactCategoryById(impactCategory).getPotentialImpact();
    }

    @Override
    public ImpactRef getSpecificImpact(CategoryRef impactCategory) {
        return impactCategoryById(impactCategory).getSpecificImpact();
    }

    @Override
    public ImpactRef getEffectiveImpact(CategoryRef impactCategory) {
        return impactCategoryById(impactCategory).getEffectiveImpact();
    }

    @Override
    public String getSpecificImpactExplanation(CategoryRef impactCategory) {
        return impactCategoryById(impactCategory).getSpecificImpactExplanation();
    }

    @Override
    public RiskRef getInherentRisk(CategoryRef impactCategory) {
        return riskCategoryById(impactCategory).getInherentRisk();
    }

    @Override
    public RiskRef getResidualRisk(CategoryRef impactCategory) {
        return riskCategoryById(impactCategory).getResidualRisk();
    }

    @Override
    public void setResidualRiskExplanation(CategoryRef impactCategory,
            String residualRiskExplanation) {
        riskCategoryById(impactCategory).setResidualRiskExplanation(residualRiskExplanation);
    }

    @Override
    public String getResidualRiskExplanation(CategoryRef impactCategory) {
        return riskCategoryById(impactCategory).getResidualRiskExplanation();
    }

    @Override
    public Set<RiskTreatmentOption> getRiskTreatments(CategoryRef impactCategory) {
        return riskCategoryById(impactCategory).getRiskTreatments();
    }

    @Override
    public void setPotentialImpact(CategoryRef impactCategory, ImpactRef potential) {
        // TODO VEO-1104 check if valid acc. to risk definition
        impactCategoryById(impactCategory).setPotentialImpact(potential);
    }

    @Override
    public String getRiskTreatmentExplanation(CategoryRef impactCategory) {
        return riskCategoryById(impactCategory).getRiskTreatmentExplanation();
    }

    @Override
    public void setSpecificImpactExplanation(CategoryRef impactCategory, String explanation) {
        impactCategoryById(impactCategory).setSpecificImpactExplanation(explanation);
    }

    public void setResidualRisk(CategoryRef impactCategory, RiskRef residualRisk) {
        // TODO VEO-1104 check if valid acc. to risk definition
        riskCategoryById(impactCategory).setResidualRisk(residualRisk);
    }

    @Override
    public void setRiskTreatments(CategoryRef impactCategory,
            Set<RiskTreatmentOption> riskTreatments) {
        // TODO VEO-1104 check if valid acc. to risk definition
        riskCategoryById(impactCategory).setRiskTreatments(riskTreatments);
    }

    private ImpactImpl impactCategoryById(CategoryRef categoryId) {
        return impactCategories.stream()
                               .filter(c -> c.getCategory()
                                             .equals(categoryId))
                               .findFirst()
                               .orElseThrow(() -> new NotFoundException(
                                       "Impact category %s does not exist for for risk aspect "
                                               + "with ID %s",
                                       categoryId, getDbId()));
    }

    private DeterminedRiskImpl riskCategoryById(CategoryRef categoryId) {
        return riskCategories.stream()
                             .filter(c -> c.getCategory()
                                           .equals(categoryId))
                             .findFirst()
                             .orElseThrow(() -> new NotFoundException(
                                     "Risk category %s does not exist for risk aspect with ID %s",
                                     categoryId, getDbId()));
    }
}
