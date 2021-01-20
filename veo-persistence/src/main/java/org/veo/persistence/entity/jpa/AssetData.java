/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.persistence.entity.jpa;

import static java.util.Collections.singleton;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import org.veo.core.entity.Asset;
import org.veo.core.entity.AssetRisk;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.Person;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.exception.ModelConsistencyException;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Entity(name = "asset")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class AssetData extends EntityLayerSupertypeData implements Asset {

    @ManyToMany(targetEntity = AssetData.class,
                cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "asset_parts",
               joinColumns = @JoinColumn(name = "composite_id"),
               inverseJoinColumns = @JoinColumn(name = "part_id"))
    @Getter
    private final Set<Asset> parts = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL,
               orphanRemoval = true,
               targetEntity = AssetRiskData.class,
               mappedBy = "asset",
               fetch = FetchType.LAZY)
    private final Set<AssetRisk> risks = new HashSet<>();

    @Override
    public void setRisks(Set<AssetRisk> newRisks) {
        risks.clear();
        risks.addAll(newRisks);
    }

    @Override
    public Set<AssetRisk> getRisks() {
        return risks;
    }

    boolean addRisk(AssetRisk risk) {
        checkRisk(risk);
        return risks.add(risk);
    }

    private void checkRisk(AssetRisk risk) {
        if (!risk.getAsset()
                 .equals(this))
            throw new IllegalArgumentException();
    }

    @Override
    public boolean removeRisk(AssetRisk risk) {
        return risks.remove(risk);
    }

    @Override
    public void removeRisks(Set<AssetRisk> newRisks) {
        risks.removeAll(newRisks);
    }

    @Override
    public AssetRisk newRisk(Scenario scenario, Domain domain) {
        scenario.checkSameClient(getOwner().getClient());
        isDomainValid(domain);

        var assetRiskData = new AssetRiskData(this, scenario, domain);
        addRisk(assetRiskData);
        return assetRiskData;
    }

    @Override
    public Set<AssetRisk> newRisks(Set<Scenario> scenarios, Domain domain) {
        return this.newRisks(scenarios, singleton(domain));
    }

    @Override
    public Set<AssetRisk> newRisks(Set<Scenario> scenarios, Set<Domain> domains) {
        scenarios.forEach(s -> s.checkSameClient(getOwner().getClient()));
        domains.forEach(this::isDomainValid);

        return scenarios.stream()
                        .map(s -> newRisk(s, domains))
                        .map(AssetRisk.class::cast)
                        .collect(Collectors.toSet());
    }

    @Override
    public AssetRisk newRisk(Scenario scenario, Set<Domain> domains) {
        if (domains.isEmpty())
            throw new IllegalArgumentException("Need at least one domain to create a risk.");

        var risk = newRisk(scenario, domains.stream()
                                            .findFirst()
                                            .orElseThrow());
        domains.forEach(risk::addToDomains);

        return risk;
    }

    @Override
    public Optional<AssetRisk> getRisk(Scenario scenario) {
        return risks.stream()
                    .filter(risk -> risk.getScenario()
                                        .equals(scenario))
                    .findFirst();
    }

    @Override
    public Optional<AssetRisk> getRisk(Key<UUID> scenarioRef) {
        return risks.stream()
                    .filter(risk -> risk.getScenario()
                                        .getId()
                                        .equals(scenarioRef))
                    .findFirst();
    }

    @Override
    public AssetRisk updateRisk(AssetRisk existingRisk, Set<Domain> domains,
            @Nullable Control mitigation, @Nullable Person riskOwner) {

        var assetRisk = getRisk(existingRisk.getScenario()).orElseThrow(() -> new IllegalArgumentException(
                String.format("The risk is not know to this asset: %s", existingRisk)));

        assetRisk.setDomains(domains);
        return assetRisk.mitigate(mitigation)
                        .appoint(riskOwner);
    }

    private void isDomainValid(Domain domain) {
        if (!getDomains().contains(domain))
            throw new ModelConsistencyException(
                    "The provided domain '%s' is not yet known to this asset. ",
                    domain.getDisplayName());
    }
}
