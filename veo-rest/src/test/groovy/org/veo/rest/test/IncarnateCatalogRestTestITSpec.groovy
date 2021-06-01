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
 * Tests creation of elements from the catalog 'dsgvo-example.json'.
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
 *
 * CC-1 and CC-2 cannot be created currently - see the issue linked below.
 *
 */
class IncarnateCatalogRestTestITSpec extends VeoRestTest {

    public static final String UNIT_NAME = 'Testunit'

    def postResponse
    String unitId

    def "Create linked elements from a catalog"() {
        log.info("Create a unit and get ApplyCatalogItems info for Items")

        given:
        postResponse = postNewUnit(UNIT_NAME)
        unitId = postResponse.resourceId

        when: "the catalog is retrieved"
        def catalogId = extractLastId(getDomains().catalogs.first().targetUri)
        def catalog = getCatalog(catalogId)

        then: "the expected catalog was instantiated"
        with(catalog) {
            catalogItems.size() == 6
            name == "DSGVO-Controls"
            domainTemplate.displayName == "DSGVO-test DSGVO-test"
        }

        when: "a selection of catalog items is applied"
        def elementResults
        log.info("===============> first pass")
        elementResults = applyCatalogItems(catalog, ["Control-1", "Control-2"], null)

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
        elementResults = applyCatalogItems(catalog)

        then: "all elements were created"
        // This time, C-3 was created as well. C-3 was created with a link to
        // the instance of C-1 created in the first pass.
        elementResults.size() == 4
        elementResults.collect { it.name }.sort() == [
            'Control-1',
            'Control-2',
            'Control-3',
            'Control-4'
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

        // FIXME VEO-726 Note that CC-1 and CC-2 cannot be created because circular references cannot be resolved yet
        // The two-pass approach will no longer be necessary with VEO-726 as well.
    }

    def "Create elements with reversed links from catalog"() {
        given:
        def postResponse = postNewUnit(UNIT_NAME)
        unitId = postResponse.resourceId

        when: "the catalog is retrieved"
        def catalogId = extractLastId(getDomains().catalogs.first().targetUri)
        def catalog = getCatalog(catalogId)

        then: "the expected catalog was instantiated"
        with(catalog) {
            catalogItems.size() == 6
            name == "DSGVO-Controls"
            domainTemplate.displayName == "DSGVO-test DSGVO-test"
        }

        when: "a control is created"
        def sourceControlId = post("/controls", [
            name : "Link Target Control",
            owner: [targetUri: "/units/$unitId"],
        ]).body.resourceId

        and: "C-4 is instantiated"
        def elementResults = applyCatalogItems(catalog, ["Control-4"], "/controls/$sourceControlId")
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

    private applyCatalogItems(catalog) {
        return applyCatalogItems(catalog, null, null)
    }



    private applyCatalogItems(catalog, selectedItems, sourceElementUri) {
        def elementResults = []
        catalog.catalogItems
                .sort { it.displayName }
                .reverse()
                .findAll { selectedItems == null || isSelectedItem(it.displayName, selectedItems) }
                .each {
                    log.info("Read catalog item: {}", it.displayName)

                    when: "list an item"
                    def itemId = extractLastId(it.targetUri)

                    and: "get the apply information"
                    def applyInfo = getIncarnationDescriptions(unitId, itemId)
                    log.info("Catalogitem {} has {} references", it.displayName, applyInfo.parameters.first().references.size())

                    // change apply info:
                    if (sourceElementUri != null) {
                        applyInfo.parameters.first().references.first().put("referencedCatalogable", [
                            "targetUri": "$sourceElementUri"
                        ])
                    }

                    and: "create only items without references to other catalog items"
                    def beforeCreation = Instant.now()
                    // skip items that have references pointing towards other elements when
                    // those elements have not been created yet (see the fix-me regarding VEO-726):
                    if (applyInfo.parameters.first().references.size() == 0 // no reference present
                            || applyInfo.parameters.first().references[0].referencedCatalogable != null // reference to an element previously created from catalog
                            ) {
                        log.info("Will be applied: {}", it.displayName)

                        def postApply = postIncarnationDescriptions(unitId, applyInfo)

                        and: "get the created element"
                        def elementResult = get(postApply.first().targetUri).body
                        log.info("Incarnated element {}", elementResult)
                        elementResults.add(elementResult)

                        assert it.displayName ==~ /.*$elementResult.name.*/
                        assert Instant.parse(elementResult.createdAt) > beforeCreation
                        assert Instant.parse(elementResult.updatedAt) > beforeCreation
                        assert !elementResult.description.isBlank()
                        assert elementResult.domains[0].displayName == 'DSGVO-test DSGVO-test'
                        assert !elementResult.id.isBlank()
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

    private postIncarnationDescriptions(unitId, applyInfo) {
        def response = post("/units/${unitId}/incarnations", applyInfo)
        log.info("postIncarnationDescriptions: {}", response.body)
        response.body
    }

    private boolean isSelectedItem(String name, List<String> items) {
        items.find {name.contains(it) } != null
    }
}