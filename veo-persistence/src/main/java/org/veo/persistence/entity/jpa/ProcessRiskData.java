/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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

import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.risk.CategorizedImpactValueProvider;
import org.veo.core.entity.risk.CategorizedRiskValueProvider;
import org.veo.core.entity.risk.DeterminedRisk;
import org.veo.core.entity.risk.DomainRiskReferenceProvider;
import org.veo.core.entity.risk.DomainRiskReferenceValidator;
import org.veo.core.entity.risk.Impact;
import org.veo.core.entity.risk.ProbabilityImpl;
import org.veo.core.entity.risk.ProbabilityValueProvider;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.risk.RiskValues;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Entity(name = "processrisk")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ProcessRiskData extends AbstractRiskData<Process, ProcessRisk> implements ProcessRisk {

    // see https://github.com/rzwitserloot/lombok/issues/1134
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    ProcessRiskData(@NotNull @NonNull Process process, @NotNull Scenario scenario) {
        super(scenario, process);
    }

    private void createRiskAspects() {
        log.debug("Creating risk aspects");
        getDomains().forEach(d -> {
            log.debug("Creating risk aspect for domain {} with risk definitions: {}",
                      d.getDisplayName(), String.join(", ", d.getRiskDefinitions()
                                                             .keySet()));
            // create one aspect for each risk-definition and domain
            d.getRiskDefinitions()
             .forEach((key, value) -> {
                 var riskDefinitionRef = RiskDefinitionRef.from(value);
                 var existingRiskDefinition = riskAspects.stream()
                                                         .filter(ra -> ra.getDomain()
                                                                         .equals(d))
                                                         .filter(ra -> ra.getRiskDefinition()
                                                                         .equals(riskDefinitionRef))
                                                         .findFirst();
                 // only create new risk definition if it doesn't exist yet:
                 if (existingRiskDefinition.isEmpty()) {
                     riskAspects.add(createRiskAspect(d, riskDefinitionRef));
                 }
             });
        });
    }

    private ProcessRiskValuesAspectData createRiskAspect(Domain d,
            RiskDefinitionRef riskDefinition) {
        var riskAspect = new ProcessRiskValuesAspectData(d, this, riskDefinition);
        getScenario().getPotentialProbability(d)
                     .ifPresent(probabilitiesByRiskDefinition -> riskAspect.setPotentialProbability(probabilitiesByRiskDefinition.get(riskDefinition)
                                                                                                                                 .getPotentialProbability()));
        // TODO VEO-1102 get potential impact from process:
        // use impactProvider or riskDefinition...
        // getImpactProvider(rdp).getAvailableCategories().forEach(c -> {
        // riskAspect.setPotentialImpact(c, getEntity().getPotentialImpact(c));
        // });

        return riskAspect;
    }

    /**
     * There may be different risk aspects for different risk-definitions. NOTE:
     * this risk object is scoped to a list of domains, so the risk aspects in this
     * {@code riskAspects} set must all belong to one of these domains.
     */
    @Column(name = "risk_aspects")
    @OneToMany(cascade = CascadeType.ALL,
               orphanRemoval = true,
               targetEntity = ProcessRiskValuesAspectData.class,
               mappedBy = "owner",
               fetch = FetchType.LAZY)
    @Valid
    private Set<ProcessRiskValuesAspectData> riskAspects = new HashSet<>();

    @Override
    public ProbabilityValueProvider getProbabilityProvider(RiskDefinitionRef riskDefinition) {
        return findRiskAspectForDefinition(riskDefinition).orElseThrow(() -> new NotFoundException(
                "No probability values found for process" + " %s and scenario %s with "
                        + "risk-definition" + " %s",
                getEntity().getDisplayName(), getScenario().getDisplayName(),
                riskDefinition.getIdRef()));
    }

    @Override
    public CategorizedImpactValueProvider getImpactProvider(RiskDefinitionRef riskDefinition) {
        return findRiskAspectForDefinition(riskDefinition).orElseThrow(() -> new NotFoundException(
                "No risk impacts found for process %s " + "and scenario %s with "
                        + "risk-definition %s",
                getEntity().getDisplayName(), getScenario().getDisplayName(),
                riskDefinition.getIdRef()));
    }

    private Optional<ProcessRiskValuesAspectData> findRiskAspectForDefinition(
            RiskDefinitionRef riskDefinition) {
        return riskAspects.stream()
                          .filter(a -> a.getRiskDefinition()
                                        .equals(riskDefinition))
                          .findFirst();
    }

    @Override
    public CategorizedRiskValueProvider getRiskProvider(RiskDefinitionRef riskDefinition) {
        return findRiskAspectForDefinition(riskDefinition).orElseThrow(() -> new NotFoundException(
                "No risk values found for process %s " + "and scenario %s with "
                        + "risk-definition %s",
                getEntity().getDisplayName(), getScenario().getDisplayName(),
                riskDefinition.getIdRef()));
    }

    @Override
    public Set<RiskDefinitionRef> getRiskDefinitions() {
        return riskAspects.stream()
                          .map(ProcessRiskValuesAspectData::getRiskDefinition)
                          .collect(toSet());
    }

    @Override
    public Set<RiskDefinitionRef> getRiskDefinitions(Domain domain) {
        return riskAspects.stream()
                          .filter(ra -> ra.getDomain()
                                          .equals(domain))
                          .map(ProcessRiskValuesAspectData::getRiskDefinition)
                          .collect(toSet());
    }

    @Override
    public void updateRiskValues(Set<RiskValues> newValuesSet) {
        riskAspects.forEach(ra -> {
            var domain = ra.getDomain();
            var rd = ra.getRiskDefinition();

            getRiskValuesForRiskDefinition(newValuesSet, domain, rd).ifPresent(newValues -> {
                var validator = new DomainRiskReferenceValidator(
                        DomainRiskReferenceProvider.referencesForDomain(domain), rd);

                var probability = ra.getProbability();
                updateProbability(probability, newValues, validator);

                var impacts = ra.getImpactCategories();
                updateImpacts(impacts, newValues, validator);

                var risks = ra.getCategorizedRisks();
                updateRisks(risks, newValues, validator);
            });
        });
        // FIXME VEO-1105 check if risk-definitions are all valid for scope
    }

    private void updateRisks(List<DeterminedRisk> risks, RiskValues newRiskValues,
            DomainRiskReferenceValidator validator) {
        risks.forEach(risk -> {
            var category = risk.getCategory();
            // handle partial inputs correctly - if not all categories are present in the
            // new value set, leave the existing categories as they are:
            if (newRiskValues.getCategorizedRisks() != null
                    && newRiskValues.categoryExists(category)) {
                risk.setResidualRisk(validator.validate(category,
                                                        newRiskValues.getResidualRisk(category)));
                risk.setResidualRiskExplanation(newRiskValues.getResidualRiskExplanation(category));
                risk.setRiskTreatments(newRiskValues.getRiskTreatments(category));
                risk.setRiskTreatmentExplanation(newRiskValues.getRiskTreatmentExplanation(category));
            }
        });
    }

    private void updateProbability(ProbabilityImpl probability, RiskValues newAspect,
            DomainRiskReferenceValidator validator) {
        // only change existing probability values if they are present in the input:
        if (newAspect.getProbability() == null)
            return;
        probability.setSpecificProbability(validator.validate(newAspect.getSpecificProbability()));
        probability.setSpecificProbabilityExplanation(newAspect.getSpecificProbabilityExplanation());
    }

    private void updateImpacts(List<Impact> impacts, RiskValues newImpactValues,
            DomainRiskReferenceValidator validator) {
        impacts.forEach(impact -> {
            var category = impact.getCategory();
            // handle partial inputs correctly - if not all categories are present in the
            // new value set, leave the existing categories as they are:
            if (newImpactValues.getImpactCategories() != null
                    && newImpactValues.categoryExists(category)) {
                impact.setSpecificImpact(validator.validate(category,
                                                            newImpactValues.getSpecificImpact(category)));
                impact.setSpecificImpactExplanation(newImpactValues.getSpecificImpactExplanation(category));
            }
        });
    }

    private Optional<RiskValues> getRiskValuesForRiskDefinition(Set<RiskValues> newValues,
            DomainTemplate domain, RiskDefinitionRef riskDefinition) {
        return newValues.stream()
                        .filter(nv -> nv.getDomainId()
                                        .equals(domain.getId()))
                        .filter(nv -> nv.getRiskDefinitionId()
                                        .value()
                                        .equals(riskDefinition.getIdRef()))
                        .findFirst();
    }

    @Override
    public boolean addToDomains(Domain aDomain) {
        var added = super.addToDomains(aDomain);
        if (added)
            createRiskAspects();
        return added;
    }

    @Override
    public boolean removeFromDomains(Domain aDomain) {
        var success = super.removeFromDomains(aDomain);
        removeRiskAspects(aDomain);
        return success;
    }

    private void removeRiskAspects(Domain aDomain) {
        riskAspects.removeIf(ra -> ra.getDomain()
                                     .equals(aDomain));
    }
}
