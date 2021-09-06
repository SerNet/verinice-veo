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

import javax.transaction.Transactional

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.CustomAspect
import org.veo.core.entity.CustomLink
import org.veo.core.entity.Person
import org.veo.core.entity.Unit
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.PersonRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl

@SpringBootTest(classes = CustomLinkPersistenceSpec.class
)
@Transactional()
@ActiveProfiles("test")
class CustomLinkPersistenceSpec extends VeoSpringSpec {


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

        Client client = clientRepository.save(newClient())
        Unit unit = newUnit(client)
        Person person = newPerson(unit)
        Asset asset = newAsset(unit)

        CustomLink cp = newCustomLink(person, 'my.new.linktype')

        asset.setLinks([cp] as Set)

        unitRepository.save(unit)
        personRepository.save(person)
        assetRepository.save(asset)

        when: "loaded from db"

        Optional<Asset> savedAsset = assetRepository.findById(asset.id)

        then : "is all ok"
        savedAsset.present

        when: "add some properties"
        Asset assetData = savedAsset.get()
        assetData.links.first().attributes = [
            "my.key.1": "my test value 1",
            "my.key.2": "my test value 2",
        ]

        assetRepository.save(savedAsset.get())

        assetData = assetRepository.findById(asset.id).get()
        then: "The properties are also transformed"

        assetData.getLinks().size() == 1
        with(assetData.getLinks().first()) {
            getType() == cp.getType()
            attributes["my.key.1"] == "my test value 1"
            attributes["my.key.2"] == "my test value 2"
        }

        when: "add properties of type number"

        cp.attributes = [
            "my.key.1": 10.0
        ]

        assetRepository.save(assetData)
        savedAsset = assetRepository.findById(asset.id)
        then:
        savedAsset.present
        when:
        assetData = savedAsset.get()
        CustomAspect savedCp = assetData.getLinks().first()

        then: "numbers also"
        savedCp.attributes["my.key.1"] == 10
    }
}
