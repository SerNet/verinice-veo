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

    def "existing incarnations are used as reference targets"() {
        when: "c1 is incarnated without the link"
        def pass1Elements = postIncarnationDescriptions(getIncarnationDescriptions(testDomainId, ["Control-1"], ["LINK"]))

        then: "it has been created"
        pass1Elements*.name ==~ ['Control-1']
        String pass1Element1Id = pass1Elements.first().id
        !pass1Element1Id.isBlank()

        when: "the related item c-3 is incarnated"
        def pass2Elements = postIncarnationDescriptions(getIncarnationDescriptions(testDomainId, ["Control-3"]))

        then: "c-1 has not been re-applied"
        pass2Elements*.name ==~ ['Control-3']

        and: "the new c-3 is linked with the old c-1"
        pass2Elements.find { it.abbreviation == 'c-3' }.links.Control_details_Control[0].target.id == pass1Element1Id
    }

    def "requested items are always created as new elements by default"() {
        when: "c1 is incarnated without the link"
        def pass1Elements = postIncarnationDescriptions(getIncarnationDescriptions(testDomainId, ["Control-1"], ["LINK"]))

        then: "it has been created"
        pass1Elements*.name ==~ ['Control-1']
        String pass1Element1Id = pass1Elements.first().id
        !pass1Element1Id.isBlank()

        when: "c-1 and its related item c-3 are incarnated"
        def pass2Elements = postIncarnationDescriptions(getIncarnationDescriptions(testDomainId, ["Control-1", "Control-3"]))

        then: "both items have been incarnated as new elements"
        pass2Elements*.name ==~ [
            'Control-1',
            'Control-3',
        ]
        pass2Elements.find { it.abbreviation == 'c-1' }.id != pass1Element1Id

        and: "the new c-3 is linked with the new c-1"
        pass2Elements.find { it.abbreviation == 'c-3' }.links.Control_details_Control[0].target.id != pass1Element1Id
    }

    def "existing incarnations can be used for requested items"() {
        when: "c1 is incarnated without the link"
        def pass1Elements = postIncarnationDescriptions(getIncarnationDescriptions(testDomainId, ["Control-1"], ["LINK"]))

        then: "it has been created"
        pass1Elements*.name ==~ ['Control-1']
        String pass1Element1Id = pass1Elements.first().id
        !pass1Element1Id.isBlank()

        when: "c-1 and its related item c-3 are incarnated"
        def pass2Elements = postIncarnationDescriptions(getIncarnationDescriptions(testDomainId, ["Control-1", "Control-3"], [], "DEFAULT", "ALWAYS"))

        then: "only c-3 is returned"
        pass2Elements*.name ==~ [
            'Control-3',
        ]

        and: "there is no new incarnation of c-1"
        get("/controls?abbreviation=c-1&unit=$unitId").body.totalItemCount == 1

        and: "the new c-3 is linked with the old c-1"
        pass2Elements.find { it.abbreviation == 'c-3' }.links.Control_details_Control[0].target.id == pass1Element1Id
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
        def incarnationDescriptions = getIncarnationDescriptions(testDomainId, ["Control-4"], [], "MANUAL")
        incarnationDescriptions.parameters.first().references.first().referencedElement = [
            targetUri: "/controls/$sourceControlId"
        ]
        def elementResults = postIncarnationDescriptions(incarnationDescriptions)
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
        def incarnationDescriptions = getIncarnationDescriptions(testDomainId, ["Control-4"])
        incarnationDescriptions.parameters.first().references.first().referencedElement = [
            targetUri: "/controls/$sourceControlId"
        ]
        def elementResults = postIncarnationDescriptions(incarnationDescriptions, false)

        then: "an error messages is returned"
        elementResults.message == "Element cannot contain custom aspects or links for domains it is not associated with"
    }

    def "Create elements with wrong mode"() {
        when: "a control is created"
        def allItems = getCatalogItems(dsgvoDomainId)*.id.join(',')
        def error = get("/units/${unitId}/incarnations?itemIds=${allItems.join(',')}&mode=TEST", 400).body

        then: "an error messages is returned"
        error.message ==~/No enum constant org.veo.core.usecase.catalogitem.IncarnationRequestModeType.TEST/
    }

    def "Create linked elements from the dsgvo catalog"() {
        when:"we create a process"
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

        and: "incarnate controls, directing their external link references to the existing process"
        def incarnationDescriptions = getIncarnationDescriptions(dsgvoDomainId, [
            "TOM zur Gewährleistung der Vertraulichkeit",
            "TOM zur Gewährleistung der Verfügbarkeit",
            "TOM zur Wiederherstellbarkeit",
            "TOM zur Verschlüsselung",
            "TOM zur Gewährleistung der Integrität",
            "TOM zur Pseudonymisierung",
            "TOM zur Gewährleistung der Belastbarkeit",
            "Verfahren regelmäßiger Überprüfung, Bewertung und Evaluierung der Wirksamkeit der TOM",
        ], [], "MANUAL")
        incarnationDescriptions.parameters.each {
            it.references.first().referencedElement = [
                targetUri: "/processes/$sourceProcessId"
            ]
        }
        postIncarnationDescriptions(incarnationDescriptions)

        then: "the links have been added to the existing process"
        get("/processes/${sourceProcessId}").body.links.size() == 1
        get("/processes/${sourceProcessId}").body.links.process_tom.size() == 8

        when: "incarnating the referenced process catalog item"
        def processVVT = postIncarnationDescriptions(getIncarnationDescriptions(dsgvoDomainId, ["VVT"])).first()

        then:"The process is linked with the controls"
        processVVT.links.size() == 1
        processVVT.links.process_tom.size() == 8
    }

    def "Create all linked elements from the dsgvo catalog in one step"() {
        when:"we create all controls"
        def elementResults = postIncarnationDescriptions(getIncarnationDescriptions(dsgvoDomainId))

        then: "all elements are created"
        elementResults.size() == 65

        and: "all elements have subtype information"
        elementResults.each { element ->
            assert element.domains[dsgvoDomainId].subType != null
            assert element.domains[dsgvoDomainId].status != null
        }
    }

    private getIncarnationDescriptions(String domainId, selectedItemNames = null, Collection<String> exclude = [], String mode = "DEFAULT", String useExistingIncarnations = "FOR_REFERENCED_ITEMS") {
        def itemIds = getCatalogItems(domainId)
                .findAll { selectedItemNames == null || selectedItemNames.contains(it.name) }
                *.id
        return get("/units/$unitId/incarnations?itemIds=${itemIds.join(',')}&mode=$mode&exclude=${exclude.join(',')}&useExistingIncarnations=$useExistingIncarnations").body
    }

    private postIncarnationDescriptions(incarnationDescriptions, expectSuccess = true) {
        log.info("postIncarnationDescriptions before: {}", incarnationDescriptions)
        def response = post("/units/${unitId}/incarnations", incarnationDescriptions, expectSuccess ? 201 : 400).body
        log.info("postIncarnationDescriptions after: {}", response.body)
        return expectSuccess ? response.collect { get(it.targetUri).body } : response
    }
}