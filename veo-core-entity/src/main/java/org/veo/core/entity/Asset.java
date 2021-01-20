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
package org.veo.core.entity;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import lombok.NonNull;

/**
 * An asset describes a part in the unit. It could be a physical element like a
 * computer, but also something not physical like a software for example.
 */
public interface Asset extends EntityLayerSupertype, CompositeEntity<Asset> {

    @Override
    default String getModelType() {
        return EntityTypeNames.ASSET;
    }

    @Override
    default Class<? extends ModelObject> getModelInterface() {
        return Asset.class;
    }

    void setRisks(Set<AssetRisk> risks);

    Set<AssetRisk> getRisks();

    boolean removeRisk(AssetRisk risk);

    void removeRisks(Set<AssetRisk> risks);

    /**
     * Creates a new risk for this asset.
     *
     * @param scenario
     *            The scenario affecting this asset. May be a scenario composite.
     * @param domain
     *            The domain applicable to this risk. Must be one of the domains
     *            known to the asset.
     */
    AssetRisk newRisk(Scenario scenario, Domain domain);

    Set<AssetRisk> newRisks(Set<Scenario> scenarios, Domain domain);

    AssetRisk newRisk(Scenario scenario, Set<Domain> domains);

    Set<AssetRisk> newRisks(Set<Scenario> scenarios, Set<Domain> domains);

    /**
     * Retrieves the risk caused to this asset by a given scenario.
     *
     * @param scenario
     *            The scenario that is affecting this asset.
     * @return The existing risk object or Optional.empty() if none is present.
     */
    Optional<AssetRisk> getRisk(Scenario scenario);

    /**
     * Retrieves the risk caused to this asset by a given scenario.
     *
     * @param scenarioRef
     *            The key reference of a scenario that is affecting this asset.
     * @return The existing risk object or Optional.empty() if none is present.
     */
    Optional<AssetRisk> getRisk(Key<UUID> scenarioRef);

    /**
     * Updates an existing risk with new values. Increases the version number of the
     * risk.
     *
     * @param existingRisk
     *            the existing risk value that will be updated with new values
     * @param domains
     *            the new domain list
     * @param mitigation
     *            the new control to mitigate this risk
     * @param riskOwner
     *            the new person to appoint the risk to
     * @return the updated risk entity
     */
    AssetRisk updateRisk(@NotNull @NonNull AssetRisk existingRisk,
            @NotNull @NonNull Set<Domain> domains, @Nullable Control mitigation,
            @Nullable Person riskOwner);
}
