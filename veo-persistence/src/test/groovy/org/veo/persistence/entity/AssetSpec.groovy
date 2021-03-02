/*******************************************************************************
 * Copyright (c) 2021 Alexander Koderman.
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
package org.veo.persistence.entity

import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.test.VeoSpec

class AssetSpec extends VeoSpec {
    EntityFactory entityFactory

    Client client

    Unit unit

    def setup() {
        entityFactory = new EntityDataFactory()
        this.client = entityFactory.createClient(Key.newUuid(), "client")
        this.unit = entityFactory.createUnit("unit", null)
        this.unit.setClient(client)
    }

    def "Two freshly created assets are not equal"() {
        given: "two assets"
        Asset asset1 = newAsset(unit)
        Asset asset2 = newAsset(unit)

        expect: "the assets are not equal"
        asset1 != asset2
    }
}
