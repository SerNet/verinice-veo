/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman
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

class IncarnateCatalogItemsRestTestITSpec extends VeoRestTest {
    final def UNIT_NAME = 'incarnate catalog item test unit'

    String unitId

    def setup() {
        unitId = postNewUnit(UNIT_NAME).resourceId
    }

    def "Create a unit and incarnate c-1 and cc-1"() {
        when:
        def itemC1Id = itemIdByAbbreviation("c-1")

        def incarnationDescriptions = getIncarnationDescriptions([itemC1Id])
        def newElements = incarnate(incarnationDescriptions)
        def createdElementC1 = newElements.first().targetUri

        def itemCC1Id = itemIdByAbbreviation("cc-1")

        incarnationDescriptions = getIncarnationDescriptions([itemCC1Id])
        incarnationDescriptions.parameters.first().references[0].put("referencedElement", ["targetUri":createdElementC1])

        newElements = incarnate(incarnationDescriptions)
        def controlCC1tResult = get(newElements.first().targetUri).body

        then: "the control cc-1 ist created and linked to c-1"
        with(controlCC1tResult) {
            name == "Control-cc-1"
            abbreviation == "cc-1"
            description.startsWith("Lorem ipsum")
            domains[owner.testDomainId] == [
                subType: "TOM",
                status: "NEW",
                decisionResults: [:],
                riskValues: [:],
            ]
            // Mind the difference between Closure#owner and Element#owner
            it.owner.displayName == owner.UNIT_NAME
            links.size() == 1
            links["Control_details_Control"].domains.size() == 1
            links["Control_details_Control"].domains[0].displayName[0] == "td test-domain"
            links["Control_details_Control"].target.targetUri[0] == createdElementC1
        }
    }

    def "Create a unit and incarnate c-1 and c-2 in one request"() {
        when: "we get c1 and c2 id"
        def itemC1Id = itemIdByAbbreviation("c-1")
        def itemC2Id = itemIdByAbbreviation("c-2")
        def incarnationDescription = getIncarnationDescriptions([itemC1Id, itemC2Id])

        and: "we post the given description"
        def newElements = incarnate(incarnationDescription)

        then: "two elements are created"
        newElements.size() == 2

        when: "we access the created elements"
        def controlC1Result = get(newElements[0].targetUri).body
        def controlC2Result = get(newElements[1].targetUri).body

        then: "The data is correct"
        controlC1Result.name == "Control-1"
        controlC2Result.name == "Control-2"
    }

    def "Create a unit and incarnate c-1 and cc-1->c-1, and cc-2 linked default to cc-1"() {
        when:
        def itemC1Id = itemIdByAbbreviation("c-1")
        def incarnationDescription = getIncarnationDescriptions([itemC1Id])
        def newElements = incarnate(incarnationDescription)
        def createdElementC1 = newElements.first().targetUri

        def itemCC1Id = itemIdByAbbreviation("cc-1")
        incarnationDescription = getIncarnationDescriptions([itemCC1Id])
        incarnationDescription.parameters.first().references[0].put("referencedElement", ["targetUri":createdElementC1])
        newElements = incarnate(incarnationDescription)
        def controlCC1TargetUri = newElements.first().targetUri

        def itemCC2Id = itemIdByAbbreviation("cc-2")
        incarnationDescription = getIncarnationDescriptions([itemCC2Id])

        then: "the link reference is filled with the suggested cc-1"
        incarnationDescription.parameters[0].references[0].referencedElement.targetUri == controlCC1TargetUri

        when: "we post the given incarnationDescription"
        newElements = incarnate(incarnationDescription)
        def controlCC2tResult = get(newElements.first().targetUri).body

        then: "the control cc-2 ist created and linked to cc-1"
        with(controlCC2tResult) {
            name == "Control-cc-2"
            abbreviation == "cc-2"
            description.startsWith("Lorem ipsum")
            it.owner.displayName == owner.UNIT_NAME
            domains[owner.testDomainId] == [
                subType: "TOM",
                status: "NEW",
                decisionResults: [:],
                riskValues: [:],
            ]
            links.size() == 1
            links["Control_details_Control"].domains.size() == 1
            links["Control_details_Control"].domains[0].displayName[0] == "td test-domain"
            links["Control_details_Control"].target.targetUri[0] == controlCC1TargetUri
        }
    }

    def "Create a unit incarnate c-1 and build a list by linking the last cc-1 10 times"() {
        when:
        def itemC1Id = itemIdByAbbreviation("c-1")
        def incarnationDescriptions = getIncarnationDescriptions([itemC1Id])
        def createdElement = incarnate(incarnationDescriptions).first().targetUri

        def itemCC1Id = itemIdByAbbreviation("cc-1")
        incarnationDescriptions = getIncarnationDescriptions([itemCC1Id])
        10.times {
            incarnationDescriptions.parameters.first().references[0].put("referencedElement", ["targetUri":createdElement])
            createdElement = incarnate(incarnationDescriptions).first().targetUri
        }
        def lastControl = uriToId(createdElement)
        def controls = getControls()

        then:
        controls.totalItemCount == 11

        when: "we walk back the list in reverse"
        def counter = 0
        def controlResult = null
        while (lastControl != null) {
            controlResult = getControl(lastControl)
            lastControl = controlResult.links.Control_details_Control?.with { uriToId(it.target.targetUri[0]) }
            counter++
        }

        then: "we are back where we started"
        counter == 11
        controlResult.name == "Control-1"
    }

    def "Create a unit and incarnate c-2 10 times"() {
        when: "c-2 is incarnated 10 times"
        def itemC2Id = itemIdByAbbreviation("c-2")
        def incarnationDescription = getIncarnationDescriptions([itemC2Id])
        10.times {
            incarnate(incarnationDescription)
        }

        def controls = getControls()

        then: "10 items are returned"
        with(controls) {
            totalItemCount == 10
            items[0].name == "Control-2"
            items[0].abbreviation == "c-2"
            items[0].description.startsWith("Lorem ipsum")
            items[0].owner.displayName == owner.UNIT_NAME
            items[0].domains[owner.testDomainId] == [
                subType: "TOM",
                status: "NEW",
                decisionResults: [:],
                riskValues: [:],
            ]
        }
    }

    def "Create a unit and incarnate cc-1->cc-2 in one step"() {
        when:
        def itemCC1Id = itemIdByAbbreviation("cc-1")
        def itemCC2Id = itemIdByAbbreviation("cc-2")
        def incarnationDescription = getIncarnationDescriptions([itemCC1Id, itemCC2Id])
        def newElements = incarnate(incarnationDescription)

        def controlCC1tResult = get(newElements[0].targetUri).body
        def controlCC2tResult = get(newElements[1].targetUri).body

        then: "the control cc-1 ist created and linked to cc-2 and cc-2 is created and linked to cc-1"
        with(controlCC1tResult) {
            name == "Control-cc-1"
            abbreviation == "cc-1"
            description.startsWith("Lorem ipsum")
            domains[owner.testDomainId] == [
                subType: "TOM",
                status: "NEW",
                decisionResults: [:],
                riskValues: [:],
            ]
            it.owner.displayName == owner.UNIT_NAME
            links.size() == 1
            links["Control_details_Control"].domains.size() == 1
            links["Control_details_Control"].domains[0].displayName[0] == "td test-domain"
            links["Control_details_Control"].target.targetUri[0] == controlCC2tResult._self
        }

        with(controlCC2tResult) {
            name == "Control-cc-2"
            abbreviation == "cc-2"
            description.startsWith("Lorem ipsum")
            domains[owner.testDomainId] == [
                subType: "TOM",
                status: "NEW",
                decisionResults: [:],
                riskValues: [:],
            ]
            it.owner.displayName == owner.UNIT_NAME
            links.size() == 1
            links["Control_details_Control"].domains.size() == 1
            links["Control_details_Control"].domains[0].displayName[0] == "td test-domain"
            links["Control_details_Control"].target.targetUri[0] == controlCC1tResult._self
        }
    }

    def "Create a unit and the whole catalog in one step"() {
        when: "We create all elements"
        def catalogItemsIds = getCatalogItems(testDomainId)*.id

        def incarnationDescription = getIncarnationDescriptions(catalogItemsIds)
        def newElements = incarnate(incarnationDescription)

        then: "all items of the catalog are created"
        newElements.size() == catalogItemsIds.size()
    }

    def "Create a unit and one tom, then add the tom again and again"() {
        when: "We create one element"
        def tomi = itemIdByAbbreviation("TOM-I", dsgvoDomainId)
        def incarnationDescription = get("/units/${unitId}/incarnations?itemIds=${tomi}").body
        incarnate(incarnationDescription)

        and: "We add the same control from the catalog"
        def idTom = get("/units/${unitId}/incarnations?itemIds=${tomi}").body
        def toms = incarnate(idTom)
        def tom1 = get(toms[0].targetUri).body

        then: "we check some basic data"
        toms.size() == 1
        tom1.abbreviation == "TOM-I"

        when: "We add the same element again"
        idTom = get("/units/${unitId}/incarnations?itemIds=${tomi}").body
        toms = incarnate(idTom)
        tom1 = get(toms[0].targetUri).body

        then: "we check some basic data"
        toms.size() == 1
        tom1.abbreviation == "TOM-I"
    }

    def "Create a unit and the whole dsgvo catalog in one step add controls after"() {
        when: "We create all elements"
        def catalogItemsIds = getCatalogItems(dsgvoDomainId)*.id
        def incarnationDescription = getIncarnationDescriptions(catalogItemsIds)
        def newElements = incarnate(incarnationDescription)

        then: "all items of the catalog are created"
        newElements.size() == catalogItemsIds.size()

        when: "We add other controls from the catalog"
        def tomi = itemIdByAbbreviation("TOM-I", dsgvoDomainId)
        def tome = itemIdByAbbreviation("TOM-P", dsgvoDomainId)

        def idTom = get("/units/${unitId}/incarnations?itemIds=${tomi},${tome}").body
        def toms = incarnate(idTom)

        def tom1 = get(toms[0].targetUri).body
        def tom2 = get(toms[1].targetUri).body

        then: "we check some basic data"
        toms.size() == 2
        tom1.abbreviation  == "TOM-I"
        tom2.abbreviation  == "TOM-P"
    }

    private String itemIdByAbbreviation(String abbreviation, String domainId = testDomainId) {
        getCatalogItems(domainId).find { it.abbreviation == abbreviation }.id
    }

    private uriToId(String targetUri) {
        targetUri.split('/').last()
    }

    private Object getIncarnationDescriptions(Collection<String> itemIds) {
        get("/units/${unitId}/incarnations?itemIds=${itemIds.join(',')}").body
    }

    private getControls() {
        get("/controls?unit=${unitId}").body
    }

    private incarnate(descriptions) {
        post("/units/${unitId}/incarnations", descriptions).body
    }
}