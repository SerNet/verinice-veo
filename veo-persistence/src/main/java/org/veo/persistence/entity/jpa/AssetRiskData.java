/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
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

import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.veo.core.entity.Asset;
import org.veo.core.entity.AssetRisk;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.Person;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.exception.ModelConsistencyException;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "assetrisk")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AssetRiskData extends AbstractRiskData implements AssetRisk {

    AssetRiskData(@NotNull @NonNull Asset asset, @NotNull Scenario scenario, Domain domain) {
        super(scenario);
        this.asset = asset;
        setDbId(Key.newUuid()
                   .uuidValue());
        setVersion(0);
        addToDomains(domain);
    }

    @Override
    public final boolean addToDomains(Domain aDomain) {
        checkDomain(aDomain);
        return super.addToDomains(aDomain);
    }

    private void checkDomain(Domain aDomain) {
        if (!asset.getDomains()
                  .contains(aDomain)) {
            throw new ModelConsistencyException(
                    "The provided domain '%s' is not yet known to the" + " asset.",
                    aDomain.getDisplayName());
        }
    }

    @NotNull
    @NonNull
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = AssetData.class, optional = false)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PRIVATE)
    private Asset asset;

    @Override
    public boolean remove() {
        return this.asset.removeRisk(this);
    }

    @Override
    public AssetRiskData mitigate(@Nullable Control control) {
        return (AssetRiskData) super.mitigate(control);
    }

    @Override
    public AssetRiskData appoint(@Nullable Person riskOwner) {
        return (AssetRiskData) super.appoint(riskOwner);
    }

}
