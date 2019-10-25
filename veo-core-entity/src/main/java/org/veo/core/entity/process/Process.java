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

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Key;
import org.veo.core.entity.asset.Asset;

/**
 * A business process.
 *
 * @author akoderman
 *
 */
public class Process extends EntityLayerSupertype {

    private String name;
    private Set<Asset> assets;

    public Process(Key id, String name) {
        super(id, EntityLayerSupertype.Lifecycle.CREATING, new Date(), null);
        this.name = name;
        this.assets = new HashSet<>();
    }

    public void addAsset(Asset asset) {
        this.assets.add(asset);
    }

    public void addAssets(Set<Asset> assets) {
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
