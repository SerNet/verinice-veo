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

import java.time.OffsetDateTime

import javax.transaction.Transactional

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan

import org.veo.core.entity.*
import org.veo.core.entity.custom.LinkImpl
import org.veo.core.entity.impl.AssetImpl
import org.veo.core.entity.impl.ClientImpl
import org.veo.core.entity.impl.PersonImpl
import org.veo.core.entity.impl.UnitImpl
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import spock.lang.Specification

@SpringBootTest(classes = CustomLinkPersistenceSpec.class
)
@Transactional()
@ComponentScan("org.veo")
class CustomLinkPersistenceSpec extends Specification {


    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private AssetRepositoryImpl assetRepository
    @Autowired
    private PersonRepositoryImpl personRepository

    def "create an asset with a customLink and save-load it"() {
        given: "a person and an asset"

        Key unitId = Key.newUuid()

        Unit unit = new UnitImpl(unitId, "unit", null)

        Person person = new PersonImpl(Key.newUuid(), "P1", unit)

        Asset asset = new AssetImpl(Key.newUuid(), "AssetName", unit)

        CustomLink cp = new LinkImpl(Key.newUuid(), null, person, asset)

        cp.setType('my.new.linktype')
        cp.setApplicableTo(['Asset'] as Set)

        asset.addToLinks(cp)

        Client client = new ClientImpl(Key.newUuid(), "Demo client")
        client.setUnits([unit] as Set)
        unit.setClient(client)

        clientRepository.save(client)
        personRepository.save(person)
        assetRepository.save(asset)

        when: "loaded from db"

        Optional<Asset> savedAsset = assetRepository.findById(asset.id)

        then : "is all ok"
        savedAsset.present

        when: "add some properties"

        cp.setProperty("my.key.1", "my test value 1")
        cp.setProperty("my.key.2", "my test value 2")

        assetRepository.save(asset)

        Asset assetData = assetRepository.findById(asset.id).get()
        then: "The properties are also transformed"

        assetData.getLinks().size() == 1
        with(assetData.getLinks().first()) {
            getId() == cp.getId()
            getType() == cp.getType()

            stringProperties["my.key.1"] == "my test value 1"
            stringProperties["my.key.2"] == "my test value 2"
        }

        when: "add properties of type number"

        cp.setProperty("my.key.3", 10)

        assetRepository.save(asset)
        savedAsset = assetRepository.findById(asset.id)
        then:
        savedAsset.present
        when:
        CustomProperties savedCp = savedAsset.get().getLinks().first()

        then: "numbers also"
        savedCp.integerProperties["my.key.3"] == 10

        when: "add properties of type date"

        cp.setProperty("my.key.4", OffsetDateTime.parse("2020-02-02T00:00:00Z"))

        assetRepository.save(asset)
        savedAsset = assetRepository.findById(asset.id)
        then:
        savedAsset.present
        when:
        savedCp = savedAsset.get().getLinks().first()

        then: "date also"
        savedCp.getOffsetDateTimeProperties().get("my.key.4") == OffsetDateTime.parse("2020-02-02T00:00:00Z")
    }
}
