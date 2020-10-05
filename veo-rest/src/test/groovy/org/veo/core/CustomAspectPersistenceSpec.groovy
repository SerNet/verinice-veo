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
package org.veo.core

import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE

import java.time.OffsetDateTime

import javax.transaction.Transactional

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles

import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.CustomProperties
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory

import spock.lang.Specification

@SpringBootTest(classes = CustomAspectPersistenceSpec.class
)
@Transactional()
@ComponentScan("org.veo")
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
class CustomAspectPersistenceSpec extends Specification {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private UnitRepositoryImpl unitRepository
    @Autowired
    private AssetRepositoryImpl assetRepository
    @Autowired
    private EntityDataFactory entityFactory

    def "create an asset with a customAspect and save-load it"() {
        given: "a Unit and an asset"

        Key unitId = Key.newUuid()

        CustomProperties cp = entityFactory.createCustomProperties()
        cp.setType('my.new.linktype')
        cp.setApplicableTo(['Asset'] as Set)

        Unit unit = entityFactory.createUnit(unitId, "unit", null)
        Client client = entityFactory.createClient(Key.newUuid(), "Demo Client")
        unit.setClient(client)
        Asset asset = entityFactory.createAsset(Key.newUuid(), "AssetName", unit)
        asset.setCustomAspects([cp] as Set)

        clientRepository.save(client)
        unitRepository.save(unit)
        assetRepository.save(asset)

        when: "loaded from db"
        Optional<Asset> savedAsset = assetRepository.findById(asset.id)

        then : "is all ok"
        savedAsset.present


        when: "add some properties"

        cp.setProperty("my.key.1", "my test value 1")
        cp.setProperty("my.key.2", "my test value 2")


        assetRepository.save(asset)

        savedAsset = assetRepository.findById(asset.id)
        then:
        savedAsset.present
        when:
        Asset assetData = savedAsset.get()
        then: "The properties are also transformed"

        assetData.getCustomAspects().size() == 1
        assetData.getCustomAspects().first().getType().equals(cp.getType())

        assetData.getCustomAspects().first().stringProperties.size() == 2
        assetData.getCustomAspects().first().stringProperties["my.key.1"] == "my test value 1"
        assetData.getCustomAspects().first().stringProperties["my.key.2"] == "my test value 2"

        when: "add properties of type number"

        cp.setProperty("my.key.3",(Integer) 10)

        assetRepository.save(asset)

        savedAsset = assetRepository.findById(asset.id)
        then:
        savedAsset.present
        when:
        CustomProperties savedCp = savedAsset.get().getCustomAspects().first()

        then: "numbers also"
        savedCp.doubleProperties.size() == 1
        savedCp.doubleProperties["my.key.3"] == 10

        when: "add properties of type date"

        cp.setProperty("my.key.4", OffsetDateTime.parse("2020-02-02T00:00:00Z"))

        assetRepository.save(asset)

        savedAsset = assetRepository.findById(asset.id)
        then:
        savedAsset.present
        when:
        savedCp = savedAsset.get().getCustomAspects().first()

        then: "date also"
        savedCp.getOffsetDateTimeProperties().size() == 1
        savedCp.getOffsetDateTimeProperties().get("my.key.4") == OffsetDateTime.parse("2020-02-02T00:00:00Z")

        when: "add properties of type list string"

        CustomProperties aspect = entityFactory.createCustomProperties()
        aspect.setType('my_new_asset_custom_aspect')
        aspect.setApplicableTo(['Asset'] as Set)
        aspect.setProperty('l1', ['e1', 'e2'])

        asset.getCustomAspects().clear()
        asset.getCustomAspects().add(aspect)

        assetRepository.save(asset)

        savedAsset = assetRepository.findById(asset.id)
        then:
        savedAsset.present
        when:
        CustomProperties savedAspect = savedAsset.get().getCustomAspects().first()

        then: "list also"

        savedAspect.applicableTo.size() == 1
        savedAspect.applicableTo.contains('Asset')
        savedAspect.type == 'my_new_asset_custom_aspect'
        with(savedAspect.stringListProperties) {
            size() == 1
            get("l1") == ["e1", "e2"]
        }
    }
}
