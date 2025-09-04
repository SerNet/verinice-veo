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
/**
 * This class contains tests of the basic operations create, read, update, and delete (CRUD).
 */
class BasicCrudRestTest extends VeoRestTest {

    String unitId
    String unitName

    def setup() {
        unitName = 'CRUD test unit'
        unitId = postNewUnit(unitName).resourceId

        UUID.fromString(unitId)
    }

    def cleanup() {
        delete("$baseUrl/units/$unitId", 204, UserType.DEFAULT)
    }

    def "CRUD unit"() {
        when: 'loading the unit'
        def getResponse = get("/units/$unitId", 200, UserType.DEFAULT)

        then: 'the id and name should match the unit previously created'
        getResponse.body.id == unitId
        getResponse.body.name == unitName

        when: 'loading all units'
        def getResponseAll = get("/units", 200, UserType.DEFAULT)

        then: 'the unit is found'
        getResponseAll.body*.id.contains(unitId)
        getResponseAll.body*.name.contains(unitName)

        when: 'updating the unit'
        def etag = getResponse.getETag()
        unitName = "New name of Unit"

        then: 'RESTapi responds with status code 200'
        put("/units/$unitId", ["name": unitName], etag, 200, UserType.DEFAULT)

        then: 'updating with the same eTag should no longer work (412)'
        put("/units/$unitId", ["name": unitName], etag,412, UserType.DEFAULT)
    }

    def "CRUD asset"() {
        when: 'given an asset'
        def assetName = 'CRUD test asset'
        def targetUri = "$baseUrl/units/$unitId"
        def asset = [name: assetName,
            subType: 'AST_Application',
            status: 'NEW',
            owner: [ targetUri: targetUri]
        ]

        and: 'creating an asset inside the unit'
        def postResponse = post("$baseUrl/domains/$dsgvoDomainId/assets", asset, 201, UserType.DEFAULT)

        then: 'Resource ID is a string'
        def assetId = postResponse.body.resourceId
        UUID.fromString(assetId)

        when: 'loading the asset'
        def getResponse = get("/assets/$assetId", 200, UserType.DEFAULT)

        then: 'returned asset has the correct values'
        getResponse.body.id == assetId
        getResponse.body.name == assetName
        getResponse.body.owner.displayName == unitName
        getResponse.body.owner.targetUri == "$baseUrl/units/$unitId"

        def assetEtag = getResponse.getETag()
        assetEtag instanceof String

        and: 'when loading all assets in the unit'
        def getResponseAllAssets = get("$baseUrl/assets?unit=$unitId", 200, UserType.DEFAULT)

        then: 'only one asset should be returned'
        getResponseAllAssets.body.totalItemCount == 1
        getResponseAllAssets.body.items[0].id == assetId
        getResponseAllAssets.body.items[0].name == assetName

        when:
        assetName = "New name of Asset"

        and: 'Status Code is correct when updating the asset'
        put("$baseUrl/domains/$dsgvoDomainId/assets/$assetId", [
            id: assetId,
            name: assetName,
            subType: 'AST_Application',
            status: 'NEW',
            owner: [displayName: unitName, targetUri: targetUri]
        ], assetEtag, 200, UserType.DEFAULT)

        and: 'Server denies updating the asset concurrently'
        put("$baseUrl/domains/$dsgvoDomainId/assets/$assetId", [
            id: assetId,
            name: assetName,
            subType: 'AST_Application',
            status: 'NEW',
            owner: [displayName: unitName, targetUri: targetUri]
        ], assetEtag, 412, UserType.DEFAULT)

        and: 'Deleting the asset'
        delete("$baseUrl/assets/$assetId", 204, UserType.DEFAULT)

        and: 'Creating an asset inside the unit with properties'
        def propertiesAssetName = 'CRUD test asset'
        def propertiesAssetBody = [
            name: propertiesAssetName,
            owner: [displayName: unitName, targetUri: targetUri],
            subType: "AST_Application",
            status: "NEW",
            riskValues: [
                DSRA : [
                    potentialImpacts: [
                        "C": 0,
                        "I": 1
                    ]
                ]
            ],
            customAspects: [
                asset_details: [
                    asset_details_number: 1,
                    asset_details_operatingStage: "asset_details_operatingStage_planning"
                ]
            ]
        ]
        def postPropertiesAssetResponse = post("$baseUrl/domains/$dsgvoDomainId/assets", propertiesAssetBody, 201, UserType.DEFAULT)
        def propertiesAssetId = postPropertiesAssetResponse.body.resourceId
        UUID.fromString(propertiesAssetId)

        and: 'Loading the asset'
        def getPropertiesAssetResponse = get("$baseUrl/assets/$propertiesAssetId", 200, UserType.DEFAULT)

        then: 'Proporties are set'
        getPropertiesAssetResponse.body.id == propertiesAssetId
        getPropertiesAssetResponse.body.name == propertiesAssetName
        getPropertiesAssetResponse.body.owner.displayName == unitName
        getPropertiesAssetResponse.body.owner.targetUri == targetUri
        getPropertiesAssetResponse.body.customAspects.containsKey('asset_details')
        //        getPropertiesAssetResponse.body.domains.containsKey('asset_details')
    }

    def "CRUD person in domain context"() {
        when: "creating a new person within a domain"
        def creationResponse = post("/domains/$dsgvoDomainId/persons", [
            name: "test person",
            owner: [targetUri: "/units/$unitId"],
            subType: "PER_Person",
            status: "NEW"
        ])
        def personId = creationResponse.body.resourceId
        def personInDomainUri = creationResponse.location

        then: "the response is correct"
        creationResponse.body.success
        creationResponse.body.message == "Person created successfully."
        personId.size() == 36
        personInDomainUri == "/domains/$dsgvoDomainId/persons/$personId"

        when: "retrieving the person from the viewpoint of the domain"
        def getResponse = get(personInDomainUri)
        def person = getResponse.body
        def eTag = getResponse.getETag()

        then: "the response is correct"
        person._self == baseUrl + personInDomainUri
        person.name == "test person"
        person.subType == "PER_Person"
        person.status == "NEW"
        eTag.size() > 2

        when: "updating the person"
        person.status = "ARCHIVED"
        put(personInDomainUri, person, eTag)

        then: "the change has been applied"
        with(get(personInDomainUri).body) {
            status == "ARCHIVED"
        }

        when: "deleting the domain association"
        // TODO VEO-2015
        // delete(personInDomainUri)

        then: "it's gone"
        // get(personInDomainUri, 404)
    }

    def "CRUD person"() {
        when: 'Creating a person inside the unit'
        def personName = 'CRUD test person'
        def targetUri = "$baseUrl/units/$unitId"

        then:
        def postResponsePerson = post("$baseUrl/domains/$dsgvoDomainId/persons", [
            name: personName,
            subType: 'PER_Person',
            status: 'NEW',
            owner: [displayName: unitName, targetUri: targetUri]
        ], 201, UserType.DEFAULT)

        then: 'Resource ID is a string and matches'
        def personId = postResponsePerson.body.resourceId
        UUID.fromString(personId)

        and: 'Loading all persons in the unit'
        def getResponseAllPersons = get("$baseUrl/persons?unit=$unitId", 200, UserType.DEFAULT)

        then: 'Only one person should be returned and it should match previously created person'
        getResponseAllPersons.body.totalItemCount == 1
        getResponseAllPersons.body.items[0].name == personName
        getResponseAllPersons.body.items[0].id == personId
        getResponseAllPersons.body.items[1] == null

        and: 'Loading only the previously created person'
        def getResponsePerson = get("$baseUrl/persons/$personId", 200, UserType.DEFAULT)

        then: 'Only one person should be returned and it should match previously created person'
        getResponsePerson.body.id == personId
        getResponsePerson.body.name == personName
        getResponsePerson.body[1] == null
        def personEtag = getResponsePerson.getETag()
        personEtag instanceof String

        and: 'Changing the name of the person'
        def newPersonName = 'Person with Mohammed Api'
        def putBody = [id: personId,
            name: newPersonName,
            subType: 'PER_Person',
            status: 'NEW',
            owner: [displayName: unitName, targetUri: targetUri]]
        put("$baseUrl/domains/$dsgvoDomainId/persons/$personId", putBody, personEtag, 200, UserType.DEFAULT)
        get("$baseUrl/persons/$personId").body.name == newPersonName

        and: 'Updating person concurrently'
        put("$baseUrl/domains/$dsgvoDomainId/persons/$personId", putBody, personEtag, 412, UserType.DEFAULT)

        and: 'Deleting the person'
        delete("$baseUrl/persons/$personId", 204, UserType.DEFAULT)

        and: 'Creating a person inside the unit with properties'
        def propertiesPersonName = 'CRUD test person'
        def postPropertiesPersonBody = [
            name: propertiesPersonName,
            subType: "PER_Person",
            status: "NEW",
            owner: [
                displayName: unitName,
                targetUri: targetUri],
            customAspects: [
                person_generalInformation: [
                    person_generalInformation_salutation: "Herr",
                    person_generalInformation_familyName: "Schmidt"]
            ]
        ]
        def postPropertiesPersonResponse = post("$baseUrl/domains/$dsgvoDomainId/persons", postPropertiesPersonBody, 201, UserType.DEFAULT)
        def propertiesPersonId = postPropertiesPersonResponse.body.resourceId
        UUID.fromString(propertiesPersonId)

        and: 'Loading the person with properties'
        def getPropertiesPersonResponse = get("$baseUrl/persons/$propertiesPersonId", 200, UserType.DEFAULT)
        getPropertiesPersonResponse.body.id == propertiesPersonId
        getPropertiesPersonResponse.body.name == propertiesPersonName
        getPropertiesPersonResponse.body.owner.displayName == unitName
        getPropertiesPersonResponse.body.owner.targetUri == targetUri
        getPropertiesPersonResponse.body.customAspects.containsKey('person_generalInformation')

        and: 'Deleting the person'
        delete("$baseUrl/persons/$propertiesPersonId", 204, UserType.DEFAULT)
    }

    def "CRUD control"() {
        when: 'Creating a control inside the unit'
        def targetUri = "$baseUrl/units/$unitId"
        def initialControlCount = get("$baseUrl//controls?unit=$unitId", 200, UserType.DEFAULT).body.items.size()
        def controlName = 'CRUD test control'
        def controlBody = [name: controlName,
            subType: 'CTL_TOM',
            status: 'NEW',
            owner: [displayName: unitName, targetUri: targetUri]]
        def postControlResponse = post("$baseUrl/domains/$dsgvoDomainId/controls", controlBody, 201, UserType.DEFAULT)
        def controlId = postControlResponse.body.resourceId
        UUID.fromString(controlId)

        and: 'Loading all controls in the unit'
        def getAllControlResponse = get("$baseUrl//controls?unit=$unitId", 200, UserType.DEFAULT)
        getAllControlResponse.body.items[0].id == controlId
        getAllControlResponse.body.items[0].name == controlName
        getAllControlResponse.body.items.size() == initialControlCount + 1

        and: 'Loading the previously created unit'
        def getControlResponse = get("$baseUrl//controls/$controlId", 200, UserType.DEFAULT)
        getControlResponse.body.id == controlId
        getControlResponse.body.name == controlName
        getControlResponse.body.owner.displayName == unitName
        getControlResponse.body.owner.targetUri == targetUri
        def controlEtag = getControlResponse.getETag()

        controlEtag instanceof String

        and: 'Updating the control'
        def newControlName = 'Control of Conrad'
        def putControlBody = [
            id: controlId,
            name: newControlName,
            subType: 'CTL_TOM',
            status: 'NEW',
            owner: [displayName: unitName, targetUri: targetUri]]
        put("$baseUrl/domains/$dsgvoDomainId/controls/$controlId", controlBody, controlEtag, 200, UserType.DEFAULT)

        and: 'Updating the control concurrently'
        put("$baseUrl/domains/$dsgvoDomainId/controls/$controlId", putControlBody, controlEtag, 412, UserType.DEFAULT)

        and: 'Deleting the control'
        delete("$baseUrl/controls/$controlId", 204, UserType.DEFAULT)

        and: 'Creating a control inside the unit with properties'
        def propertiesControlName = 'CRUD test control'
        def postPropertiesControlBody = [
            name: propertiesControlName,
            owner: [
                displayName: unitName,
                targetUri: targetUri
            ],
            subType: "CTL_TOM",
            status: "NEW",
            customAspects: [
                control_dataProtection: [
                    control_dataProtection_objectives: [
                        "control_dataProtection_objectives_pseudonymization"
                    ]
                ]
            ]
        ]
        def postPropertiesControlResponse = post("$baseUrl/domains/$dsgvoDomainId/controls", postPropertiesControlBody, 201, UserType.DEFAULT)
        def propertiesControlId = postPropertiesControlResponse.body.resourceId
        UUID.fromString(propertiesControlId)

        and: 'Loading the control with properties'
        def getPropertiesControlResponse = get("$baseUrl/controls/$propertiesControlId", 200, UserType.DEFAULT)

        then: 'Properties are set'
        getPropertiesControlResponse.body.id == propertiesControlId
        getPropertiesControlResponse.body.name == propertiesControlName
        getPropertiesControlResponse.body.owner.displayName == unitName
        getPropertiesControlResponse.body.owner.targetUri == targetUri
        getPropertiesControlResponse.body.customAspects.containsKey('control_dataProtection')
    }

    def "Filter elements"() {
        when: 'Creating a unit'
        def unitName = 'CRUD test unit'
        def unitBody = [name: unitName, domains:[
                [targetUri: "/domains/$dsgvoDomainId"]
            ]]
        def postUnitResponse = post("$baseUrl/units", unitBody, 201, UserType.DEFAULT)

        then: 'Resource ID is a string'
        def unitId = postUnitResponse.body.resourceId
        UUID.fromString(unitId)
        def targetUriUnit = "$baseUrl/units/$unitId"

        and: 'Creating an asset inside the unit'
        def assetBody = [
            name: 'an asset inside the unit',
            subType: 'AST_Application',
            status: 'NEW',
            owner: [displayName: unitName, targetUri: targetUriUnit]
        ]
        post("$baseUrl/domains/$dsgvoDomainId/assets", assetBody, 201, UserType.DEFAULT)

        and: 'Creating 2 processes inside the unit'
        def processBody = [
            name: 'a process inside the unit',
            subType: 'PRO_DataProcessing',
            status: 'NEW',
            owner: [displayName: unitName, targetUri: targetUriUnit]
        ]
        post("$baseUrl/domains/$dsgvoDomainId/processes", processBody, 201, UserType.DEFAULT)
        def processBody2 = [
            name: 'another process inside the unit',
            subType: 'PRO_DataProcessing',
            status: 'NEW',
            owner: [displayName: unitName, targetUri: targetUriUnit]
        ]
        post("$baseUrl/domains/$dsgvoDomainId/processes", processBody2, 201, UserType.DEFAULT)

        and: 'Creating a person inside the unit'
        def personBody = [
            name: 'a person inside the unit',
            subType: 'PER_Person',
            status: 'NEW',
            owner: [displayName: unitName, targetUri: targetUriUnit]
        ]
        post("$baseUrl/domains/$dsgvoDomainId/persons", personBody, 201, UserType.DEFAULT)

        and: 'Creating a control inside the unit'
        def controlBody = [
            name: 'a control inside the unit',
            subType: 'CTL_TOM',
            status: 'NEW',
            owner: [displayName: unitName, targetUri: targetUriUnit]
        ]
        post("$baseUrl/domains/$dsgvoDomainId/controls", controlBody, 201, UserType.DEFAULT)

        and: 'Creating a scenario inside the unit'
        def unitScenarioName2 = 'filter test scenario'
        def unitScenarioBody2 = [
            name: unitScenarioName2,
            subType: 'SCN_Scenario',
            status: 'NEW',
            owner: [displayName: unitName, targetUri: targetUriUnit]
        ]
        def postUnitScenarioResponse = post("$baseUrl/domains/$dsgvoDomainId/scenarios", unitScenarioBody2, 201, UserType.DEFAULT)
        def unitScenarioId = postUnitScenarioResponse.body.resourceId
        UUID.fromString(unitScenarioId)

        and: 'Loading all scenarios in the unit'
        def getScenarioResponse = get("$baseUrl/scenarios?unit=$unitId", 200, UserType.DEFAULT)
        getScenarioResponse.body.items*.id ==~ [
            unitScenarioId
        ]

        and: 'Loading scenarios with an non existing unit fails'
        def nonExistingUnitId = UUID.randomUUID().toString()
        get("$baseUrl/scenarios?unit=$nonExistingUnitId", 404, UserType.DEFAULT)

        and: 'Deleting the unit'
        delete("$baseUrl/units/$unitId", 204, UserType.DEFAULT)
    }

    def "error messages are returned"() {
        when:
        def responseBody = post("/domains/$testDomainId/assets", [:], 400).body

        then:
        responseBody.name == "A name must be present."
        responseBody.owner == "An owner must be present."

        expect:
        post("/domains/$testDomainId/incidents", [
            name: "terrible incident",
            owner: [targetUri: "/units/$unitId"],
            subType: "DISASTER",
            status: "DETECTED",
            customAspects: [
                fantasyCa: [
                    fantasyAttr: 99
                ]
            ]
        ], 400).body.message == "Custom aspect 'fantasyCa' is not defined"
    }

    def "moving an element between units is forbidden"() {
        given: "an asset in main unit"
        def assetId = post("/domains/$testDomainId/assets", [
            name: "sneaky asset",
            subType: 'Information',
            status: 'CURRENT',
            owner: [targetUri: "/units/$unitId"]
        ]).body.resourceId
        def otherUnitId = postNewUnit().resourceId

        expect: "that the elements cannot be moved to the other unit"
        get("/domains/$testDomainId/assets/$assetId").with {
            body.owner = [targetUri: "/units/$otherUnitId"]
            put(body._self, body, getETag(), 422)
        }.body.message == "Elements cannot be moved between units"
    }

    def "linking an element in another unit is forbidden"() {
        given: "a person in main unit"
        def personUri = post("/domains/$testDomainId/persons", [
            name: "mod",
            subType: "MasterOfDisaster",
            status: "CAUSING_REAL_DISASTERS",
            owner: [targetUri: "/units/$unitId"]
        ]).location
        def otherUnitId = postNewUnit().resourceId

        expect: "that the person cannot be linked from another unit"
        post("/domains/$testDomainId/assets", [
            name: "sneaky asset",
            subType: "Server",
            status: "RUNNING",
            owner: [targetUri: "/units/$otherUnitId"],
            links: [
                admin: [
                    [
                        target: [targetUri: personUri]
                    ]
                ]
            ]
        ], 422).body.message == "Elements in other units must not be referenced"
    }
}