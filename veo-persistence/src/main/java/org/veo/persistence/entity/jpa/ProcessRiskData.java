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
import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.risk.CategorizedImpactValueProvider;
import org.veo.core.entity.risk.CategorizedRiskValueProvider;
import org.veo.core.entity.risk.ProbabilityValueProvider;
import org.veo.core.entity.risk.RiskDefinitionRef;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Entity(name = "processrisk")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProcessRiskData extends AbstractRiskData<Process, ProcessRisk> implements ProcessRisk {

    // see https://github.com/rzwitserloot/lombok/issues/1134
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
    ProcessRiskData(@NotNull @NonNull Process process, @NotNull Scenario scenario) {
        super(scenario, process);
    }

    private void createRiskAspects() {
        getDomains().forEach(d -> {
            var rdRef = d.getRiskDefinitions();
            // create one aspect for each risk-definition and domain
            rdRef.forEach((key,
                    value) -> riskAspects.add(createRiskAspect(d, RiskDefinitionRef.from(value))));
        });
    }

    private ProcessRiskValueAspectData createRiskAspect(Domain d, RiskDefinitionRef rdp) {
        var riskAspect = new ProcessRiskValueAspectData(d, this, rdp);
        // TODO VEO-1101 get prob from scenario, i.e.:
        // riskAspect.setPotentialProbability(getScenario().getPotentialProbability());

        // TODO VEO-1102 add pot impact from process:
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
               targetEntity = ProcessRiskValueAspectData.class,
               mappedBy = "owner",
               fetch = FetchType.LAZY)
    @Valid
    private Set<ProcessRiskValueAspectData> riskAspects = new HashSet<>();

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

    private Optional<ProcessRiskValueAspectData> findRiskAspectForDefinition(
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
                          .map(ProcessRiskValueAspectData::getRiskDefinition)
                          .collect(toSet());
    }

    @Override
    public void setDomains(@NonNull Set<Domain> newDomains) {
        super.setDomains(newDomains);
        createRiskAspects();
    }

    @Override
    public boolean addToDomains(Domain aDomain) {
        var domains = super.addToDomains(aDomain);
        createRiskAspects();
        return domains;
    }
}
