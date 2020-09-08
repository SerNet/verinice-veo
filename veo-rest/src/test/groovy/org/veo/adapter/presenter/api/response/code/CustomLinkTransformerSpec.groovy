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
package org.veo.adapter.presenter.api.response.code

import java.time.OffsetDateTime

import org.veo.adapter.presenter.api.dto.full.FullAssetDto
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoContext
import org.veo.core.entity.Asset
import org.veo.core.entity.CustomLink
import org.veo.core.entity.CustomProperties
import org.veo.core.entity.Key
import org.veo.core.entity.Person
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory

import spock.lang.Specification

class CustomLinkTransformerSpec extends Specification {

    def "create an asset with a customLink and transform it"() {
        given: "a person and an asset"

        def id1= Key.newUuid()
        def id2 =Key.newUuid()
        def id3 =Key.newUuid()

        EntityFactory entityFactory = new EntityDataFactory()
        Person person = entityFactory.createPerson(id1,"P1", null)
        Asset asset = entityFactory.createAsset(id2,"AssetName",null)

        CustomLink cp = entityFactory.createCustomLink()
        cp.source = person
        cp.target = asset
        cp.name = 'linkName'
        cp.type = 'my.new.linktype'
        cp.applicableTo = (['Asset'] as Set)
        asset.links = [cp as Set]


        when: "add some properties"
        cp.setProperty("my.key.1","my test value 1")

        cp.setProperty("my.key.2","my test value 2")


        DtoToEntityContext tcontext = new DtoToEntityContext(entityFactory)
        tcontext.addEntity(person)

        Asset assetData = FullAssetDto.from(asset, EntityToDtoContext.getCompleteTransformationContext())
                .toEntity(tcontext)

        then: "The properties are also transformed"
        assetData.getLinks().size() == 1
        assetData.getLinks().first().getType().equals(cp.getType())

        assetData.getLinks().first().stringProperties.size() == 2
        assetData.getLinks().first().stringProperties["my.key.1"] == "my test value 1"
        assetData.getLinks().first().stringProperties["my.key.2"] == "my test value 2"

        when: "add properties of type number"
        cp.setProperty("my.key.3", 10)


        tcontext = new DtoToEntityContext(entityFactory)
        tcontext.addEntity(person)

        Asset savedAsset = FullAssetDto.from(asset, EntityToDtoContext.getCompleteTransformationContext()).toEntity(tcontext)
        CustomProperties savedCp = savedAsset.getLinks().first()

        then: "numbers also"
        savedCp.integerProperties.size() == 1
        savedCp.integerProperties["my.key.3"] == 10


        when: "add properties of type date"
        cp.setProperty("my.key.4", OffsetDateTime.parse("2020-02-02T00:00:00.000Z"))


        tcontext = new DtoToEntityContext(entityFactory)
        tcontext.addEntity(person)

        savedAsset = FullAssetDto.from(asset, EntityToDtoContext.getCompleteTransformationContext()).toEntity(tcontext)
        savedCp = savedAsset.getLinks().first()

        then: "date also"
        savedCp.getOffsetDateTimeProperties().size() == 1
        savedCp.getOffsetDateTimeProperties().get("my.key.4") == OffsetDateTime.parse("2020-02-02T00:00:00.000Z")
    }
}
