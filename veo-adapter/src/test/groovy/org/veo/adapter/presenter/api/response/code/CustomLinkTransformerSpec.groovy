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

import org.veo.adapter.presenter.api.response.*
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoContext
import org.veo.core.entity.Asset
import org.veo.core.entity.CustomLink
import org.veo.core.entity.CustomProperties
import org.veo.core.entity.Key
import org.veo.core.entity.Person
import org.veo.core.entity.custom.LinkImpl
import org.veo.core.entity.impl.AssetImpl
import org.veo.core.entity.impl.PersonImpl
import spock.lang.Specification

class CustomLinkTransformerSpec extends Specification {


    def "create an asset with a customLink and transform it"() {
        given: "a person and an asset"

        Person person = new PersonImpl(Key.newUuid(), "P1", null)

        Asset asset = new AssetImpl(Key.newUuid(), "AssetName", null)

        CustomLink cp = new LinkImpl(Key.newUuid(), null, person, asset)
        cp.setType('my.new.linktype')
        cp.setApplicableTo(['Asset'] as Set)
        asset.addToLinks(cp)


        when: "add some properties"
        cp.setProperty("my.key.1","my test value 1")

        cp.setProperty("my.key.2","my test value 2")


        DtoToEntityContext tcontext = DtoToEntityContext.getCompleteTransformationContext()
        tcontext.addEntity(person)

        Asset assetData = AssetDto.from(asset, EntityToDtoContext.getCompleteTransformationContext()).toAsset(tcontext)

        then: "The properties are also transformed"
        assetData.getLinks().size() == 1
        assetData.getLinks().first().getId().equals(cp.getId())
        assetData.getLinks().first().getType().equals(cp.getType())

        assetData.getLinks().first().stringProperties.size() == 2
        assetData.getLinks().first().stringProperties["my.key.1"] == "my test value 1"
        assetData.getLinks().first().stringProperties["my.key.2"] == "my test value 2"

        when: "add properties of type number"
        cp.setProperty("my.key.3", 10)


        tcontext = DtoToEntityContext.getCompleteTransformationContext()
        tcontext.addEntity(person)

        Asset savedAsset = AssetDto.from(asset, EntityToDtoContext.getCompleteTransformationContext()).toAsset(tcontext)
        CustomProperties savedCp = savedAsset.getLinks().first()

        then: "numbers also"
        savedCp.integerProperties.size() == 1
        savedCp.integerProperties["my.key.3"] == 10


        when: "add properties of type date"
        cp.setProperty("my.key.4", OffsetDateTime.parse("2020-02-02T00:00:00.000Z"))


        tcontext = DtoToEntityContext.getCompleteTransformationContext()
        tcontext.addEntity(person)

        savedAsset = AssetDto.from(asset, EntityToDtoContext.getCompleteTransformationContext()).toAsset(tcontext)
        savedCp = savedAsset.getLinks().first()

        then: "date also"
        savedCp.getOffsetDateTimeProperties().size() == 1
        savedCp.getOffsetDateTimeProperties().get("my.key.4") == OffsetDateTime.parse("2020-02-02T00:00:00.000Z")
    }
}
