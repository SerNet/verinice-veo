/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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

class DeletionRestTest extends VeoRestTest {
    String unitUri

    def setup() {
        unitUri = "$baseUrl/units/" + postNewUnit().resourceId
    }

    def "link is removed when target element is deleted"() {
        given:
        def assetId = post("/domains/$dsgvoDomainId/assets", [
            name: "data type",
            subType: "AST_Datatype",
            status: "NEW",
            owner: [targetUri: unitUri]
        ]).body.resourceId
        def processId = post("/domains/$dsgvoDomainId/processes", [
            name: "source process",
            subType: "PRO_DataProcessing",
            status: "NEW",
            links: [
                process_dataType: [
                    [
                        target: [
                            targetUri: "$baseUrl/assets/$assetId"
                        ]]
                ]
            ],
            owner: [targetUri: unitUri]
        ]).body.resourceId
        def processETag = get("/processes/$processId").getETag()

        when: "deleting the target"
        delete("/assets/$assetId")

        then: "the link has been removed and the ETag has changed"
        with(get("/processes/$processId")) {
            body.links.size() == 0
            getETag() != processETag
        }
    }

    def "elements are removed when a unit is deleted"() {
        given: "a document and scenario in the unit"
        def documentId = post("/domains/$dsgvoDomainId/documents", [
            name: "document that must be deleted",
            subType: 'DOC_Document',
            status: 'NEW',
            owner: [targetUri: unitUri]
        ]).body.resourceId
        def scenarioId = post("/domains/$dsgvoDomainId/scenarios", [
            name: "scenario that must be deleted",
            subType: "SCN_Scenario",
            status: "NEW",
            owner: [targetUri: unitUri]
        ]).body.resourceId

        when: "deleting the unit"
        delete(unitUri)

        then: "the unit and its elements are gone"
        get(unitUri, 404)
        get("/documents/$documentId", 404)
        get("/scenarios/$scenarioId", 404)
    }
}
