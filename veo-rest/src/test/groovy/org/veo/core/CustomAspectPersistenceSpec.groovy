/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Urs Zeidler.
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
package org.veo.core

import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE

import javax.transaction.Transactional

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase

import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.CustomAspect
import org.veo.core.entity.Unit
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

@Transactional()
@AutoConfigureTestDatabase(replace = NONE)
class CustomAspectPersistenceSpec extends VeoSpringSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository
    @Autowired
    private AssetRepositoryImpl assetRepository

    def "create an asset with a customAspect and save-load it"() {
        given: "a Unit and an asset"

        CustomAspect cp = newCustomAspect('my.new.linktype')

        Client client = clientRepository.save(newClient())
        Unit unit = newUnit(client)
        Asset asset = newAsset(unit) {
            customAspects = [cp] as Set
        }

        unitRepository.save(unit)
        assetRepository.save(asset)

        when: "loaded from db"
        Optional<Asset> savedAsset = assetRepository.findById(asset.id)

        then : "is all ok"
        savedAsset.present


        when: "add some properties"
        def assetData = savedAsset.get()
        cp = assetData.customAspects.first()
        cp.attributes = [
            "my.key.1": "my test value 1",
            "my.key.2": "my test value 2",
            "my.key.3": 10.0,
        ]

        assetRepository.save(assetData)

        savedAsset = assetRepository.findById(asset.id)
        then:
        savedAsset.present
        when:
        assetData = savedAsset.get()
        then: "The properties are also transformed"

        assetData.getCustomAspects().size() == 1
        when:
        cp = assetData.getCustomAspects().first()
        then:
        cp.getType().equals(cp.getType())

        cp.attributes == [
            "my.key.1": "my test value 1",
            "my.key.2": "my test value 2",
            "my.key.3": 10.0,
        ]
    }
}
