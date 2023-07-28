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

import java.time.Instant

import groovy.util.logging.Slf4j

@Slf4j
/**
 * Tests creation of elements from the catalog 'dsgvo.json'.
 * The catalog contains six controls: C-1, C-2, C-3, C-4, CC-1 and CC-2
 * They have the following tailoring references between them:
 * C1
 * C3       -link-> C1
 * C2
 * C4       -link_external-> C2
 * CC-1     -link-> CC-2
 * CC-2     -link-> CC-1
 *
 * The catalog items are applied in descending order (C-4, C-3, C-2, C.1 ...) by this test.
 * This means that C-3 cannot be created at first because C-1 does not exist: no link could
 * be created from C-3 to C-1.
 * A second pass will then be run to create a second instance of C-1 and C-2 as well as
 * one instance of C-3.
 */
class IncarnateCatalogRestTestITSpec extends VeoRestTest {
    String unitId

    def setup() {
        unitId = postNewUnit().resourceId
    }

    def "expected catalog items are present"() {
        expect:
        getCatalogItems(testDomainId).size() == 7
        getCatalogItems(dsgvoDomainId).size() == 65
    }

    def "Create linked elements from a catalog"() {
        when: "a selection of catalog items is applied"
        def elementResults
        log.info("===============> first pass")
        elementResults = applyCatalogItems(testDomainId, ["Control-1", "Control-2"])

        then: "only elements without references to other catalog items were created"
        elementResults.size() == 2
        elementResults.collect { it.name }.sort() == [
            'Control-1',
            'Control-2'
        ]
        String pass1Element1Id = elementResults.find { it.abbreviation == 'c-1' }?.id
        !pass1Element1Id.isBlank()

        when: "the same catalog items are applied again"
        log.info("===============> second pass")
        elementResults = applyCatalogItems(testDomainId)

        then: "all elements were created"
        // This time, C-3 was created as well. C-3 was created with a link to
        // the instance of C-1 created in the first pass.
        elementResults.size() == 5
        elementResults.collect { it.name }.sort() == [
            'Control-1',
            'Control-2',
            'Control-3',
            'Control-4',
            'General server'
        ]
        // check if item1 was created again with a new ID:
        pass1Element1Id != elementResults.find { it.abbreviation == 'c-1' }?.id

        // check if C-3 from the second pass was linked to C-1 from the first pass:
        def uri = elementResults
                .find { it.abbreviation == 'c-3' }
                ?.links
                ?.Control_details_Control[0]
                .target
                .targetUri
        assert extractLastId(uri) == pass1Element1Id
    }

    def "Create elements with reversed links from catalog"() {
        when: "a control is created"
        def sourceControlId = post("/controls", [
            name : "Link Target Control",
            owner: [targetUri: "$baseUrl/units/$unitId"],
            domains: [
                (testDomainId): [
                    subType: "TOM",
                    status: "NEW",
                ]
            ]
        ]).body.resourceId

        and: "C-4 is instantiated"
        def elementResults = applyCatalogItems(testDomainId, ["Control-4"], "/controls/$sourceControlId")
        String c4Id = elementResults.first().id

        then: "C-4 was created"
        elementResults.size() == 1
        with(elementResults.first()) {
            name == "Control-4"
            abbreviation == "c-4"
            links.size() == 0
            !c4Id.isBlank()
        }

        when: "the source control is queried"
        def sourceControl = getControl(sourceControlId)

        then: "it now contains a link to C-4"
        sourceControl.links.size() == 1
        sourceControl.links.Control_details_Control.target.displayName ==~ /.*Control-4.*/
        sourceControl.links.Control_details_Control.target.targetUri ==~ /.*\/controls\/$c4Id.*/
    }

    def "Create elements with reversed links from catalog to wrong element"() {
        when: "a control is created"
        def sourceControlId = post("/controls", [
            name : "Link Target Control",
            owner: [targetUri: "$baseUrl/units/$unitId"],
        ]).body.resourceId

        and: "C-4 is instantiated"
        def elementResults = applyCatalogItems(testDomainId, ["Control-4"], "/controls/$sourceControlId", false)

        then: "an error messages is returned"
        elementResults.message ==~/The element to link is not part of the domain: CTL.*/
    }

    def "Create linked elements from the dsgvo catalog"() {
        when:"we create the controls"
        def sourceProcessId = post("/processes", [
            name: "process",
            domains: [
                (dsgvoDomainId): [
                    subType: "PRO_DataProcessing",
                    status: "NEW"
                ]
            ],
            owner: [targetUri: "$baseUrl/units/$unitId"]
        ]).body.resourceId

        def elementResults = applyCatalogItems(dsgvoDomainId, [
            "TOM zur Gewährleistung der Vertraulichkeit"
        ], "/processes/$sourceProcessId")
        get("/processes/${sourceProcessId}").body.links.size()==1

        elementResults = applyCatalogItems(dsgvoDomainId, [
            "TOM zur Gewährleistung der Verfügbarkeit"
        ], "/processes/$sourceProcessId")
        get("/processes/${sourceProcessId}").body.links.size()==2

        elementResults = applyCatalogItems(dsgvoDomainId, [
            "TOM zur Wiederherstellbarkeit"
        ], "/processes/$sourceProcessId")
        get("/processes/${sourceProcessId}").body.links.size()==3

        elementResults = applyCatalogItems(dsgvoDomainId, [
            "TOM zur Verschlüsselung"
        ], "/processes/$sourceProcessId")
        get("/processes/${sourceProcessId}").body.links.size()==4

        elementResults = applyCatalogItems(dsgvoDomainId, [
            "Verfahren regelmäßiger Überprüfung, Bewertung und Evaluierung der Wirksamkeit der TOM"
        ], "/processes/$sourceProcessId")
        get("/processes/${sourceProcessId}").body.links.size()==5

        elementResults = applyCatalogItems(dsgvoDomainId, [
            "TOM zur Gewährleistung der Integrität"
        ], "/processes/$sourceProcessId")
        get("/processes/${sourceProcessId}").body.links.size()==6

        elementResults = applyCatalogItems(dsgvoDomainId, [
            "TOM zur Pseudonymisierung"
        ], "/processes/$sourceProcessId")
        get("/processes/${sourceProcessId}").body.links.size()==7

        elementResults = applyCatalogItems(dsgvoDomainId, [
            "TOM zur Gewährleistung der Belastbarkeit"
        ], "/processes/$sourceProcessId")
        get("/processes/${sourceProcessId}").body.links.size()==8

        elementResults = applyCatalogItems(dsgvoDomainId, ["VVT"], null)
        def processVVT = elementResults.first()

        then:"The process is linked with the controlls"
        processVVT.links.size() == 1
        processVVT.links.process_tom.size() == 8
    }

    def "Create all linked elements from the dsgvo catalog in one step"() {
        when:"we create all controls"
        def allItems = getCatalogItems(dsgvoDomainId)*.id.join(',')
        log.debug("==> allItems: {}", allItems)

        def incarnationDescription = get("/units/${unitId}/incarnations?itemIds=${allItems}").body
        def elementResults = postIncarnationDescriptions(unitId, incarnationDescription)

        log.debug("==> elementResults: {}", elementResults)

        then: "all elements are created"
        elementResults.size() == 65

        and: "all elements have subtype information"
        elementResults.each { element ->
            String targetUri = element.targetUri
            def elementResult = get(targetUri).body
            log.debug("==> elementResult: {}", elementResult)

            assert elementResult.domains[dsgvoDomainId].subType != null
            assert elementResult.domains[dsgvoDomainId].status != null
        }
    }

    private applyCatalogItems(String domainId, selectedItemNames = null, sourceElementUri = null, boolean succesful = true) {
        def elementResults = []
        getCatalogItems(domainId)
                .sort { it.abbreviation }
                .reverse()
                .findAll { selectedItemNames == null || selectedItemNames.contains(it.name) }
                .each {
                    log.info("Read catalog item: {}", it.name)

                    when: "list an item"
                    def itemId = it.id

                    and: "get the apply information"
                    def applyInfo = getIncarnationDescriptions(unitId, itemId)
                    log.info("Catalogitem {} has {} references", it.abbreviation, applyInfo.parameters.first().references.size())

                    // change apply info:
                    if (sourceElementUri != null) {
                        applyInfo.parameters.first().references.first().put("referencedElement", [
                            "targetUri": "$sourceElementUri"
                        ])
                    }

                    and: "create only items without references to other catalog items"
                    def beforeCreation = Instant.now()
                    // skip items that have references pointing towards other elements when
                    // those elements have not been created yet (see the fix-me regarding VEO-726):
                    if (applyInfo.parameters.first().references.size() == 0 // no reference present
                            || applyInfo.parameters.first().references[0].referencedElement != null // reference to an element previously created from catalog
                            ) {
                        log.info("Will be applied: {}", it.name)

                        def postApply = postIncarnationDescriptions(unitId, applyInfo, succesful ? 201 : 400)
                        if(succesful) {

                            and: "get the created element"
                            def elementResult = get(postApply.first().targetUri).body
                            log.info("Incarnated element {}", elementResult)
                            elementResults.add(elementResult)

                            assert it.name == elementResult.name
                            assert Instant.parse(elementResult.createdAt) > beforeCreation
                            assert Instant.parse(elementResult.updatedAt) > beforeCreation
                            assert !elementResult.description.isBlank()
                            assert !elementResult.id.isBlank()
                        } else {
                            elementResults = postApply
                            return
                        }
                    }
                }
        return elementResults
    }

    private extractLastId(String targetUri) {
        targetUri.split('/').last()
    }

    private getIncarnationDescriptions(String unitId, String... itemIds) {
        get("/units/${unitId}/incarnations?itemIds=${itemIds.join(',')}").body
    }

    private postIncarnationDescriptions(unitId, applyInfo, int expectedStatus = 201) {
        log.info("postIncarnationDescriptions before: {}", applyInfo)
        def response = post("/units/${unitId}/incarnations", applyInfo, expectedStatus)
        log.info("postIncarnationDescriptions after: {}", response.body)
        response.body
    }
}