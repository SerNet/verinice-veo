/*******************************************************************************
 * Copyright (c) 2020 Urs Zeidler.
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
package org.veo.persistence.entity.jpa.code

import org.veo.core.entity.Asset
import org.veo.core.entity.CustomProperties
import org.veo.core.entity.Key
import org.veo.core.entity.custom.SimpleProperties
import org.veo.core.entity.impl.AssetImpl
import org.veo.persistence.entity.jpa.AssetData
import org.veo.persistence.entity.jpa.custom.PropertyData
import spock.lang.Specification

class CustomPropertiesTransformerSpec extends Specification {

    def "Transform entities with custom aspects"() {

        given: "An entity ex. asset"

        CustomProperties cp = new SimpleProperties()
        cp.setType("my.new.type")
        cp.setApplicableTo(['Asset'] as Set)
        cp.setId(Key.newUuid())
        Asset asset = new AssetImpl(Key.newUuid(), "AssetName", null)
        asset.setCustomAspects([cp] as Set)

        when:"The entity is transformed"
        AssetData assetData = AssetData.from(asset)

        then: "The customproperties are also transformed"

        assetData.getCustomAspects().size() == 1
        assetData.getCustomAspects().first().getId() == cp.getId().uuidValue()
        assetData.getCustomAspects().first().getType() == cp.getType()

        when: "Some properties are present."

        cp.setProperty("my.key.1", "my test value 1")
        cp.setProperty("my.key.2", "my test value 2")

        assetData = AssetData.from(asset)

        then: "The properties are also transformed"

        assetData.getCustomAspects().size() == 1
        assetData.getCustomAspects().first().getId() == cp.getId().uuidValue()
        assetData.getCustomAspects().first().getType() == cp.getType()

        with(assetData.getCustomAspects().first().getDataProperties()) {
            size() == 2
            with(it.find{it.key == "my.key.1"}) {
                type == PropertyData.Type.STRING
                stringValue == "my test value 1"
            }
            with(it.find{it.key == "my.key.2"}) {
                type == PropertyData.Type.STRING
                stringValue == "my test value 2"
            }
        }
    }
}
