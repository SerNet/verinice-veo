/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

class ProcessRestTestITSpec extends VeoRestTest{
    String unitId

    def setup() {
        unitId = post("/units", [
            name: "process test unit"
        ]).body.resourceId
    }

    def "Create, retrieve, update & delete process"() {
        given: "a target asset"
        def assetId = post("/assets", [
            name: "target asset for process",
            owner: [targetUri: "/units/$unitId"],
        ]).body.resourceId

        when: "creating a process"
        def processId = post("/processes", [
            customAspects: [
                process_dataProcessing: [
                    attributes: [
                        process_dataProcessing_legalBasis: [
                            "process_dataProcessing_legalBasis_Art6Abs1liteDSGVO",
                            "process_dataProcessing_legalBasis_Art6Abs1litbDSGVO"
                        ]
                    ]
                ]
            ],
            links: [
                process_dataType: [
                    [
                        target: [
                            targetUri: "/assets/$assetId"
                        ]
                    ]
                ]
            ],
            name: "process",
            owner: [targetUri: "/units/$unitId"]
        ]).body.resourceId

        then: "it can be retrieved"
        def retrievalResponse = get("/processes/$processId")
        with(retrievalResponse.body) {
            customAspects.process_dataProcessing.attributes.process_dataProcessing_legalBasis == [
                "process_dataProcessing_legalBasis_Art6Abs1liteDSGVO",
                "process_dataProcessing_legalBasis_Art6Abs1litbDSGVO"
            ]
            id == processId
            links.process_dataType[0].target.targetUri.endsWith("/assets/$assetId")
            name == "process"
        }

        and: "it is retrieved when requesting all processes in the unit"
        get("/processes?unit=$unitId&size=2147483647").body.items*.id.contains(processId)

        when: "updating and retrieving the process"
        def updatedProcess = retrievalResponse.body
        updatedProcess.name = "new name"
        put("/processes/$processId", updatedProcess, retrievalResponse.headers.ETag.toString())

        then: "the updated process can be retrieved"
        def newRetrievalResponse = get("/processes/$processId")
        with(newRetrievalResponse.body) {
            name == "new name"
        }

        expect: "update to fail with outdated ETag"
        put("/processes/$processId", updatedProcess, retrievalResponse.headers.ETag.toString(), 412)

        when: "deleting the process"
        delete("/processes/$processId")

        then: "it can't be retrieved"
        get("/proceses/$processId", 404)
    }
}
