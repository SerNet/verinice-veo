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

import org.veo.core.entity.CustomAspect
import org.veo.core.entity.CustomLink
import org.veo.core.entity.Domain
import org.veo.core.entity.Element

import spock.lang.Specification

class EntitySchemaSpec extends Specification {
    ObjectMapper om = new ObjectMapper()
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
                                    properties          : [
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
                links        : [
                    properties: [
                        SuperLink   : [
                            type : "array",
                            items: [
                                type      : "object",
                                properties: [
                                    attributes: [
                                        additionalProperties: false,
                                        properties          : [
                                            fa1: [
                                                type: "string"
                                            ],
                                            fa2: [
                                                type: "boolean"
                                            ]
                                        ]
                                    ],
                                    target    : [
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
                            type : "array",
                            items: [
                                type      : "object",
                                properties: [
                                    attributes: [
                                        additionalProperties: false,
                                        properties          : [
                                        ]
                                    ],
                                    target    : [
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
                        ],
                        SubTypeSensitiveLink: [
                            type : "array",
                            items: [
                                type      : "object",
                                properties: [
                                    attributes: [
                                        additionalProperties: false,
                                        properties          : [
                                        ]
                                    ],
                                    target    : [
                                        properties: [
                                            type: [
                                                enum: [
                                                    "target_type"
                                                ]
                                            ],
                                            subType: [
                                                enum: [
                                                    "sub_type_1",
                                                    "sub_type_2"
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
        sut = new EntitySchema(schema)
    }

    def "validates custom aspect attributes"() {
        given:
        def aspect = Mock(CustomAspect) {
            attributes >> [
                "foo1": "bar",
                "foo2": 5,
            ]
            type >> "SuperAspect"
        }

        when: "validating"
        sut.validateCustomAspect(aspect)

        then:
        noExceptionThrown()
    }

    def "invalid aspect attribute data type causes exception"() {
        given:
        def aspect = Mock(CustomAspect) {
            attributes >> [
                "foo1": "bar",
                "foo2": "5",
            ]
            type >> "SuperAspect"
        }

        when: "validating"
        sut.validateCustomAspect(aspect)

        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "unknown aspect causes exception"() {
        given:
        def aspect = Mock(CustomAspect) {
            attributes >> [
                "foo1": "bar",
                "foo2": 5,
            ]
            type >> "MissingAspect"
        }

        when: "validating"
        sut.validateCustomAspect(aspect)

        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "unknown aspect attribute type causes exception"() {
        given:
        def aspect = Mock(CustomAspect) {
            attributes >> [
                "fantasyFoo": "baritone",
            ]
            type >> "SuperAspect"
        }

        when: "validating"
        sut.validateCustomAspect(aspect)

        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "invalid link attribute data type causes exception"() {
        given:
        def link = Mock(CustomLink) {
            attributes >> [
                "fa1": "na",
                "fa2": "true",
            ]
            type >> "SuperLink"
        }
        when: "validating"
        sut.validateCustomLink(link)

        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "validates link"() {
        given:
        def link = Mock(CustomLink) {
            attributes >> [
                "fa1": "na",
                "fa2": true,
            ]
            target >> Mock(Element) {
                modelType >> "target_type"
            }
            type >> "SuperLink"
        }
        when: "validating"
        sut.validateCustomLink(link)

        then:
        noExceptionThrown()
    }

    def "unknown link type causes exception"() {
        given:
        def link = Mock(CustomLink) {
            attributes >> [
                "fa1": "na",
                "fa2": true,
            ]
            target >> Mock(Element) {
                modelType >> "target_type"
            }
            type >> "MissingLink"
        }
        when: "validating with unknown link type"
        sut.validateCustomLink(link)

        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "unknown link attribute type causes exception"() {
        given:
        def link = Mock(CustomLink) {
            attributes >> [
                "fantasyFa:": "na-na-na",
            ]
            target >> Mock(Element) {
                modelType >> "target_type"
            }
            type >> "SuperLink"
        }
        when: "validating"
        sut.validateCustomLink(link)

        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "invalid link targets cause exception"() {
        given: "a link with invalid target modelType"
        def link = Mock(CustomLink) {
            attributes >> [
                "fa1": "na",
                "fa2": true,
            ]
            type >> "SuperLink"
            target >> Mock(Element) {
                modelType >> "no_target_type"
            }
        }

        when: "validated"
        sut.validateCustomLink(link)

        then: "an exception is thrown"
        thrown(IllegalArgumentException)
    }

    def "another valid link is allowed"() {
        given: "a link with valid target modelType"
        def link = Mock(CustomLink) {
            attributes >> [:]
            type >> "UltimateLink"
            target >> Mock(Element) {
                modelType >> "another_target_type"
            }
        }

        when: "validated"
        sut.validateCustomLink(link)

        then:
        noExceptionThrown()
    }


    def "link target with valid sub type is allowed"() {
        given: "a link with valid target sub type"
        def domain1 = Mock(Domain)
        def domain2 = Mock(Domain)
        def link = Mock(CustomLink) {
            attributes >> [:]
            type >> "SubTypeSensitiveLink"
            target >> Mock(Element) {
                domains >> [domain1, domain2]
                modelType >> "target_type"
                getSubType(domain1) >> Optional.of("wrong_type")
                getSubType(domain2) >> Optional.of("sub_type_2")
            }
        }

        when: "validated"
        sut.validateCustomLink(link)

        then:
        noExceptionThrown()
    }

    def "link target with invalid sub types is not allowed"() {
        given: "a link with invalid target sub type"
        def domain = Mock(Domain)
        def link = Mock(CustomLink) {
            attributes >> [:]
            type >> "SubTypeSensitiveLink"
            target >> Mock(Element) {
                domains >> [domain]
                modelType >> "target_type"
                getSubType(domain) >> Optional.of("wrong_type")
            }
        }

        when: "validated"
        sut.validateCustomLink(link)

        then:
        thrown(IllegalArgumentException)
    }

    def "link target without sub type is not allowed"() {
        given: "a link with no target sub type"
        def domain = Mock(Domain)
        def link = Mock(CustomLink) {
            attributes >> [:]
            type >> "SubTypeSensitiveLink"
            target >> Mock(Element) {
                domains >> [domain]
                modelType >> "target_type"
                getSubType(domain) >> Optional.empty()
            }
        }

        when: "validated"
        sut.validateCustomLink(link)

        then:
        thrown(IllegalArgumentException)
    }
}
