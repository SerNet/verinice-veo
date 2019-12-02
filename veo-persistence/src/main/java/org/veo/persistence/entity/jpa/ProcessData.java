/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
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
 *
 * Contributors:
 *     Alexander Koderman <ak@sernet.de> - initial API and implementation
 ******************************************************************************/
package org.veo.persistence.entity.jpa;

import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.Valid;

import org.veo.core.entity.asset.Asset;
import org.veo.core.entity.process.Process;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@ToString

@Entity(name = "process")
@Table(name = "processes")
public class ProcessData extends EntityLayerSupertypeData {

    @Column(name="name")
    private String name;

    /*
     * Not modelled from asset's side. TODO will be replaced by Aspect
     * relationship. This would be the mapping if this were a bidirectional
     * association:
     * 
     * @ManyToMany
     * 
     * @JoinTable(name = "process_asset", joinColumns =
     * { @JoinColumn(name="processId", referencedColumnName="uuid") },
     * inverseJoinColumns = {@JoinColumn(name="assetId",
     * referencedColumnName="uuid")})
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(
        cascade = { CascadeType.PERSIST, CascadeType.MERGE }, 
        fetch = FetchType.EAGER,
        orphanRemoval = true 
    )
    private Set<AssetData> assets;

    public static ProcessData from(@Valid Process process) {
        // map fields
        return new ProcessData();
    }

    public Process toProcess() {
        return Process.existingProcessWithAssets(
                uuid.toKey(), unit.toUnit(), name, state, validFrom, version, toAssetSet(assets)
        );
    }

    private Set<Asset> toAssetSet(Set<AssetData> assets) {
        return assets.stream()
                .map(AssetData::toAsset)
                .collect(Collectors.toSet());
    }
}
