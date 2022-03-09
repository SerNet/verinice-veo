/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Anton Jacobsson
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
package org.veo.rest.test

class BasicCrudRestTest extends VeoRestTest{

    def "Validate API response"() {

        given: 'a unit name'
        def name = 'Bobbard'
        def unitName = 'CRUD test unit'
        def initialUnitCount = get("/units", 200, UserType.DEFAULT).body.size()

        when: 'creating the unit'
        def postResponse = post("/units", ["name": unitName], 201)

        then: 'resourceId has been created and is of type String and the response code is 201'
        def unitId = postResponse.body.resourceId
        unitId instanceof String
        UUID.fromString(unitId)

        when: 'loading the unit'
        def getResponse = get("/units/$unitId", 200, UserType.DEFAULT)

        then: 'the id and name should match the unit previously created'
        getResponse.body.id == unitId
        getResponse.body.name == unitName

        when: 'loading all units'
        def getResponseAll = get("/units", 200, UserType.DEFAULT)

        then: 'size of JSON-unit-list should be increased by one (+ Demo Unit) and the unitId is found' //From the Postman-test: 'only one unit should be loaded and it should be the one we created earlier'
        with(getResponseAll.body) {
            println(getResponseAll.body)
            def expectedUnitCount = initialUnitCount + (initialUnitCount == 0 ? 2 : 1) //if there are no units Demo unit will also be created
            size() == expectedUnitCount
            def unitFromList = it.find { it.id == unitId }
            unitFromList != null
            unitFromList.name == unitName
        }

        when: 'updating the unit'
        def etag = getResponse.parseETag()
        unitName = "Unit of $name"

        then: 'RESTapi responds with status code 200'
        put("/units/$unitId", ["name": unitName], etag,200, UserType.DEFAULT)

        then: 'updating with the same eTag should no longer work (412)'
        put("/units/$unitId", ["name": unitName], etag, 412, UserType.DEFAULT)

        and: 'given an asset'
        def assetName = 'CRUD test asset'
        def asset = [name: assetName, owner: [displayName: "$unitName", targetUri: "$baseUrl/units/$unitId"]]

        when: 'creating an asset inside the unit'
        postResponse = post("/assets", asset, 201, UserType.DEFAULT)

        then: 'Resource ID is a string'
        def assetId = postResponse.body.resourceId
        assetId instanceof String

        when: 'loading the asset'
        getResponse = get("/assets/$assetId",200, UserType.DEFAULT)

        then: 'returned asset has the correct values'
        getResponse.body.id                 == assetId
        getResponse.body.name               == assetName
        getResponse.body.owner.displayName  == unitName
        getResponse.body.owner.targetUri    == "$baseUrl/units/$unitId"
        def targetUri = "$baseUrl/units/$unitId"
        def assetEtag = getResponse.parseETag()

        and: 'when loading all assets in the unit'
        def getResponseAllAssets = get("/assets?unit=$unitId",200, UserType.DEFAULT)

        then: 'only one asset should be returned'
        getResponseAllAssets.body.totalItemCount    == 1
        getResponseAllAssets.body.items[0].id       == assetId
        getResponseAllAssets.body.items[0].name     == assetName

        when:
        assetName = "Asset of $name"

        and: 'Status Code is correct when updating the asset'
        put("/assets/$assetId", [id: assetId, name: assetName, owner: [displayName: unitName,targetUri: targetUri]], assetEtag,200, UserType.DEFAULT)

        and: 'Server denies updating the asset concurrently'
        put("/assets/$assetId", [id: assetId, name: assetName, owner: [displayName: unitName, targetUri: targetUri]], assetEtag,412, UserType.DEFAULT)

        and: 'Creating a person inside the unit'
        def personName = 'CRUD test person'

        then:
        def postResponsePerson = post("/persons", [name: personName, owner: [displayName: unitName, targetUri: targetUri]],201, UserType.DEFAULT)

        then: 'Resource ID is a string and matches'
        def personId = postResponsePerson.body.resourceId
        personId instanceof String

        and: 'Loading all persons in the unit'
        def getResponseAllPersons = get("/persons?unit=$unitId",200, UserType.DEFAULT)

        then: 'Only one person should be returned and it should match previously created person'
        getResponseAllPersons.body.totalItemCount     == 1
        getResponseAllPersons.body.items[0].name      == personName
        getResponseAllPersons.body.items[0].id        == personId
        getResponseAllPersons.body.items[1]           == null

        and: 'Loading only the previously created person'
        def getResponsePerson = get("/persons/$personId", 200, UserType.DEFAULT)

        then: 'Only one person should be returned and it should match previously created person'
        getResponsePerson.body.id       == personId
        getResponsePerson.body.name     == personName
        getResponsePerson.body[1]       == null
        def personEtag= getResponsePerson.parseETag()

        and: 'Changing the name of the person'
        def newPersonName = 'Person with Mohammed Api'
        def putBody = [id: personId, name: newPersonName, owner: [displayName: unitName, targetUri: targetUri]]
        put("$baseUrl/persons/$personId", putBody, personEtag,200, UserType.DEFAULT)

        and: 'Updating person concurrently'
        put("$baseUrl/persons/$personId",putBody,personEtag,412,UserType.DEFAULT)

        and: 'Creating a control inside the unit'
        def controlName = 'CRUD test control'
        def controlBody = [name: controlName, owner: [displayName: unitName, targetUri: targetUri]]
        def postControlResponse = post("$baseUrl"+"/controls",controlBody,201,UserType.DEFAULT)
        postControlResponse.body.resourceId instanceof String
    }
}
