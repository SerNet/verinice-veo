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
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

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
    @Autowired
    private EntityFactory entityFactory
    @Autowired
    private UnitRepositoryImpl unitRepository

    def "create an asset with a customLink and save-load it"() {
        given: "a person and an asset"

        Key unitId = Key.newUuid()
        Unit unit = entityFactory.createUnit(unitId, "unit", null)
        Person person = entityFactory.createPerson(Key.newUuid(), "P1", unit)
        Asset asset = entityFactory.createAsset(Key.newUuid(), "AssetName", unit)

        CustomLink cp = entityFactory.createCustomLink(Key.newUuid(), "My link", person, asset)

        cp.setType('my.new.linktype')
        cp.setApplicableTo(['Asset'] as Set)

        asset.setLinks(cp as Set)

        Client client = entityFactory.createClient(Key.newUuid(), "Demo client")
        unit.setClient(client)

        clientRepository.save(client)
        unitRepository.save(unit)
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
