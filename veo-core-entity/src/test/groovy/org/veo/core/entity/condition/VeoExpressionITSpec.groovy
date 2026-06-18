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
import org.veo.core.entity.CustomAspect
import org.veo.core.entity.CustomLink
import org.veo.core.entity.Document
import org.veo.core.entity.Domain
import org.veo.core.entity.Element
import org.veo.core.entity.ElementType
import org.veo.core.entity.Incident
import org.veo.core.entity.Person
import org.veo.core.entity.Scenario
import org.veo.core.entity.Scope
import org.veo.core.entity.definitions.ElementTypeDefinition
import org.veo.core.entity.definitions.LinkDefinition
import org.veo.core.entity.definitions.SubTypeDefinition
import org.veo.core.entity.definitions.attribute.BooleanAttributeDefinition
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition
import org.veo.core.entity.definitions.attribute.ListAttributeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.core.entity.type.VeoType

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
        validExp.selfValidate(domain,ElementType.DOCUMENT)

        then:
        noExceptionThrown()

        expect:
        validExp.getValueType(domain,ElementType.DOCUMENT) == VeoType.integer()
        validExp.getValue(document,domain) == 3

        when:
        new CustomAspectAttributeSizeExpression("myType", "intribute").selfValidate(domain,ElementType.DOCUMENT)

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
        exp.getValue(element,domain) ==~ [targetA, targetB, targetC]
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
            getValueType(domain, ElementType.ASSET) >> VeoType.listOf(
                    VeoType.sumOf(VeoType.element(ElementType.DOCUMENT), VeoType.element(ElementType.PERSON))
                    ).orNothing()
        }
        def exp = new LinkTargetsExpression(sources, null)

        when:
        exp.selfValidate(domain,ElementType.ASSET)

        then:
        noExceptionThrown()

        expect:
        exp.getValueType(domain, ElementType.ASSET) == VeoType.listOf(VeoType.sumOf(
                VeoType.element(ElementType.INCIDENT),
                VeoType.element(ElementType.PROCESS),
                VeoType.element(ElementType.SCOPE),
                ))

        when: "filtering by a certain type"
        exp = new LinkTargetsExpression(sources, "myLink")
        exp.selfValidate(domain,ElementType.ASSET)

        then:
        noExceptionThrown()

        expect:
        exp.getValueType(domain, ElementType.ASSET) == VeoType.listOf(VeoType.sumOf(
                VeoType.element(ElementType.INCIDENT),
                VeoType.element(ElementType.SCOPE),
                ))

        when: "filtering by a missing type"
        exp = new LinkTargetsExpression(sources, "fantasyLink")
        exp.selfValidate(domain,ElementType.ASSET)

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
        exp.selfValidate(domain,ElementType.INCIDENT)
        exp.getValueType(domain,ElementType.INCIDENT).toString() == expectedType
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

    def "Ternary expression returns expected result"() {
        given:
        Domain domain = Mock()
        Element element = Mock()

        ConstantExpression conditionExpression = Stub{
            getValue(element, domain) >> conditionValue
        }
        CustomAspectAttributeValueExpression thenExpression = Stub{
            getValue(element, domain) >> thenValue
        }
        ConstantExpression elseExpression = Stub{
            getValue(element, domain) >> elseValue
        }
        TernaryExpression expression = new TernaryExpression(conditionExpression, thenExpression, elseExpression)

        expect:
        expression.getValue(element, domain) == result

        where:
        conditionValue | thenValue | elseValue| result
        true | 3 | 4 | 3
        false | 3 | 4 | 4
        Boolean.TRUE | 3 | 4 | 3
        Boolean.FALSE | 3 | 4 | 4
        null | 3 | 4 | 4
        "true" | 3 | 4 | 4
    }

    def "Ternary validates correctly with different value types"() {
        given:
        Domain domain = Mock()
        ElementType elementType = Mock()

        ConstantExpression conditionExpression = Stub{
            getValueType(domain, elementType) >> VeoType.bool()
        }
        CustomAspectAttributeValueExpression thenExpression = Stub {
            getValueType(domain, elementType) >> thenValueType
        }

        ConstantExpression elseExpression = Stub {
            getValueType(domain, elementType) >> elseValueType
        }

        TernaryExpression expression = new TernaryExpression(conditionExpression, thenExpression, elseExpression)

        when:
        expression.selfValidate(domain, elementType)

        then:
        noExceptionThrown()

        where:
        thenValueType     | elseValueType
        VeoType.string()  | VeoType.string()
        VeoType.string()  | VeoType.nothing()
        VeoType.nothing() | VeoType.string()
    }

    def "Ternary supports two different result types"() {
        given:
        Domain domain = Mock()
        ElementType elementType = Mock()

        ConstantExpression conditionExpression = Stub{
            getValueType(domain, elementType) >> VeoType.bool()
        }
        CustomAspectAttributeValueExpression thenExpression = Stub {
            getValueType(domain, elementType) >> thenValueType
        }

        ConstantExpression elseExpression = Stub {
            getValueType(domain, elementType) >> elseValueType
        }

        TernaryExpression expression = new TernaryExpression(conditionExpression, thenExpression, elseExpression)

        when:
        expression.selfValidate(domain, elementType)

        then:
        noExceptionThrown()
        expression.getValueType(domain,elementType) == VeoType.sumOf([thenValueType, elseValueType])

        where:
        thenValueType | elseValueType
        VeoType.integer() | VeoType.string()
        VeoType.string() | VeoType.listOf(VeoType.string())
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
        [1, 2, 3] | [1:2 , 3:2] | [2, null, 2]
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
        haystackType                                                     | needleType
        VeoType.nothing()                                                | VeoType.string()
        VeoType.listOf(VeoType.integer())                                | VeoType.integer()
        VeoType.listOf(VeoType.integer().orNothing())                    | VeoType.nothing()
        VeoType.listOf(VeoType.integer().orNothing())                    | VeoType.integer()
        VeoType.listOf(VeoType.integer().orNothing()).orNothing()        | VeoType.integer()
        VeoType.listOf(VeoType.sumOf(VeoType.integer(), VeoType.bool())) | VeoType.integer()
        VeoType.listOf(VeoType.sumOf(VeoType.integer(), VeoType.bool())) | VeoType.bool()
        VeoType.listOf(VeoType.sumOf(VeoType.integer(), VeoType.bool())) | VeoType.sumOf(VeoType.bool(), VeoType.string())
        VeoType.listOf(VeoType.integer())                                | VeoType.integer().orNothing()
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
        haystackType                                      | needleType        | expectedError
        VeoType.integer()                                 | VeoType.integer() | "invalid source ('from') for removal: expected List<*|Null>|Null, got Integer"
        VeoType.mapOf(VeoType.string(), VeoType.string()) | VeoType.integer() | "invalid source ('from') for removal: expected List<*|Null>|Null, got Map<String,String>"
        VeoType.listOf(VeoType.integer())                 | VeoType.string()  | "source ('from') cannot contain value: Integer does not intersect with String"
        VeoType.listOf(VeoType.integer().orNothing())     | VeoType.string()  | "source ('from') cannot contain value: Integer|Null does not intersect with String"
        VeoType.listOf(VeoType.integer())                 | VeoType.nothing() | "source ('from') cannot contain value: Integer does not intersect with Null"
    }

    def "equals expression is valid for #leftType and #rightType"() {
        given:
        def domain = Mock(Domain) {}
        def exp = new EqualsExpression(
                Mock(CustomAspectAttributeValueExpression) {
                    getValueType(domain, ElementType.ASSET) >> leftType
                },
                Mock(ConstantExpression) {
                    getValueType(domain, ElementType.ASSET) >> rightType
                },
                )

        when:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        noExceptionThrown()
        exp.getValueType(domain, ElementType.ASSET) == VeoType.bool()

        when: "inverting the comparison"
        exp = new EqualsExpression(exp.right, exp.left)

        and:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        noExceptionThrown()
        exp.getValueType(domain, ElementType.ASSET) == VeoType.bool()

        where:
        leftType                                                         | rightType
        VeoType.integer()                                                | VeoType.integer()
        VeoType.integer().orNothing()                                    | VeoType.integer()
        VeoType.integer().orNothing()                                    | VeoType.sumOf(VeoType.integer(), VeoType.string())
        VeoType.sumOf(VeoType.integer(), VeoType.bool())                 | VeoType.integer()
        VeoType.sumOf(VeoType.integer(), VeoType.bool())                 | VeoType.bool()
        VeoType.sumOf(VeoType.integer(), VeoType.bool())                 | VeoType.bool().orNothing()
        VeoType.sumOf(VeoType.integer(), VeoType.bool())                 | VeoType.sumOf(VeoType.bool(), VeoType.string())
        VeoType.listOf(VeoType.integer())                                | VeoType.listOf(VeoType.integer())
        VeoType.listOf(VeoType.integer())                                | VeoType.listOf(VeoType.integer().orNothing())
        VeoType.listOf(VeoType.sumOf(VeoType.integer(), VeoType.bool())) | VeoType.listOf(VeoType.sumOf(VeoType.string(), VeoType.bool()))
        VeoType.sumOf(VeoType.bool(), VeoType.listOf(VeoType.integer())) | VeoType.sumOf(VeoType.string(), VeoType.listOf(VeoType.integer().orNothing()))
    }

    def "equals expression is invalid for #leftType and #rightType"() {
        given:
        def domain = Mock(Domain) {}
        def exp = new EqualsExpression(
                Mock(CustomAspectAttributeValueExpression) {
                    getValueType(domain, ElementType.ASSET) >> leftType
                },
                Mock(ConstantExpression) {
                    getValueType(domain, ElementType.ASSET) >> rightType
                },
                )

        when:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        IllegalArgumentException illEx = thrown()
        illEx.message == expectedError

        when: "inverting the comparison"
        exp = new EqualsExpression(exp.right, exp.left)

        and:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        thrown(IllegalArgumentException)

        where:
        leftType                                        | rightType                    | expectedError
        VeoType.integer()                               | VeoType.nothing()            | "given values cannot be equal: Integer does not intersect with Null"
        VeoType.integer()                               | VeoType.string()             | "given values cannot be equal: Integer does not intersect with String"
        VeoType.integer().orNothing()                   | VeoType.string()             | "given values cannot be equal: Integer|Null does not intersect with String"
        VeoType.sumOf(VeoType.bool(), VeoType.string()) | VeoType.number()             | "given values cannot be equal: Boolean|String does not intersect with BigDecimal|Integer|Long"
        VeoType.listOf(VeoType.integer())               | VeoType.integer()            | "given values cannot be equal: List<Integer> does not intersect with Integer"
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
        exp.getValueType(domain, ElementType.ASSET) == VeoType.bool()

        where:
        haystackType                                                                     | needleType
        VeoType.nothing()                                                                | VeoType.string()
        VeoType.listOf(VeoType.integer())                                                | VeoType.integer()
        VeoType.listOf(VeoType.integer())                                                | VeoType.integer().orNothing()
        VeoType.listOf(VeoType.integer().orNothing())                                    | VeoType.nothing()
        VeoType.listOf(VeoType.integer().orNothing())                                    | VeoType.integer()
        VeoType.listOf(VeoType.integer().orNothing()).orNothing()                        | VeoType.integer()
        VeoType.listOf(VeoType.sumOf(VeoType.integer(), VeoType.bool()))                 | VeoType.integer()
        VeoType.listOf(VeoType.sumOf(VeoType.integer(), VeoType.bool()))                 | VeoType.bool()
        VeoType.listOf(VeoType.sumOf(VeoType.integer(), VeoType.bool()))                 | VeoType.sumOf(VeoType.bool(), VeoType.string())
        VeoType.sumOf(VeoType.listOf(VeoType.integer()), VeoType.listOf(VeoType.bool())) | VeoType.bool()
        VeoType.sumOf(VeoType.listOf(VeoType.integer()), VeoType.listOf(VeoType.bool())) | VeoType.integer()
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
        haystackType                                      | needleType        | expectedError
        VeoType.integer()                                 | VeoType.integer() | "invalid haystack: expected List<*|Null>|Null, got Integer"
        VeoType.mapOf(VeoType.string(), VeoType.string()) | VeoType.integer() | "invalid haystack: expected List<*|Null>|Null, got Map<String,String>"
        VeoType.listOf(VeoType.integer())                 | VeoType.string()  | "haystack cannot contain needle: Integer does not intersect with String"
        VeoType.listOf(VeoType.integer().orNothing())     | VeoType.string()  | "haystack cannot contain needle: Integer|Null does not intersect with String"
        VeoType.listOf(VeoType.integer())                 | VeoType.nothing() | "haystack cannot contain needle: Integer does not intersect with Null"
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
        sourceType                                                       | mappingType                                         | expectedResultType
        VeoType.nothing()                                                | VeoType.mapOf(VeoType.integer(), VeoType.integer()) | VeoType.nothing()
        VeoType.listOf(VeoType.integer())                                | VeoType.mapOf(VeoType.integer(), VeoType.integer()) | VeoType.listOf(VeoType.integer().orNothing())
        VeoType.listOf(VeoType.integer())                                | VeoType.mapOf(VeoType.integer(), VeoType.string())  | VeoType.listOf(VeoType.string().orNothing())
        VeoType.listOf(VeoType.integer()).orNothing()                    | VeoType.mapOf(VeoType.integer(), VeoType.integer()) | VeoType.listOf(VeoType.integer().orNothing()).orNothing()
        VeoType.listOf(VeoType.integer()).orNothing()                    | VeoType.mapOf(VeoType.integer(), VeoType.string())  | VeoType.listOf(VeoType.string().orNothing()).orNothing()
        VeoType.listOf(VeoType.sumOf(VeoType.integer(), VeoType.bool())) | VeoType.mapOf(VeoType.integer(), VeoType.string())  | VeoType.listOf(VeoType.string().orNothing())
        VeoType.listOf(VeoType.sumOf(VeoType.integer(), VeoType.bool())) | VeoType.mapOf(VeoType.bool(), VeoType.string())     | VeoType.listOf(VeoType.string().orNothing())
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
        sourceType                                                       | mappingType                                                     | expectedError
        VeoType.integer()                                                | VeoType.mapOf(VeoType.string(), VeoType.string())               | "invalid source: expected List<*|Null>|Null, got Integer"
        VeoType.integer().orNothing()                                    | VeoType.mapOf(VeoType.string(), VeoType.string())               | "invalid source: expected List<*|Null>|Null, got Integer|Null"
        VeoType.listOf(VeoType.integer())                                | VeoType.nothing()                                               | "invalid mapping: expected Map, got Null"
        VeoType.listOf(VeoType.integer())                                | VeoType.mapOf(VeoType.integer(), VeoType.integer()).orNothing() | "invalid mapping: expected Map, got Map<Integer,Integer>|Null"
        VeoType.listOf(VeoType.integer())                                | VeoType.mapOf(VeoType.string(), VeoType.string())               | "source does not match mapping: Integer does not intersect with String"
        VeoType.listOf(VeoType.integer())                                | VeoType.mapOf(VeoType.string(), VeoType.integer())              | "source does not match mapping: Integer does not intersect with String"
        VeoType.listOf(VeoType.integer().orNothing())                    | VeoType.mapOf(VeoType.string(), VeoType.integer())              | "source does not match mapping: Integer|Null does not intersect with String"
    }

    def "current element can be evaluated"() {
        given:
        Domain domain = Mock()
        Element element = Mock()
        CurrentElementExpression exp = new CurrentElementExpression()

        when:
        exp.selfValidate(domain,ElementType.CONTROL)

        then:
        noExceptionThrown()

        expect:
        exp.getValueType(domain,ElementType.CONTROL) == VeoType.element(ElementType.CONTROL)
        exp.getValue(element,domain) == element
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
        validExp.selfValidate(domain,ElementType.PERSON)

        then:
        noExceptionThrown()

        expect:
        validExp.getValueType(domain,ElementType.PERSON) == VeoType.bool().orNothing()
        validExp.getValue(person,domain) == true
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
        exp.getValueType(domain, ElementType.PERSON) == VeoType.integer()
        exp.getValue(person,domain) == 2
    }

    def "can only find CI controls for risk-affected element"() {
        given:
        def domain = Mock(Domain)
        def exp = new ImplementedRequirementsExpression(Mock(VeoExpression) {
            getValueType(domain, ElementType.ASSET) >> VeoType.element(ElementType.SCENARIO)
        })

        when:
        exp.selfValidate(domain,ElementType.ASSET)

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

        CustomAspectAttributeValueExpression e = new CustomAspectAttributeValueExpression('ca', 'attr' )

        expect:
        e.getValue(element, domain1) == 'v1'
        e.getValue(element, domain2) == 'v2'
    }
}
