/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.adapter.presenter.api.response.transformer

import com.fasterxml.jackson.databind.ObjectMapper

import org.veo.core.entity.CustomLink
import org.veo.core.entity.CustomProperties

import spock.lang.Specification

class CustomAttributesTransformerSpec extends Specification{
    ObjectMapper om = new ObjectMapper()
    AttributeTransformer transformer = Mock()

    CustomAttributesTransformer sut

    def setup() {
        def schema = om.valueToTree([
            properties: [
                customAspects: [
                    properties: [
                        SuperAspect: [
                            properties: [
                                attributes: [
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
                                        properties: [
                                            fa1: [
                                                type: "string"
                                            ],
                                            fa2: [
                                                type: "boolean"
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
        sut = new CustomAttributesTransformer(schema, transformer)
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
}
