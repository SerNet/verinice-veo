/*
 * verinice.veo
 * Copyright (C) 2024  Jochen Kemnade
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
 */
package org.veo.core.entity.condition

import org.veo.core.entity.Asset
import org.veo.core.entity.Control
import org.veo.core.entity.ControlImplementationConfiguration
import org.veo.core.entity.CustomAspect
import org.veo.core.entity.CustomLink
import org.veo.core.entity.Document
import org.veo.core.entity.Domain
import org.veo.core.entity.Element
import org.veo.core.entity.ElementType
import org.veo.core.entity.Incident
import org.veo.core.entity.LinkDirection
import org.veo.core.entity.Person
import org.veo.core.entity.Process
import org.veo.core.entity.Scope
import org.veo.core.entity.compliance.ControlImplementation
import org.veo.core.entity.definitions.ElementTypeDefinition
import org.veo.core.entity.definitions.LinkDefinition
import org.veo.core.entity.definitions.SubTypeDefinition
import org.veo.core.entity.definitions.attribute.BooleanAttributeDefinition
import org.veo.core.entity.definitions.attribute.DurationAttributeDefinition
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition
import org.veo.core.entity.definitions.attribute.ListAttributeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.core.entity.event.ElementEvent
import org.veo.core.entity.event.InboundLinkEvent
import org.veo.core.entity.event.RiskAffectingElementChangeEvent
import org.veo.core.entity.event.RiskChangedEvent
import org.veo.core.entity.event.RiskEvent
import org.veo.core.entity.risk.CategoryRef
import org.veo.core.entity.risk.ImpactRef
import org.veo.core.entity.risk.ImpactValues
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.core.entity.riskdefinition.CategoryDefinition
import org.veo.core.entity.riskdefinition.CategoryLevel
import org.veo.core.entity.riskdefinition.RiskDefinition
import org.veo.core.entity.type.VeoType as T

import spock.lang.Specification

class VeoExpressionITSpec extends Specification {

    def "custom attribute size can be evaluated"() {
        given:
        Domain domain = Mock() {
            getCustomAspectAttributeDefinition(ElementType.DOCUMENT, "myType", "fattribute") >> new ListAttributeDefinition(
                    new TextAttributeDefinition()
            )
            getCustomAspectAttributeDefinition(ElementType.DOCUMENT, "myType", "intribute") >> new IntegerAttributeDefinition()
        }
        Document document = Spy {
            getCustomAspects(domain) >> [
                Mock(CustomAspect) {
                    type >> "myType"
                    attributes >> [
                        fattribute: ["a", "b", "c"]
                    ]
                }
            ]
        }
        def validExp = new CustomAspectAttributeSizeExpression("myType", "fattribute")

        when:
        validExp.selfValidate(domain, ElementType.DOCUMENT)

        then:
        noExceptionThrown()

        expect:
        validExp.getValueType(domain, ElementType.DOCUMENT) == T.integer()
        validExp.getValue(document, domain) == 3

        when:
        new CustomAspectAttributeSizeExpression("myType", "intribute").selfValidate(domain, ElementType.DOCUMENT)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Cannot determine size for attribute 'intribute': expected List<*|Null>|Null, got Integer"
    }

    def "link targets can be evaluated"() {
        given:
        Domain domain = Mock()
        Element element = Mock()

        Incident targetA = Spy(Incident)
        Incident targetB = Spy(Incident)
        Scope targetC = Spy(Scope)
        Scope targetD = Spy(Scope)
        Document sourceA = Spy {
            getLinks(domain) >> [
                Mock(CustomLink) {
                    type >> "myLink"
                    loadTarget() >> targetA
                },
                Mock(CustomLink) {
                    type >> "myLink"
                    loadTarget() >> targetB
                },
            ]
        }
        Person sourceB = Spy {
            getLinks(domain) >> [
                Mock(CustomLink) {
                    type >> "myLink"
                    loadTarget() >> targetC
                },
                Mock(CustomLink) {
                    type >> "notMyLink"
                    loadTarget() >> targetD
                },
            ]
        }
        def sources = Mock(VeoExpression) {
            getValue(element, domain) >> [sourceA, sourceB]
        }
        def exp = new LinkTargetsExpression(sources, "myLink")

        expect:
        exp.getValue(element, domain) ==~ [targetA, targetB, targetC]
    }

    def "link target expression is validated"() {
        given:
        Domain domain = Mock() {
            getElementTypeDefinition(ElementType.DOCUMENT) >> Mock(ElementTypeDefinition) {
                getLinks() >> [
                    myLink: Mock(LinkDefinition) {
                        targetType >> ElementType.INCIDENT
                    },
                    notMyLink: Mock(LinkDefinition) {
                        targetType >> ElementType.PROCESS
                    },
                ]
            }
            getElementTypeDefinition(ElementType.PERSON) >> Mock(ElementTypeDefinition) {
                getLinks() >> [
                    myLink: Mock(LinkDefinition) {
                        targetType >> ElementType.SCOPE
                    },
                ]
            }
        }
        def sources = Mock(VeoExpression) {
            getValueType(domain, ElementType.ASSET) >> T.listOf(
                    T.sumOf(T.element(ElementType.DOCUMENT), T.element(ElementType.PERSON))
            ).orNothing()
        }
        def exp = new LinkTargetsExpression(sources, null)

        when:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        noExceptionThrown()

        expect:
        exp.getValueType(domain, ElementType.ASSET) == T.listOf(T.sumOf(
                        T.element(ElementType.INCIDENT),
                        T.element(ElementType.PROCESS),
                        T.element(ElementType.SCOPE),
                ))

        when: "filtering by a certain type"
        exp = new LinkTargetsExpression(sources, "myLink")
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        noExceptionThrown()

        expect:
        exp.getValueType(domain, ElementType.ASSET) == T.listOf(T.sumOf(
                        T.element(ElementType.INCIDENT),
                        T.element(ElementType.SCOPE),
                ))

        when: "filtering by a missing type"
        exp = new LinkTargetsExpression(sources, "fantasyLink")
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "No links defined for element type Document|Person and link type 'fantasyLink'"
    }

    def "use #value as constant"() {
        given:
        def domain = Mock(Domain)
        def element = Mock(Element)
        def exp = new ConstantExpression(value)

        expect:
        exp.selfValidate(domain, ElementType.INCIDENT)
        exp.getValueType(domain, ElementType.INCIDENT).toString() == expectedType
        exp.getValue(element, domain) == value

        where:
        value                    | expectedType
        null                     | "Null"
        1                        | "Integer"
        "ok"                     | "String"
        true                     | "Boolean"
        ["tar", null]            | "List<String|Null>"
        [foo: "bar"]             | "Map<String,String>"
        [foo: "bar", boo: false] | "Map<String,Boolean|String>"
    }

    def "Remove #value from #list returns #result"() {
        given:
        Domain domain = Mock()
        Element element = Mock()
        CustomAspectAttributeValueExpression from = Stub{
            getValue(element, domain) >> list
        }
        ConstantExpression valueExpression = Stub{
            getValue(element, domain) >> value
        }
        RemoveExpression expression = new RemoveExpression(from, valueExpression)

        expect:
        expression.getValue(element, domain) == result

        where:
        list | value | result
        null | 3 | null
        [] | 3 | []
        [1, 2, 3] | 3 | [1, 2]
        [1, 2, 3] | null | [1, 2, 3]
        ['f', 'o', 'o'] | 'o' | ['f']
    }

    def "Map a list using a map"() {
        given:
        Domain domain = Mock()
        Element element = Mock()
        CustomAspectAttributeValueExpression source = Stub{
            getValue(element, domain) >> list
        }
        ConstantExpression mapping = Stub{
            getValue(element, domain) >> map
        }
        MapExpression expression = new MapExpression(source, mapping)

        expect:
        expression.getValue(element, domain) == result

        where:
        list | map | result
        null | [:] | null
        [] | [:] | []
        [1, 2, 3] | [1: 2, 3: 2] | [2, null, 2]
        ['foo', 'bar'] | [bar: 'baz'] | [null, 'baz']
    }

    def "remove expression is validated"() {
        given:
        def domain = Mock(Domain) {
            getCustomAspectAttributeDefinition(ElementType.ASSET, "aspect1", "attr1") >> new ListAttributeDefinition(new TextAttributeDefinition())
        }
        def exp = new RemoveExpression(
                new CustomAspectAttributeValueExpression("aspect1", "attr1"),
                new ConstantExpression(5)
        )

        when:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        IllegalArgumentException illEx = thrown()
        illEx.message == "source ('from') cannot contain value: String|Null does not intersect with Integer"
    }

    def "remove expression is valid for #haystackType and #needleType"() {
        given:
        def domain = Mock(Domain) {}
        def exp = new RemoveExpression(
                Mock(CustomAspectAttributeValueExpression) {
                    getValueType(domain, ElementType.ASSET) >> haystackType
                },
                Mock(ConstantExpression) {
                    getValueType(domain, ElementType.ASSET) >> needleType
                },
        )

        when:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        noExceptionThrown()
        exp.getValueType(domain, ElementType.ASSET) == haystackType

        where:
        haystackType                                  | needleType
        T.nothing()                                   | T.string()
        T.listOf(T.integer())                         | T.integer()
        T.listOf(T.integer().orNothing())             | T.nothing()
        T.listOf(T.integer().orNothing())             | T.integer()
        T.listOf(T.integer().orNothing()).orNothing() | T.integer()
        T.listOf(T.sumOf(T.integer(), T.bool()))      | T.integer()
        T.listOf(T.sumOf(T.integer(), T.bool()))      | T.bool()
        T.listOf(T.sumOf(T.integer(), T.bool()))      | T.sumOf(T.bool(), T.string())
        T.listOf(T.integer())                         | T.integer().orNothing()
    }

    def "remove expression is invalid for #haystackType and #needleType"() {
        given:
        def domain = Mock(Domain) {}
        def exp = new RemoveExpression(
                Mock(CustomAspectAttributeValueExpression) {
                    getValueType(domain, ElementType.ASSET) >> haystackType
                },
                Mock(ConstantExpression) {
                    getValueType(domain, ElementType.ASSET) >> needleType
                },
        )

        when:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        IllegalArgumentException illEx = thrown()
        illEx.message == expectedError

        where:
        haystackType                      | needleType  | expectedError
        T.integer()                       | T.integer() | "invalid source ('from') for removal: expected List<*|Null>|Null, got Integer"
        T.mapOf(T.string(), T.string())   | T.integer() | "invalid source ('from') for removal: expected List<*|Null>|Null, got Map<String,String>"
        T.listOf(T.integer())             | T.string()  | "source ('from') cannot contain value: Integer does not intersect with String"
        T.listOf(T.integer().orNothing()) | T.string()  | "source ('from') cannot contain value: Integer|Null does not intersect with String"
        T.listOf(T.integer())             | T.nothing() | "source ('from') cannot contain value: Integer does not intersect with Null"
    }

    def "contains expression is valid for #haystackType and #needleType"() {
        given:
        def domain = Mock(Domain) {}
        def exp = new ContainsExpression(
                Mock(CustomAspectAttributeValueExpression) {
                    getValueType(domain, ElementType.ASSET) >> haystackType
                },
                Mock(ConstantExpression) {
                    getValueType(domain, ElementType.ASSET) >> needleType
                },
        )

        when:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        noExceptionThrown()
        exp.getValueType(domain, ElementType.ASSET) == T.bool()

        where:
        haystackType                                       | needleType
        T.nothing()                                        | T.string()
        T.listOf(T.integer())                              | T.integer()
        T.listOf(T.integer())                              | T.integer().orNothing()
        T.listOf(T.integer().orNothing())                  | T.nothing()
        T.listOf(T.integer().orNothing())                  | T.integer()
        T.listOf(T.integer().orNothing()).orNothing()      | T.integer()
        T.listOf(T.sumOf(T.integer(), T.bool()))           | T.integer()
        T.listOf(T.sumOf(T.integer(), T.bool()))           | T.bool()
        T.listOf(T.sumOf(T.integer(), T.bool()))           | T.sumOf(T.bool(), T.string())
        T.sumOf(T.listOf(T.integer()), T.listOf(T.bool())) | T.bool()
        T.sumOf(T.listOf(T.integer()), T.listOf(T.bool())) | T.integer()
    }

    def "contains expression is invalid for #haystackType and #needleType"() {
        given:
        def domain = Mock(Domain) {}
        def exp = new ContainsExpression(
                Mock(CustomAspectAttributeValueExpression) {
                    getValueType(domain, ElementType.ASSET) >> haystackType
                },
                Mock(ConstantExpression) {
                    getValueType(domain, ElementType.ASSET) >> needleType
                },
        )

        when:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        IllegalArgumentException illEx = thrown()
        illEx.message == expectedError

        where:
        haystackType                      | needleType  | expectedError
        T.integer()                       | T.integer() | "invalid haystack: expected List<*|Null>|Null, got Integer"
        T.mapOf(T.string(), T.string())   | T.integer() | "invalid haystack: expected List<*|Null>|Null, got Map<String,String>"
        T.listOf(T.integer())             | T.string()  | "haystack cannot contain needle: Integer does not intersect with String"
        T.listOf(T.integer().orNothing()) | T.string()  | "haystack cannot contain needle: Integer|Null does not intersect with String"
        T.listOf(T.integer())             | T.nothing() | "haystack cannot contain needle: Integer does not intersect with Null"
    }

    def "map expression is valid for #sourceType and #mappingType"() {
        given:
        def domain = Mock(Domain) {}
        def exp = new MapExpression(
                Mock(CustomAspectAttributeValueExpression) {
                    getValueType(domain, ElementType.ASSET) >> sourceType
                },
                Mock(ConstantExpression) {
                    getValueType(domain, ElementType.ASSET) >> mappingType
                },
        )

        when:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        noExceptionThrown()
        exp.getValueType(domain, ElementType.ASSET) == expectedResultType

        where:
        sourceType                               | mappingType                       | expectedResultType
        T.nothing()                              | T.mapOf(T.integer(), T.integer()) | T.nothing()
        T.listOf(T.integer())                    | T.mapOf(T.integer(), T.integer()) | T.listOf(T.integer().orNothing())
        T.listOf(T.integer())                    | T.mapOf(T.integer(), T.string())  | T.listOf(T.string().orNothing())
        T.listOf(T.integer()).orNothing()        | T.mapOf(T.integer(), T.integer()) | T.listOf(T.integer().orNothing()).orNothing()
        T.listOf(T.integer()).orNothing()        | T.mapOf(T.integer(), T.string())  | T.listOf(T.string().orNothing()).orNothing()
        T.listOf(T.sumOf(T.integer(), T.bool())) | T.mapOf(T.integer(), T.string())  | T.listOf(T.string().orNothing())
        T.listOf(T.sumOf(T.integer(), T.bool())) | T.mapOf(T.bool(), T.string())     | T.listOf(T.string().orNothing())
    }

    def "map expression is invalid for #sourceType and #mappingType"() {
        given:
        def domain = Mock(Domain) {}
        def exp = new MapExpression(
                Mock(VeoExpression) {
                    getValueType(domain, ElementType.ASSET) >> sourceType
                },
                Mock(ConstantExpression) {
                    getValueType(domain, ElementType.ASSET) >> mappingType
                },
        )

        when:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        IllegalArgumentException illEx = thrown()
        illEx.message == expectedError

        where:
        sourceType                        | mappingType                                   | expectedError
        T.integer()                       | T.mapOf(T.string(), T.string())               | "invalid source: expected List<*|Null>|Null, got Integer"
        T.integer().orNothing()           | T.mapOf(T.string(), T.string())               | "invalid source: expected List<*|Null>|Null, got Integer|Null"
        T.listOf(T.integer())             | T.nothing()                                   | "invalid mapping: expected Map, got Null"
        T.listOf(T.integer())             | T.mapOf(T.integer(), T.integer()).orNothing() | "invalid mapping: expected Map, got Map<Integer,Integer>|Null"
        T.listOf(T.integer())             | T.mapOf(T.string(), T.string())               | "source does not match mapping: Integer does not intersect with String"
        T.listOf(T.integer())             | T.mapOf(T.string(), T.integer())              | "source does not match mapping: Integer does not intersect with String"
        T.listOf(T.integer().orNothing()) | T.mapOf(T.string(), T.integer())              | "source does not match mapping: Integer|Null does not intersect with String"
    }

    def "current element can be evaluated"() {
        given:
        Domain domain = Mock()
        Element element = Mock()
        CurrentElementExpression exp = new CurrentElementExpression()

        when:
        exp.selfValidate(domain, ElementType.CONTROL)

        then:
        noExceptionThrown()

        expect:
        exp.getValueType(domain, ElementType.CONTROL) == T.element(ElementType.CONTROL)
        exp.getValue(element, domain) == element
    }

    def "read attribute"() {
        given:
        Domain domain = Mock() {
            getCustomAspectAttributeDefinition(ElementType.PERSON, "myType", "attribool") >> new BooleanAttributeDefinition()
        }
        Person person = Spy {
            getCustomAspects(domain) >> [
                Mock(CustomAspect) {
                    type >> "myType"
                    attributes >> [
                        attribool: true
                    ]
                }
            ]
        }
        def validExp = new CustomAspectAttributeValueExpression("myType", "attribool")

        when:
        validExp.selfValidate(domain, ElementType.PERSON)

        then:
        noExceptionThrown()

        expect:
        validExp.getValueType(domain, ElementType.PERSON) == T.bool().orNothing()
        validExp.getValue(person, domain) == true
    }

    def "count parts"() {
        given:
        Domain domain = Mock() {
            getElementTypeDefinition(ElementType.PERSON) >> Mock(ElementTypeDefinition) {
                getSubTypeDefinition("theOne") >> Mock(SubTypeDefinition)
                getSubTypeDefinition("notTheOne") >> Mock(SubTypeDefinition)
            }
        }
        Person person = Spy {
            parts >> [
                Spy(Person) {
                    findSubType(domain) >> Optional.of("theOne")
                },
                Spy(Person) {
                    findSubType(domain) >> Optional.of("theOne")
                },
                Spy(Person) {
                    findSubType(domain) >> Optional.of("notTheOne")
                },
                Spy(Person) {
                    findSubType(domain) >> Optional.empty()
                },
            ]
        }
        def exp = new PartCountExpression("theOne")

        when:
        exp.selfValidate(domain, ElementType.PERSON)

        then:
        noExceptionThrown()

        expect:
        exp.getValueType(domain, ElementType.PERSON) == T.integer()
        exp.getValue(person, domain) == 2
    }

    def "can only find CI controls for risk-affected element"() {
        given:
        def domain = Mock(Domain)
        def exp = new ImplementedRequirementsExpression(Mock(VeoExpression) {
            getValueType(domain, ElementType.ASSET) >> T.element(ElementType.SCENARIO)
        })

        when:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "cannot determine implemented controls: expected Asset|Process|Scope, got Scenario"
    }

    def "Read same attribute from different domains"() {
        given:
        Domain domain1 = Mock()
        CustomAspect ca1 = Stub {
            getType() >> 'ca'
            getAttributes()>> [attr: 'v1']
        }
        Domain domain2 = Mock()
        CustomAspect ca2 = Stub {
            getType() >> 'ca'
            getAttributes()>> [attr: 'v2']
        }
        Element element = Stub {
            getCustomAspects(domain1)>> [ca1]
            getCustomAspects(domain2)>> [ca2]
        }

        CustomAspectAttributeValueExpression e = new CustomAspectAttributeValueExpression('ca', 'attr')

        expect:
        e.getValue(element, domain1) == 'v1'
        e.getValue(element, domain2) == 'v2'
    }

    def "minimum inbound link attribute value is determined"() {
        given:
        def domain = Mock(Domain) {
            getLinkDefinition(ElementType.PERSON, "workedOn") >> Mock(LinkDefinition) {
                attributeDefinitions >> [
                    time: new DurationAttributeDefinition()
                ]
            }
        }

        and:
        def process = Spy(Process) {
            getInboundLinks(domain) >> [
                Mock(CustomLink) {
                    type >> "workedOn"
                    source >> Spy(Person)
                    attributes >> [
                        time: "P1D"
                    ]
                },
                Mock(CustomLink) {
                    type >> "workedOn"
                    source >> Spy(Person)
                    attributes >> [
                        time: "PT20H"
                    ]
                },
                Mock(CustomLink) {
                    type >> "workedOn"
                    source >> Spy(Person)
                    attributes >> [:]
                },
            ]
        }

        and:
        def exp = new MinExpression(
                new AttributeExpression(
                        new LinksExpression(
                                LinkDirection.INBOUND,
                                "workedOn",
                                ElementType.PERSON,
                                ),
                        "time",
                        )
        )

        expect:
        exp.selfValidate(domain, ElementType.PROCESS)
        exp.getValueType(domain, ElementType.PROCESS) == T.durationString().orNothing()
        exp.getValue(process, domain) == "PT20H"
    }

    def "minimum outbound link attribute is determined"() {
        given:
        def domain = Mock(Domain) {
            getLinkDefinition(ElementType.PERSON, "workedOn") >> Mock(LinkDefinition) {
                attributeDefinitions >> [
                    time: new DurationAttributeDefinition()
                ]
            }
        }

        and:
        def person = Spy(Person) {
            getLinks(domain) >> [
                Mock(CustomLink) {
                    type >> "workedOn"
                    target >> Spy(Process)
                    attributes >> [
                        time: "P4D"
                    ]
                },
                Mock(CustomLink) {
                    type >> "workedOn"
                    target >> Spy(Process)
                    attributes >> [
                        time: "PT8M"
                    ]
                },
                Mock(CustomLink) {
                    type >> "workedOn"
                    target >> Spy(Process)
                    attributes >> [:]
                },
            ]
        }

        and:
        def exp = new MinExpression(
                new AttributeExpression(
                        new LinksExpression(
                                LinkDirection.OUTBOUND,
                                "workedOn",
                                null
                                ),
                        "time",
                        )
        )

        expect:
        exp.selfValidate(domain, ElementType.PERSON)
        exp.getValueType(domain, ElementType.PERSON) == T.durationString().orNothing()
        exp.getValue(person, domain) == "PT8M"
    }

    def "attribute foo can be extracted from #sourceType"(T sourceType, T outType) {
        given:
        def domain = Mock(Domain)
        def exp = new AttributeExpression(Mock(VeoExpression) {
            getValueType(domain, ElementType.PERSON) >> sourceType
        }, "foo")

        expect:
        exp.selfValidate(domain, ElementType.PERSON)
        exp.getValueType(domain, ElementType.PERSON) == outType

        where:
        sourceType                                                                               | outType
        T.attributeContainer([foo: T.integer()])                                                 | T.integer()
        T.attributeContainer([foo: T.string(), bar: T.bool()])                                   | T.string()
        T.attributeContainer([foo: T.bool().orNothing()])                                        | T.bool().orNothing()
        T.listOf(T.attributeContainer([foo: T.string()]))                                        | T.listOf(T.string())
        T.listOf(T.attributeContainer([foo: T.string().orNothing()]))                            | T.listOf(T.string().orNothing())
        T.sumOf(T.attributeContainer([foo: T.integer()]), T.attributeContainer([foo: T.bool()])) | T.sumOf(T.integer(), T.bool())
    }

    def "attribute foo cannot be extracted from #data.sourceType"(data) {
        given:
        def domain = Mock(Domain)
        def exp = new AttributeExpression(Mock(VeoExpression) {
            getValueType(domain, ElementType.PERSON) >> data.sourceType
        }, "foo")

        when:
        exp.selfValidate(domain, ElementType.PERSON)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == data.expectedError

        where:
        data << [
            [
                sourceType: T.attributeContainer([goo: T.string(), star: T.integer()]),
                expectedError: "invalid source: AttributeContainer<goo:String,star:Integer> does not contain attribute foo"
            ],
            [
                sourceType: T.attributeContainer([foo: T.string()]).orNothing(),
                expectedError: "invalid source: AttributeContainer<foo:String>|Null does not match: Null is not an attribute container"
            ],
            [
                sourceType: T.sumOf(T.attributeContainer([foo: T.string()]), T.attributeContainer([goo: T.string()])),
                expectedError: "invalid source: AttributeContainer<foo:String>|AttributeContainer<goo:String> does not match: " +
                "AttributeContainer<goo:String> does not contain attribute foo"
            ],
            [
                sourceType: T.listOf(T.attributeContainer([blue: T.string()])),
                expectedError: "invalid source items: AttributeContainer<blue:String> does not contain attribute foo"
            ],
        ]
    }

    def "cannot compare different value types"() {
        given:
        def domain = Mock(Domain)
        def exp = new MinExpression(
                Mock(VeoExpression) {
                    getValueType(domain, ElementType.ASSET) >> T.listOf(T.sumOf(T.integer(), T.string()))
                }
        )

        when:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "cannot compare values with different types (Integer|String)"
    }

    def "list expression builds list"() {
        given:
        def domain = Mock(Domain) {
            getCustomAspectAttributeDefinition(ElementType.ASSET, "nothingSpecial", "foo") >> new TextAttributeDefinition()
            getCustomAspectAttributeDefinition(ElementType.ASSET, "nothingSpecial", "bar") >> new IntegerAttributeDefinition()
        }

        and:
        def asset = Spy(Asset) {
            getCustomAspects(domain) >> [
                Mock(CustomAspect) {
                    type >> "nothingSpecial"
                    attributes >> [
                        foo: "ok",
                        bar: 42,
                    ]
                },
            ]
        }

        and:
        def exp = new ListOfExpression([
            new CustomAspectAttributeValueExpression("nothingSpecial", "foo"),
            new CustomAspectAttributeValueExpression("nothingSpecial", "bar"),
        ])

        expect:
        exp.selfValidate(domain, ElementType.ASSET)
        exp.getValueType(domain, ElementType.ASSET) == T.listOf(T.sumOf(T.integer(), T.string()).orNothing())
        exp.getValue(asset, domain) == ["ok", 42]
    }

    def "outbound links can be filtered by impact inheritance"() {
        given:
        def inheritingDefinition = Mock(LinkDefinition) {
            attributeDefinitions >> [foo: new TextAttributeDefinition()]
        }
        def alsoInheritingDefinition = Mock(LinkDefinition) {
            attributeDefinitions >> [bar: new IntegerAttributeDefinition()]
        }
        def notInheritingDefinition = Mock(LinkDefinition) {
            attributeDefinitions >> [:]
        }
        def domain = Mock(Domain) {
            riskDefinitions >> [
                r1: Mock(RiskDefinition) {
                    impactInheritingLinks >> [(ElementType.ASSET): ["inheriting"]]
                },
                r2: Mock(RiskDefinition) {
                    impactInheritingLinks >> [
                        (ElementType.ASSET): ["alsoInheriting"],
                        (ElementType.PROCESS): ["notInheriting"],
                    ]
                },
            ]
            getElementTypeDefinition(ElementType.ASSET) >> Mock(ElementTypeDefinition) {
                links >> [
                    inheriting: inheritingDefinition,
                    alsoInheriting: alsoInheritingDefinition,
                    notInheriting: notInheritingDefinition,
                ]
            }
            getLinkDefinition(ElementType.ASSET, "inheriting") >> inheritingDefinition
            getLinkDefinition(ElementType.ASSET, "notInheriting") >> notInheritingDefinition
        }

        and:
        def asset = Spy(Asset)
        def inheritingLink = Mock(CustomLink) {
            type >> "inheriting"
            source >> asset
        }
        def alsoInheritingLink = Mock(CustomLink) {
            type >> "alsoInheriting"
            source >> asset
        }
        def notInheritingLink = Mock(CustomLink) {
            type >> "notInheriting"
            source >> asset
        }
        asset.getLinks(domain) >> [
            inheritingLink,
            alsoInheritingLink,
            notInheritingLink
        ]

        when: "filtering by impact inheritance only"
        def exp = new LinksExpression(LinkDirection.OUTBOUND, null, null, true)
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        noExceptionThrown()
        exp.getValueType(domain, ElementType.ASSET) == T.listOf(T.sumOf(
                        T.attributeContainer([foo: T.string().orNothing()]),
                        T.attributeContainer([bar: T.integer()]),
                ))
        exp.getValue(asset, domain) ==~ [
            inheritingLink,
            alsoInheritingLink
        ]

        when: "not filtering at all"
        exp = new LinksExpression(LinkDirection.OUTBOUND, null, null, false)
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        noExceptionThrown()
        exp.getValue(asset, domain) ==~ [
            inheritingLink,
            alsoInheritingLink,
            notInheritingLink
        ]

        when: "combining impact inheritance with an inheriting link type"
        exp = new LinksExpression(LinkDirection.OUTBOUND, "inheriting", null, true)
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        noExceptionThrown()
        exp.getValueType(domain, ElementType.ASSET) == T.listOf(T.attributeContainer([foo: T.string().orNothing()]))
        exp.getValue(asset, domain) == [inheritingLink]

        when: "combining impact inheritance with a link type that does not inherit impacts"
        new LinksExpression(LinkDirection.OUTBOUND, "notInheriting", null, true).selfValidate(domain, ElementType.ASSET)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Link type 'notInheriting' does not inherit impacts"
    }

    def "inbound links can be filtered by impact inheritance"() {
        given:
        def domain = Mock(Domain) {
            riskDefinitions >> [
                r1: Mock(RiskDefinition) {
                    impactInheritingLinks >> [(ElementType.ASSET): ["inheriting"]]
                },
            ]
            getElementTypeDefinition(ElementType.ASSET) >> Mock(ElementTypeDefinition) {
                links >> [
                    inheriting: Mock(LinkDefinition) {
                        targetType >> ElementType.PROCESS
                        attributeDefinitions >> [:]
                    },
                    notInheriting: Mock(LinkDefinition) {
                        targetType >> ElementType.PROCESS
                        attributeDefinitions >> [:]
                    },
                    inheritingButOtherTarget: Mock(LinkDefinition) {
                        targetType >> ElementType.SCOPE
                        attributeDefinitions >> [foo: new TextAttributeDefinition()]
                    },
                ]
            }
        }
        def inheritingLink = Mock(CustomLink) {
            type >> "inheriting"
            source >> Spy(Asset)
        }
        def notInheritingLink = Mock(CustomLink) {
            type >> "notInheriting"
            source >> Spy(Asset)
        }
        def process = Spy(Process) {
            getInboundLinks(domain) >> [
                inheritingLink,
                notInheritingLink
            ]
        }
        def exp = new LinksExpression(LinkDirection.INBOUND, null, ElementType.ASSET, true)

        expect:
        exp.selfValidate(domain, ElementType.PROCESS)
        exp.getValueType(domain, ElementType.PROCESS) == T.listOf(T.attributeContainer([:]))
        exp.getValue(process, domain) == [inheritingLink]
    }

    def "invalid links expression is rejected: #expectedMessage"() {
        given:
        def domain = Mock(Domain) {
            riskDefinitions >> [:]
            getElementTypeDefinition(ElementType.ASSET) >> Mock(ElementTypeDefinition) {
                links >> [
                    notInheriting: Mock(LinkDefinition) {
                        targetType >> ElementType.PROCESS
                    }
                ]
            }
            getElementTypeDefinition(ElementType.PERSON) >> Mock(ElementTypeDefinition) {
                links >> [
                    toScope: Mock(LinkDefinition) {
                        targetType >> ElementType.SCOPE
                    }
                ]
            }
        }

        when:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        def ex = thrown(RuntimeException)
        ex.message == expectedMessage

        where:
        exp                                                                     | expectedMessage
        new LinksExpression(LinkDirection.OUTBOUND, null, ElementType.PERSON)   | "sourceType is not applicable for outbound links and should be null"
        new LinksExpression(LinkDirection.INBOUND, null, null)                  | "sourceType must be specified for inbound links"
        new LinksExpression(LinkDirection.OUTBOUND, null, null, true)           | "No matching links defined for assets"
        new LinksExpression(LinkDirection.INBOUND, null, ElementType.PERSON)    | "No matching links defined for assets"
    }

    def "inbound link event affects #direction links with type #linkType: #expected"() {
        given:
        def domain = Mock(Domain)
        def exp = new LinksExpression(direction, linkType, direction == LinkDirection.INBOUND ? ElementType.PERSON : null)
        def event = new InboundLinkEvent(Mock(Element), domain, "workedOn", InboundLinkEvent.Operation.CREATION_OR_UPDATE, this)

        expect:
        exp.isAffectedByEvent(event, domain) == expected
        !exp.isAffectedByEvent(Mock(ElementEvent), domain)

        where:
        direction              | linkType   | expected
        LinkDirection.INBOUND  | "workedOn" | true
        LinkDirection.INBOUND  | "other"    | false
        LinkDirection.INBOUND  | null       | true
        LinkDirection.OUTBOUND | "workedOn" | false
        LinkDirection.OUTBOUND | null       | false
    }

    def "potential impacts complete is #expected for #impactValues"() {
        given:
        def domain = Mock(Domain) {
            riskDefinitions >> [
                r1: Mock(RiskDefinition) {
                    id >> "r1"
                    categories >> [
                        Mock(CategoryDefinition) {
                            id >> "C"
                            potentialImpacts >> [Mock(CategoryLevel)]
                        },
                        Mock(CategoryDefinition) {
                            id >> "I"
                            potentialImpacts >> [Mock(CategoryLevel)]
                        },
                        Mock(CategoryDefinition) {
                            id >> "noImpacts"
                            potentialImpacts >> []
                        },
                    ]
                },
                r2: Mock(RiskDefinition) {
                    id >> "r2"
                    categories >> [
                        Mock(CategoryDefinition) {
                            id >> "A"
                            potentialImpacts >> [Mock(CategoryLevel)]
                        },
                    ]
                },
            ]
        }
        def asset = Spy(Asset) {
            getImpactValues(domain, _) >> { Domain d, RiskDefinitionRef rd ->
                Optional.ofNullable(impactValues[rd.idRef])
            }
        }
        def exp = new PotentialImpactsCompleteExpression()

        expect:
        exp.selfValidate(domain, ElementType.ASSET)
        exp.getValueType(domain, ElementType.ASSET) == T.bool()
        exp.getValue(asset, domain) == expected

        where:
        impactValues                                                                       | expected
        [r1: impacts(specific: [C: 1], calculated: [I: 2]), r2: impacts(specific: [A: 0])] | true
        [r1: impacts(specific: [C: 1]), r2: impacts(specific: [A: 0])]                     | false
        [r1: impacts(specific: [C: 1, I: 1])]                                              | false
        [:]                                                                                | false
    }

    def "potential impacts complete is true without risk definitions"() {
        given:
        def domain = Mock(Domain) {
            riskDefinitions >> [:]
        }

        expect:
        new PotentialImpactsCompleteExpression().getValue(Spy(Scope), domain) == true
    }

    def "potential impacts complete can only be used for risk-affected elements"() {
        when:
        new PotentialImpactsCompleteExpression().selfValidate(Mock(Domain), ElementType.DOCUMENT)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "cannot determine potential impacts: expected Asset|Process|Scope, got Document"
    }

    def "max risk should be reevaluated after #changes"() {
        given:
        def exp = new MaxRiskExpression()
        def event = Mock(RiskAffectingElementChangeEvent) {
            changedRisks >> [
                Mock(RiskChangedEvent) {
                    it.changes >> changes
                }
            ]
        }

        expect:
        exp.isAffectedByEvent(event, Mock(Domain))

        where:
        changes << [
            [
                RiskEvent.ChangedValues.RISK_CREATED
            ],
            [
                RiskEvent.ChangedValues.RISK_VALUES_CHANGED
            ],
            [
                RiskEvent.ChangedValues.RISK_DELETED
            ],
            [
                RiskEvent.ChangedValues.PROBABILITY_VALUES_CHANGED,
                RiskEvent.ChangedValues.RISK_VALUES_CHANGED
            ],
        ]
    }

    def "max risk should not be reevaluated after #changes"() {
        given:
        def exp = new MaxRiskExpression()
        def event = Mock(RiskAffectingElementChangeEvent) {
            changedRisks >> [
                Mock(RiskChangedEvent) {
                    it.changes >> changes
                }
            ]
        }

        expect:
        !exp.isAffectedByEvent(event, Mock(Domain))

        where:
        changes << [
            [
                RiskEvent.ChangedValues.IMPACT_VALUES_CHANGED
            ],
            [
                RiskEvent.ChangedValues.PROBABILITY_VALUES_CHANGED
            ],
            [
                RiskEvent.ChangedValues.PROBABILITY_VALUES_CHANGED,
                RiskEvent.ChangedValues.IMPACT_VALUES_CHANGED
            ],
        ]
    }

    def "control implementations can be evaluated"() {
        given:
        def domain = Mock(Domain) {
            controlImplementationConfiguration >> new ControlImplementationConfiguration(
                    [
                        "CTL_Requirement",
                        "CTL_Module"
                    ] as Set,
                    "CTL_Safeguard"
            )
        }
        def requirementCi = controlImplementation(domain, "CTL_Requirement")
        def moduleCi = controlImplementation(domain, "CTL_Module")
        def safeguardCi = controlImplementation(domain, "CTL_Safeguard")
        def noSubTypeCi = controlImplementation(domain, null)
        def process = Spy(Process) {
            controlImplementations >> [
                requirementCi,
                moduleCi,
                safeguardCi,
                noSubTypeCi
            ]
        }

        when:
        def exp = new ControlImplementationsExpression(false)
        exp.selfValidate(domain, ElementType.PROCESS)

        then:
        noExceptionThrown()
        exp.getValueType(domain, ElementType.PROCESS) == T.listOf(T.controlImplementation())
        exp.getValue(process, domain) ==~ [
            requirementCi,
            moduleCi,
            safeguardCi,
            noSubTypeCi
        ]

        when: "filtering by compliance control sub types"
        exp = new ControlImplementationsExpression(true)

        then:
        exp.getValue(process, domain) ==~ [requirementCi, moduleCi]
    }

    def "control implementations can only be determined for risk-affected elements"() {
        when:
        new ControlImplementationsExpression().selfValidate(Mock(Domain), ElementType.CONTROL)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "cannot determine control implementations: expected Asset|Process|Scope, got Control"
    }

    def "risk definition of scope can be evaluated"() {
        given:
        def domain = Mock(Domain)
        def scopeWithRiskDefinition = Spy(Scope) {
            getRiskDefinition(domain) >> Optional.of(new RiskDefinitionRef("r1"))
        }
        def scopeWithoutRiskDefinition = Spy(Scope) {
            getRiskDefinition(domain) >> Optional.empty()
        }
        def exp = new RiskDefinitionExpression()

        expect:
        exp.selfValidate(domain, ElementType.SCOPE)
        exp.getValueType(domain, ElementType.SCOPE) == T.string().orNothing()
        exp.getValue(scopeWithRiskDefinition, domain) == "r1"
        exp.getValue(scopeWithoutRiskDefinition, domain) == null
    }

    def "risk definition can only be determined for scopes"() {
        when:
        new RiskDefinitionExpression().selfValidate(Mock(Domain), elementType)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "cannot determine risk definition: expected Scope, got $typeName"

        where:
        elementType         | typeName
        ElementType.ASSET   | "Asset"
        ElementType.PROCESS | "Process"
    }

    private static ImpactValues impacts(Map<String, Map<String, Integer>> values) {
        def toRefs = { Map<String, Integer> m ->
            (m ?: [:]).collectEntries { k, v ->
                [(new CategoryRef(k)): new ImpactRef(BigDecimal.valueOf(v))]
            }
        }
        new ImpactValues(toRefs(values.specific), toRefs(values.calculated), null, null)
    }

    private ControlImplementation controlImplementation(Domain domain, String controlSubType) {
        Mock(ControlImplementation) {
            control >> Mock(Control) {
                findSubType(domain) >> Optional.ofNullable(controlSubType)
            }
        }
    }
}
