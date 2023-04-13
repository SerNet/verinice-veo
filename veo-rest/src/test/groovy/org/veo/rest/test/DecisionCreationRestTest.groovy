/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import static java.util.UUID.randomUUID
import static org.veo.rest.test.UserType.CONTENT_CREATOR

class DecisionCreationRestTest extends VeoRestTest {
    String domainId

    def setup() {
        postNewUnit("init")
        domainId = post("/content-creation/domains", [
            name: "decision creation rest test ${randomUUID()}",
            authority: "me",
        ]).body.resourceId
        put("/content-creation/domains/$domainId/element-type-definitions/document", [
            subTypes: [
                Article: [statuses: ["Online"]]
            ],
            customAspects: [
                metrics: [
                    attributeDefinitions: [
                        numberOfWords: [
                            type: "integer"
                        ]
                    ]
                ]
            ]
        ], null, 204, CONTENT_CREATOR)
    }

    def "CRU valid decision"() {
        given: "a transient valid decision"
        def decisionUri = "/content-creation/domains/$domainId/decisions/TLDR"
        def decision = [
            name: [en: "Article is so long that nobody will ever read it"],
            elementType: "document",
            elementSubType: "Article",
            rules: [
                [
                    conditions: [
                        [
                            inputProvider: [
                                type: "customAspectAttributeValue",
                                customAspect: "metrics",
                                attribute: "numberOfWords"
                            ],
                            inputMatcher: [
                                type: "greaterThan",
                                comparisonValue: 9000
                            ],
                        ]
                    ],
                    output: true
                ]
            ],
            defaultResultValue: false
        ]

        when: "creating it in the domain"
        put(decisionUri, decision, null, 201, CONTENT_CREATOR)

        then: "it exists in the domain"
        with(get("/domains/$domainId").body) {
            decisions.TLDR.elementType == "document"
        }

        when: "updating the decision"
        decision.rules[0].conditions[0].inputMatcher.comparisonValue = 10000
        put(decisionUri, decision)

        then: "the change has been applied"
        with(get("/domains/$domainId").body) {
            it.decisions.TLDR.rules[0].conditions[0].inputMatcher.comparisonValue == 10000
        }
    }
}
