/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.adapter.presenter.api.response.transformer

import com.fasterxml.jackson.databind.ObjectMapper

import org.veo.core.entity.CustomLink
import org.veo.core.entity.CustomProperties
import org.veo.core.entity.EntityLayerSupertype

import spock.lang.Specification

class EntitySchemaSpec extends Specification{
    ObjectMapper om = new ObjectMapper()
    AttributeTransformer transformer = Mock()

    EntitySchema sut

    def setup() {
        def schema = om.valueToTree([
            properties: [
                customAspects: [
                    properties: [
                        SuperAspect: [
                            properties: [
                                attributes: [
                                    additionalProperties: false,
                                    properties: [
                                        foo1: [
                                            type: "string"
                                        ],
                                        foo2: [
                                            type: "number"
                                        ]
                                    ]
                                ]
                            ]
                        ]
                    ]
                ],
                links: [
                    properties: [
                        SuperLink: [
                            type : "array" ,
                            items : [
                                type : "object" ,
                                properties : [
                                    attributes: [
                                        additionalProperties: false,
                                        properties: [
                                            fa1: [
                                                type: "string"
                                            ],
                                            fa2: [
                                                type: "boolean"
                                            ]
                                        ]
                                    ],
                                    target: [
                                        properties: [
                                            type: [
                                                enum: [
                                                    "target_type"
                                                ]
                                            ]
                                        ]
                                    ]
                                ]
                            ]
                        ],
                        UltimateLink: [
                            type : "array" ,
                            items : [
                                type : "object" ,
                                properties : [
                                    attributes: [
                                        additionalProperties: false,
                                        properties: [
                                        ]
                                    ],
                                    target: [
                                        properties: [
                                            type: [
                                                enum: [
                                                    "target_type",
                                                    "another_target_type",
                                                    "yet_another_target_type"
                                                ]
                                            ]
                                        ]
                                    ]
                                ]
                            ]
                        ]
                    ]
                ]
            ]
        ])
        sut = new EntitySchema(schema, transformer)
    }

    def "applies custom aspect attributes"() {
        given:
        def input = [
            foo1: "bar",
            foo2: 5
        ]
        def target = Mock(CustomProperties) {
            it.type >> "SuperAspect"
        }

        when: "applying the input props"
        sut.applyAspectAttributes(input, target)

        then: "the transformer is called"
        1 * transformer.applyToEntity("foo1", "bar", om.valueToTree([type: "string"]), target)
        1 * transformer.applyToEntity("foo2", 5, om.valueToTree([type: "number"]), target)
    }

    def "unknown aspect causes exception"() {
        given:
        def input = [
            foo1: "bar",
            foo2: 5
        ]
        def target = Mock(CustomProperties) {
            it.type >> "MissingAspect"
        }

        when: "applying the input props"
        sut.applyAspectAttributes(input, target)

        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "unknown aspect attribute type causes exception"() {
        given:
        def input = [
            fantasyFoo: "baritone"
        ]
        def target = Mock(CustomProperties) {
            it.type >> "SuperAspect"
        }

        when: "applying the input props"
        sut.applyAspectAttributes(input, target)

        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "applies custom link attributes"() {
        given:
        def input = [
            fa1: "na",
            fa2: true
        ]
        def target = Mock(CustomLink) {
            it.type >> "SuperLink"
        }

        when: "applying the input props"
        sut.applyLinkAttributes(input, target)

        then: "the transformer is called"
        1 * transformer.applyToEntity("fa1", "na", om.valueToTree([type: "string"]), target)
        1 * transformer.applyToEntity("fa2", true, om.valueToTree([type: "boolean"]), target)
    }

    def "unknown link type causes exception"() {
        given:
        def input = [
            fa1: "na",
            fa2: true
        ]
        def target = Mock(CustomLink) {
            it.type >> "MissingLink"
        }

        when: "applying the input props"
        sut.applyLinkAttributes(input, target)

        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "unknown link attribute type causes exception"() {
        given:
        def input = [
            fantasyFa: "na-na-na"
        ]
        def target = Mock(CustomLink) {
            it.type >> "SuperLink"
        }

        when: "applying the input props"
        sut.applyLinkAttributes(input, target)

        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "invalid links cause exception"() {
        given: "a link with invalid target modelType"
        def link = Mock(CustomLink) {
            it.type >> "SuperLink"
            it.target >> Mock(EntityLayerSupertype) {
                it.modelType >> "no_target_type"
            }
        }

        when: "validated"
        sut.validateLinkTarget(link)

        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "valid links are allowed"() {
        given: "a link with valid target modelType"
        def link = Mock(CustomLink) {
            it.type >> "SuperLink"
            it.target >> Mock(EntityLayerSupertype) {
                it.modelType >> "target_type"
            }
        }

        when: "validated"
        sut.validateLinkTarget(link)

        then: "throws no illegal argument exceptions"
        notThrown(IllegalArgumentException)
    }

    def "mutiple valid links are allowed"() {
        given: "a link with valid target modelType"
        def link = Mock(CustomLink) {
            it.type >> "UltimateLink"
            it.target >> Mock(EntityLayerSupertype) {
                it.modelType >> "another_target_type"
            }
        }

        when: "validated"
        sut.validateLinkTarget(link)

        then: "throws no illegal argument exceptions"
        notThrown(IllegalArgumentException)
    }
}
