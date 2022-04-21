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

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.validation.Valid;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.risk.ProcessImpactValues;
import org.veo.core.entity.risk.RiskDefinitionRef;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Entity(name = "process")
@NamedEntityGraph(name = ProcessData.FULL_AGGREGATE_GRAPH_WITH_RISKS,
                  attributeNodes = { @NamedAttributeNode(value = "customAspects"),
                          @NamedAttributeNode(value = "domains"),
                          @NamedAttributeNode(value = "appliedCatalogItems"),
                          @NamedAttributeNode(value = "links"),
                          @NamedAttributeNode(value = "subTypeAspects"),
                          @NamedAttributeNode(value = "risks", subgraph = "risk.entities"), },
                  subgraphs = {
                          @NamedSubgraph(name = "risk.entities",
                                         attributeNodes = { @NamedAttributeNode(value = "scenario"),
                                                 @NamedAttributeNode(value = "mitigation"),
                                                 @NamedAttributeNode(value = "riskOwner"), }) },
                  subclassSubgraphs = {
                          @NamedSubgraph(name = "risk.entities",
                                         type = ProcessRiskData.class,
                                         attributeNodes = { @NamedAttributeNode(value = "scenario"),
                                                 @NamedAttributeNode(value = "mitigation"),
                                                 @NamedAttributeNode(value = "riskOwner"),
                                                 @NamedAttributeNode(value = "riskAspects"), }) })
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
public class ProcessData extends RiskAffectedData<Process, ProcessRisk> implements Process {

    public static final String FULL_AGGREGATE_GRAPH_WITH_RISKS = "fullAggregateGraphWithRisks";
    @ManyToMany(targetEntity = ProcessData.class,
                cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "process_parts",
               joinColumns = @JoinColumn(name = "composite_id"),
               inverseJoinColumns = @JoinColumn(name = "part_id"))
    @Valid
    @Getter
    private final Set<Process> parts = new HashSet<>();

    @ManyToMany(targetEntity = ProcessData.class, mappedBy = "parts", fetch = FetchType.LAZY)
    @Getter
    private final Set<Process> composites = new HashSet<>();

    @Override
    ProcessRisk createRisk(Scenario scenario) {
        return new ProcessRiskData(this, scenario);
    }

    @OneToMany(cascade = CascadeType.ALL,
               orphanRemoval = true,
               targetEntity = ProcessImpactValuesAspectData.class,
               mappedBy = "owner",
               fetch = FetchType.LAZY)
    @Valid
    private final Set<ProcessImpactValuesAspectData> riskValuesAspects = new HashSet<>();

    @Override
    public void setImpactValues(DomainTemplate domain,
            Map<RiskDefinitionRef, ProcessImpactValues> riskValues) {
        var aspect = findAspectByDomain(riskValuesAspects, domain).orElseGet(() -> {
            var newAspect = new ProcessImpactValuesAspectData(domain, this);
            riskValuesAspects.add(newAspect);
            return newAspect;
        });
        aspect.setValues(riskValues);
    }

    public Optional<Map<RiskDefinitionRef, ProcessImpactValues>> getImpactValues(
            DomainTemplate domain) {
        return findAspectByDomain(riskValuesAspects,
                                  domain).map(ProcessImpactValuesAspectData::getValues);
    }

    @Override
    public Optional<ProcessImpactValues> getImpactValues(DomainTemplate domain,
            RiskDefinitionRef riskDefinition) {
        return getImpactValues(domain).map(impactValuesByRiskDefinition -> impactValuesByRiskDefinition.get(riskDefinition));
    }

    @Override
    public void transferToDomain(Domain oldDomain, Domain newDomain) {
        findAspectByDomain(riskValuesAspects, oldDomain).ifPresent(a -> a.setDomain(newDomain));
        getRisks().forEach(r -> r.transferToDomain(oldDomain, newDomain));
        super.transferToDomain(oldDomain, newDomain);
    }
}
