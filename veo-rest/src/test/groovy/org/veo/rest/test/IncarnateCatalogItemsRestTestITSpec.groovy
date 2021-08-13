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

import groovy.util.logging.Slf4j

@Slf4j
class IncarnateCatalogItemsRestTestITSpec extends VeoRestTest {

    public static final String UNIT_NAME = 'Testunit'

    def postResponse
    def getDomains
    def getCatalog
    String unitId
    String catalogId

    def setup() {
        postResponse = postNewUnit(UNIT_NAME)
        unitId = postResponse.resourceId
        getDomains = getDomains()
        catalogId = extractLastId(getDomains.catalogs.first().targetUri)
        getCatalog = getCatalog(catalogId)
    }

    def "Create a unit and get ApplyCatalogItems info for Items"() {
        log.info("Create a unit and get ApplyCatalogItems info for Items")
        when: "a unit is created"
        def postResponse = postNewUnit(UNIT_NAME)

        and: "the domains are requested"
        def getDomains = getDomains()

        then:"the catalogs size matches"
        with(getDomains) {
            catalogs.size() == 1
        }

        and: "list the catalog"
        def catalogId = extractLastId(getDomains.catalogs.first().targetUri)
        def getCatalog = getCatalog(catalogId)
        then:"the catalogItems size matches"

        with(getCatalog) {
            catalogItems.size() == 5
        }

        log.info("===============> first pass")
        getCatalog.catalogItems.each {
            log.info("--------> {}", it.displayName)
            when: "list an item"
            def itemId = extractLastId(it.targetUri)
            def getItem = getCatalogItem(catalogId, itemId)
            log.info("process catalogitem: {}", getItem.namespace)

            and: "get the element"
            def elementId = extractLastId(getItem.element.targetUri)
            log.info("get catalogitem.element: {}", elementId)
            def catalogElementResult = getControl(elementId)

            and: "get the apply information"
            def getApplyInfo = getIncarnationDescriptions(postResponse.resourceId, itemId)
            log.info("--------> apply:{}", getApplyInfo.parameters.first().references.size())
            and: "use use this info to apply the item"
            if (  getApplyInfo.parameters.first().references.size() == 0
                    ||  getApplyInfo.parameters.first().references[0].referencedCatalogable != null
                    ) {
                log.info("--------> post apply")
                def postApply = postIncarnationDescriptions(postResponse.resourceId, getApplyInfo)

                and: "get the element"
                def controlId = extractLastId(postApply.first().targetUri)
                log.info("--------------> incarnated element")
                def elementResult = getControl(controlId)
            }
        }

        log.info("===============> second pass")
        getCatalog.catalogItems.each {
            def itemId = extractLastId(it.targetUri)
            def getApplyInfo = getIncarnationDescriptions(postResponse.resourceId, itemId)
            log.info("--------> apply:{}", getApplyInfo.parameters.first().references.size())
            and: "use use this info to apply the item"
            if (  getApplyInfo.parameters.first().references.size() == 0
                    ||  getApplyInfo.parameters.first().references[0].referencedCatalogable != null
                    ) {
                log.info("--------> post apply")
                def postApply = postIncarnationDescriptions(postResponse.resourceId, getApplyInfo)

                and: "get the element"
                def controlid = extractLastId(postApply.first().targetUri)
                log.info("--------------> incarnated element")
                def elementResult = getControl(controlid)
            }
        }
    }

    def "Create a unit and incarnate c-1 and cc-1"() {
        log.info("Create a unit and incarnate c-1 and cc-1")
        when:
        def itemc1 = itemIdByDisplayName(getCatalog, "NO_DESIGNATOR c-1 Control-1")

        log.info("===========> get incarnation description")
        def incarnationDescription = getIncarnationDescriptions(unitId, itemc1)
        log.info("===========> post incarnation description")
        def postApply = postIncarnationDescriptions(postResponse.resourceId, incarnationDescription)
        def createdElementC1 = postApply.first().targetUri

        itemc1 = itemIdByDisplayName(getCatalog, "NO_DESIGNATOR cc-1 Control-cc-1")

        log.info("===========> get incarnation description")
        incarnationDescription = getIncarnationDescriptions(unitId, itemc1)
        incarnationDescription.parameters.first().references[0].put("referencedCatalogable", ["targetUri":createdElementC1])
        log.info("===========> {}", incarnationDescription)

        log.info("===========> post incarnation description")
        postApply = postIncarnationDescriptions(unitId, incarnationDescription)
        def controlCC1Id = extractLastId(postApply.first().targetUri)
        log.info("===========> get control {}", controlCC1Id)
        def controlCC1tResult = getControl(controlCC1Id)

        then: "the controll cc-1 ist created and linked to c-1"
        postApply != null
        with(controlCC1tResult) {
            name == "Control-cc-1"
            abbreviation == "cc-1"
            description.startsWith("Lorem ipsum")
            domains[0].displayName == "DSGVO DSGVO"
            links.size() ==1
            links["Control_details_Control"].domains.size() == 1
            links["Control_details_Control"].domains[0].displayName[0] == "DSGVO DSGVO"
            links["Control_details_Control"].target.targetUri[0] == createdElementC1
        }
        controlCC1tResult.owner.displayName == "Testunit"
    }


    def "Create a unit and incarnate c-1 and c-2 in one request"() {
        log.info("Create a unit and incarnate c-1 and c-2 in one request")
        when: "we get c1 and c2 id"
        def itemC1Id = itemIdByDisplayName(getCatalog, "NO_DESIGNATOR c-1 Control-1")
        def itemC2Id = itemIdByDisplayName(getCatalog, "NO_DESIGNATOR c-2 Control-2")
        def incarnationDescription = getIncarnationDescriptions(unitId, itemC1Id, itemC2Id)

        and: "we post the given description"
        def postApply = postIncarnationDescriptions(postResponse.resourceId, incarnationDescription)
        then: "two elements are created"

        postApply.size() == 2
        when: "we access the created elements"
        def createdElementC1 = extractLastId(postApply[0].targetUri)
        def createdElementC2 = extractLastId(postApply[1].targetUri)

        def controlC1Result = getControl(createdElementC1)
        def controlC2Result = getControl(createdElementC2)
        then: "The data is correct"
        controlC1Result.name == "Control-1"
        controlC2Result.name == "Control-2"
    }

    def "Create a unit and incarnate c-1 and cc-1->c-1, and cc-2 linked default to cc-1"() {
        log.info("Create a unit and incarnate c-1 and cc-1->c-1, and cc-2 linked default to cc-1")
        when:
        def itemC1Id = itemIdByDisplayName(getCatalog, "NO_DESIGNATOR c-1 Control-1")
        log.info("===========> create element c1 by incarnation")
        def incarnationDescription = getIncarnationDescriptions(unitId, itemC1Id)
        def postApply = postIncarnationDescriptions(postResponse.resourceId, incarnationDescription)
        def createdElementC1 = postApply.first().targetUri

        log.info("===========> create element cc-1 by incarnation linked to c-1")
        def itemCC1Id = itemIdByDisplayName(getCatalog, "NO_DESIGNATOR cc-1 Control-cc-1")
        incarnationDescription = getIncarnationDescriptions(unitId, itemCC1Id)
        incarnationDescription.parameters.first().references[0].put("referencedCatalogable", ["targetUri":createdElementC1])
        postApply = postIncarnationDescriptions(postResponse.resourceId, incarnationDescription)
        def controlCC1Id = extractLastId(postApply.first().targetUri)
        def controlCC1TargetUri = postApply.first().targetUri

        log.info("===========> create element cc-2 by incarnation linked to cc-1")
        def itemCC2Id = itemIdByDisplayName(getCatalog, "NO_DESIGNATOR cc-2 Control-cc-2")
        incarnationDescription = getIncarnationDescriptions(unitId, itemCC2Id)
        log.info("===========> create element {}", incarnationDescription)
        then: "the link reference is filled with the suggested cc-1"
        incarnationDescription.parameters[0].references[0].referencedCatalogable.targetUri == controlCC1TargetUri

        when: "we post the given incarnationDescription"
        postApply = postIncarnationDescriptions(unitId, incarnationDescription)
        def controlCC2Id = extractLastId(postApply.first().targetUri)

        log.info("===========> get control {}", controlCC2Id)
        def controlCC2tResult = getControl(controlCC2Id)

        then: "the controll cc-2 ist created and linked to cc-1"
        postApply != null
        with(controlCC2tResult) {
            name == "Control-cc-2"
            abbreviation == "cc-2"
            description.startsWith("Lorem ipsum")
            domains[0].displayName == "DSGVO DSGVO"
            links.size() ==1
            links["Control_details_Control"].domains.size() == 1
            links["Control_details_Control"].domains[0].displayName[0] == "DSGVO DSGVO"
            links["Control_details_Control"].target.targetUri[0] == controlCC1TargetUri
        }
        controlCC2tResult.owner.displayName == "Testunit"
    }

    def "Create a unit incarnate c-1 and build a list by linking the last cc-1 100 times"() {
        log.info("Create a unit incarnate c-1 and build a list by linking the last cc-1 100 times")
        when:
        def itemC1Id = itemIdByDisplayName(getCatalog, "NO_DESIGNATOR c-1 Control-1")
        log.info("===========> create element c1 by incarnation as head of the list")
        def incarnationDescription = getIncarnationDescriptions(unitId, itemC1Id)
        def postApply = postIncarnationDescriptions(postResponse.resourceId, incarnationDescription)
        def createdElement = postApply.first().targetUri

        log.info("===========> create linked list")
        def itemCC1Id = itemIdByDisplayName(getCatalog, "NO_DESIGNATOR cc-1 Control-cc-1")
        incarnationDescription = getIncarnationDescriptions(unitId, itemCC1Id)
        100.times {
            incarnationDescription.parameters.first().references[0].put("referencedCatalogable", ["targetUri":createdElement])
            postApply = postIncarnationDescriptions(postResponse.resourceId, incarnationDescription)
            createdElement = postApply.first().targetUri
        }
        def lastCntrol = extractLastId(createdElement)
        def controls = getControlsForUnit(unitId)
        then:
        controls != null
        controls.totalItemCount == 101

        when: "we walk back the list in reverse"
        log.info("===========> create walk the list up")
        def counter = 0
        def controlResult = null
        while (lastCntrol!=null) {
            controlResult = getControl(lastCntrol)
            lastCntrol = controlResult.links["Control_details_Control"] == null ? null : extractLastId(controlResult.links["Control_details_Control"].target.targetUri[0])
            counter++
        }
        log.info("===========> list walked back")
        then: "we are back where we started"
        counter == 101
        controlResult.name == "Control-1"
    }

    def "Create a unit and incarnate c-2 100 times"() {
        when:
        def itemC1Id = itemIdByDisplayName(getCatalog, "NO_DESIGNATOR c-2 Control-2")
        def incarnationDescription = getIncarnationDescriptions(postResponse.resourceId, itemC1Id)
        100.times {
            def postApply = postIncarnationDescriptions(postResponse.resourceId, incarnationDescription)
            def createdElementC1 = postApply.first().targetUri
        }

        def controls = getControlsForUnit(unitId)
        then: "100 items are returned"

        log.info("===========> list controlls")
        controls != null
        with(controls) {
            totalItemCount == 100
            items[0].name == "Control-2"
            items[0].abbreviation == "c-2"
            items[0].description.startsWith("Lorem ipsum")
            items[0].owner.displayName == "Testunit"
            items[0].domains[0].displayName == "DSGVO DSGVO"
        }
    }

    private itemIdByDisplayName(catalog, displayName) {
        String tu = catalog.catalogItems
                .find{it.displayName == displayName}.targetUri
        extractLastId(tu)
    }

    private extractLastId(String targetUri) {
        targetUri.split('/').last()
    }

    private getIncarnationDescriptions(String unitId, String... itemIds) {
        get("/units/${unitId}/incarnations?itemIds=${itemIds.join(',')}").body
    }

    private getCatalogItem(catalogId, id) {
        get("/catalogs/${catalogId}/items/${id}").body
    }

    private getControlsForUnit(id) {
        get("/controls?unit=${id}").body
    }

    private postIncarnationDescriptions(unitId, applyInfo) {
        def response = post("/units/${unitId}/incarnations", applyInfo)
        log.info("postIncarnationDescriptions: {}",response.body)
        response.body
    }
}