/*******************************************************************************
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
 ******************************************************************************/
package org.veo.core.entity.condition

import java.time.Duration

import org.codehaus.groovy.ast.expr.ListExpression

import org.veo.core.entity.Asset
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
import org.veo.core.entity.definitions.ElementTypeDefinition
import org.veo.core.entity.definitions.LinkDefinition
import org.veo.core.entity.definitions.SubTypeDefinition
import org.veo.core.entity.definitions.attribute.BooleanAttributeDefinition
import org.veo.core.entity.definitions.attribute.DurationAttributeDefinition
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition
import org.veo.core.entity.definitions.attribute.ListAttributeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.core.entity.type.VeoType
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
        validExp.selfValidate(domain,ElementType.DOCUMENT)

        then:
        noExceptionThrown()

        expect:
        validExp.getValueType(domain,ElementType.DOCUMENT) == T.integer()
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
            getValueType(domain, ElementType.ASSET) >> T.listOf(
                    T.sumOf(T.element(ElementType.DOCUMENT), T.element(ElementType.PERSON))
                    ).orNothing()
        }
        def exp = new LinkTargetsExpression(sources, null)

        when:
        exp.selfValidate(domain,ElementType.ASSET)

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
        exp.selfValidate(domain,ElementType.ASSET)

        then:
        noExceptionThrown()

        expect:
        exp.getValueType(domain, ElementType.ASSET) == T.listOf(T.sumOf(
                T.element(ElementType.INCIDENT),
                T.element(ElementType.SCOPE),
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
            getValueType(domain, elementType) >> T.bool()
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
        thenValueType | elseValueType
        T.string()    | T.string()
        T.string()    | T.nothing()
        T.nothing()   | T.string()
    }

    def "Ternary supports two different result types"() {
        given:
        Domain domain = Mock()
        ElementType elementType = Mock()

        ConstantExpression conditionExpression = Stub{
            getValueType(domain, elementType) >> T.bool()
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
        expression.getValueType(domain,elementType) == T.sumOf([thenValueType, elseValueType])

        where:
        thenValueType | elseValueType
        T.integer()   | T.string()
        T.string()    | T.listOf(T.string())
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
        exp.getValueType(domain, ElementType.ASSET) == T.bool()

        when: "inverting the comparison"
        exp = new EqualsExpression(exp.right, exp.left)

        and:
        exp.selfValidate(domain, ElementType.ASSET)

        then:
        noExceptionThrown()
        exp.getValueType(domain, ElementType.ASSET) == T.bool()

        where:
        leftType                                 | rightType
        T.integer()                              | T.integer()
        T.integer().orNothing()                  | T.integer()
        T.integer().orNothing()                  | T.sumOf(T.integer(), T.string())
        T.sumOf(T.integer(), T.bool())           | T.integer()
        T.sumOf(T.integer(), T.bool())           | T.bool()
        T.sumOf(T.integer(), T.bool())           | T.bool().orNothing()
        T.sumOf(T.integer(), T.bool())           | T.sumOf(T.bool(), T.string())
        T.listOf(T.integer())                    | T.listOf(T.integer())
        T.listOf(T.integer())                    | T.listOf(T.integer().orNothing())
        T.listOf(T.sumOf(T.integer(), T.bool())) | T.listOf(T.sumOf(T.string(), T.bool()))
        T.sumOf(T.bool(), T.listOf(T.integer())) | T.sumOf(T.string(), T.listOf(T.integer().orNothing()))
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
        leftType                      | rightType   | expectedError
        T.integer()                   | T.nothing() | "given values cannot be equal: Integer does not intersect with Null"
        T.integer()                   | T.string()  | "given values cannot be equal: Integer does not intersect with String"
        T.integer().orNothing()       | T.string()  | "given values cannot be equal: Integer|Null does not intersect with String"
        T.sumOf(T.bool(), T.string()) | T.number()  | "given values cannot be equal: Boolean|String does not intersect with BigDecimal|Integer|Long"
        T.listOf(T.integer())         | T.integer() | "given values cannot be equal: List<Integer> does not intersect with Integer"
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
        exp.selfValidate(domain,ElementType.CONTROL)

        then:
        noExceptionThrown()

        expect:
        exp.getValueType(domain,ElementType.CONTROL) == T.element(ElementType.CONTROL)
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
        validExp.getValueType(domain,ElementType.PERSON) == T.bool().orNothing()
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
        exp.getValueType(domain, ElementType.PERSON) == T.integer()
        exp.getValue(person,domain) == 2
    }

    def "can only find CI controls for risk-affected element"() {
        given:
        def domain = Mock(Domain)
        def exp = new ImplementedRequirementsExpression(Mock(VeoExpression) {
            getValueType(domain, ElementType.ASSET) >> T.element(ElementType.SCENARIO)
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
        exp.selfValidate(domain,ElementType.ASSET)

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
        exp.selfValidate(domain,ElementType.ASSET)
        exp.getValueType(domain, ElementType.ASSET) == T.listOf(T.sumOf(T.integer(),T.string()).orNothing())
        exp.getValue(asset, domain) == ["ok", 42]
    }
}
