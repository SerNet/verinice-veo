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
                        mostCommonWord: [
                            type: "text"
                        ],
                        numberOfWords: [
                            type: "integer"
                        ]
                    ]
                ]
            ]
        ], null, 204, CONTENT_CREATOR)
    }

    def "CRUD valid decision"() {
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

        when: "deleting the decision"
        delete(decisionUri)

        then: "it is gone"
        with(get("/domains/$domainId").body) {
            it.decisions.TLDR == null
        }
    }

    def "invalid element type is detected"() {
        when: "trying to create a decision with an invalid element type"
        def response = put("/content-creation/domains/$domainId/decisions/elephantInTheRoom", [
            name: [en: "I like elephants"],
            elementType: "elephant",
            elementSubType: "AfricanElephant",
            rules: [],
            defaultResultValue: true
        ], null, 422, CONTENT_CREATOR)

        then: "there is an error"
        response.body.message == "Validation error in decision 'elephantInTheRoom': Domain has no definition for entity type elephant"
    }

    def "invalid element sub type is detected"() {
        when: "trying to create a decision with an invalid element type"
        def response = put("/content-creation/domains/$domainId/decisions/toDo", [
            name: [en: "I like lists"],
            elementType: "document",
            elementSubType: "ToDoList",
            rules: [],
            defaultResultValue: true
        ], null, 422, CONTENT_CREATOR)

        then: "there is an error"
        response.body.message == "Validation error in decision 'toDo': Sub type ToDoList is not defined, availabe sub types: [Article]"
    }

    def "missing custom aspect is detected"() {
        when: "trying to create a decision referencing an absent custom aspect"
        def response = put("/content-creation/domains/$domainId/decisions/numPresent", [
            name: [en: "number of words counted"],
            elementType: "document",
            elementSubType: "Article",
            rules: [
                [
                    conditions: [
                        [
                            inputProvider: [
                                type: "customAspectAttributeValue",
                                customAspect: "absent",
                                attribute: "numberOfWords"
                            ],
                            inputMatcher: [
                                type: "isNull"
                            ],
                        ]
                    ],
                    output: false
                ]
            ],
            defaultResultValue: true
        ], null, 422, CONTENT_CREATOR)

        then: "there is an error"
        response.body.message == "Validation error in decision 'numPresent': Custom aspect 'absent' is not defined"
    }

    def "missing custom aspect attribute is detected"() {
        when: "trying to create a decision with an invalid attribute"
        def response = put("/content-creation/domains/$domainId/decisions/numPresent", [
            name: [en: "number of words counted"],
            elementType: "document",
            elementSubType: "Article",
            rules: [
                [
                    conditions: [
                        [
                            inputProvider: [
                                type: "customAspectAttributeValue",
                                customAspect: "metrics",
                                attribute: "absent"
                            ],
                            inputMatcher: [
                                type: "isNull"
                            ],
                        ]
                    ],
                    output: false
                ]
            ],
            defaultResultValue: true
        ], null, 422, CONTENT_CREATOR)

        then: "there is an error"
        response.body.message == "Validation error in decision 'numPresent': Attribute 'absent' is not defined"
    }

    def "comparison between different types is detected"() {
        when: "trying to create a decision that compares a number with a boolean"
        def response = put("/content-creation/domains/$domainId/decisions/unfairComparison", [
            name: [en: "apples & oranges"],
            elementType: "document",
            elementSubType: "Article",
            rules: [
                [
                    conditions: [
                        [
                            inputProvider: [
                                type: "maxRisk"
                            ],
                            inputMatcher: [
                                type: "equals",
                                comparisonValue: true
                            ],
                        ]
                    ],
                    output: true
                ]
            ],
            defaultResultValue: false
        ], null, 422, CONTENT_CREATOR)

        then: "there is an error"
        response.body.message == "Validation error in decision 'unfairComparison': Provider yields BigDecimal, but matcher only supports [Boolean]"
    }

    def "greater-than comparison between text and number is detected"() {
        when: "trying to create a decision that compares a text with a number"
        def response = put("/content-creation/domains/$domainId/decisions/unfairComparison", [
            name: [en: "apples & oranges"],
            elementType: "document",
            elementSubType: "Article",
            rules: [
                [
                    conditions: [
                        [
                            inputProvider: [
                                type: "customAspectAttributeValue",
                                customAspect: "metrics",
                                attribute: "mostCommonWord"
                            ],
                            inputMatcher: [
                                type: "greaterThan",
                                comparisonValue: 5
                            ],
                        ]
                    ],
                    output: false
                ]
            ],
            defaultResultValue: true
        ], null, 422, CONTENT_CREATOR)

        then: "there is an error"
        response.body.message == "Validation error in decision 'unfairComparison': Provider yields String, but matcher only supports [BigDecimal,Integer,Long]"
    }

    def "size check on non-list attribute is detected"() {
        when: "trying to create a decision that gets the size of a number attribute"
        def response = put("/content-creation/domains/$domainId/decisions/sillySize", [
            name: [en: "implausible size check"],
            elementType: "document",
            elementSubType: "Article",
            rules: [
                [
                    conditions: [
                        [
                            inputProvider: [
                                type: "customAspectAttributeSize",
                                customAspectType: "metrics",
                                attributeType: "numberOfWords",
                            ],
                            inputMatcher: [
                                type: "greaterThan",
                                comparisonValue: 1
                            ],
                        ]
                    ],
                    output: true
                ]
            ],
            defaultResultValue: false
        ], null, 422, CONTENT_CREATOR)

        then: "there is an error"
        response.body.message == "Validation error in decision 'sillySize': Cannot get size of Integer attribute 'numberOfWords', expected list attribute"
    }
}
