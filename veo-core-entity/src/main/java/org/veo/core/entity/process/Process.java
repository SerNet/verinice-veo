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
 ******************************************************************************/
package org.veo.core.entity.process;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.asset.Asset;

/**
 * A business process.
 *
 * @author akoderman
 *
 */
public class Process extends EntityLayerSupertype {

    @NotBlank
    private String name;

    // TODO this relation needs to be replaced by an relational-aspect
    @NotNull(message = "The array of assets must not be null.")
    @Size(min = 0, max = 1000000,
            message = "No more than one million assets may be directly referenced by a process.")
    private Set<Asset> assets;

    private Process(Key id, Unit unit, String name, Lifecycle state, Instant validFrom,
            Instant validUntil, long version) {
        super(id, unit, state, validFrom, validUntil, version);
        this.name = name;
        this.assets = new HashSet<>();
    }

    private Process(Key<UUID> id, Unit unit, String name, Lifecycle state, Instant validFrom,
            Instant validUntil, long version, Set<Asset> assets) {
        super(id, unit, state, validFrom, validUntil, version);
        this.name = name;
        this.assets = assets;
    }

    /**
     * Factory method to create a new process object.
     * 
     * @param unit
     * @param name
     * @return
     */
    public static Process newProcess(Unit unit, String name) {
        return new Process(Key.newUuid(), unit, name, Lifecycle.CREATING, Instant.now(), null, 0);
    }

    public static Process existingProcess(Key<UUID> id, Unit unit, String name, Lifecycle state,
            Instant validFrom, long version) {
        return new Process(id, unit, name, state, validFrom, validFrom, version);
    }
    
    public static Process existingProcessWithAssets(Key<UUID> id, Unit unit, String name, Lifecycle state,
            Instant validFrom, long version, Set<Asset> assets) {
        return new Process(id, unit, name, state, validFrom, validFrom, version, assets);
    }

    public void addAsset(Asset asset) {
        checkSameClient(asset);
        this.assets.add(asset);
    }

    public void addAssets(Set<Asset> assets) {
        checkSameClients(assets);
        this.assets.addAll(assets);
    }

    public void removeAsset(Asset asset) {
        this.assets.remove(asset);
    }

    public Set<Asset> getAssets() {
        return Collections.unmodifiableSet(this.assets);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
