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
package org.veo.core

import org.springframework.beans.factory.annotation.Autowired

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.ValidationMessage

import org.veo.core.entity.Domain
import org.veo.core.entity.EntityType
import org.veo.core.entity.definitions.LinkDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.core.entity.riskdefinition.CategoryLevel
import org.veo.core.entity.riskdefinition.ProbabilityLevel
import org.veo.core.entity.riskdefinition.RiskValue
import org.veo.core.service.EntitySchemaService

class DomainSpecificJsonSchemaITSpec extends VeoSpringSpec {
    @Autowired
    EntitySchemaService entitySchemaService
    Domain domain
    ObjectMapper om = new ObjectMapper()

    def setup() {
        domain = newDomain(newClient {}) {
            applyRiskDefinition("noRiskNoFun", newRiskDefinition("noRiskNoFun") {
                probability.levels = [
                    new ProbabilityLevel(),
                    new ProbabilityLevel(),
                    new ProbabilityLevel(),
                    new ProbabilityLevel()
                ]
                categories = [
                    newCategoryDefinition("A") {
                        potentialImpacts = [
                            new CategoryLevel(),
                            new CategoryLevel(),
                        ]
                    },
                ]
                riskValues = [
                    new RiskValue("low"),
                    new RiskValue("medium"),
                    new RiskValue("high"),
                ]
                implementationStateDefinition.setLevels([
                    new CategoryLevel(),
                    new CategoryLevel()
                ])
            })
        }
    }

    def "#elementType sub type and status is validated"() {
        given:
        createElementTypeDefinition(elementType)
        def element = [
            name: "JSON schema validation test subject",
            subType: "A",
            status: "A1",
            owner: [targetUri: "http://localhost/units/..."]
        ]

        expect:
        validate(element, elementType).empty

        when: "using an undefined sub type"
        element.subType = "C"

        then:
        validate(element, elementType)*.message ==~ [
            "\$.subType: does not have a value in the enumeration [A, B]"
        ]

        when: "using a sub type that does not define the current status"
        element.subType = "B"

        then:
        validate(element, elementType)*.message ==~ [
            "\$.status: does not have a value in the enumeration [B1, B2]"
        ]

        where:
        elementType << EntityType.ELEMENT_TYPES*.singularTerm
    }

    def "#elementType custom aspects are validated"() {
        given:
        createElementTypeDefinition(elementType)
        def element = [
            name: "JSON schema validation test subject",
            subType: "A",
            status: "A1",
            owner: [targetUri: "http://localhost/units/..."],
            customAspects: [
                CA: [
                    foo: "ok"
                ]
            ]
        ]

        expect:
        validate(element, elementType).empty

        when:
        element.customAspects.CA.foo = 5
        element.customAspects.NA = [
            foo: "no way"
        ]

        then:
        validate(element, elementType)*.message ==~ [
            "\$.customAspects.CA.foo: integer found, string expected",
            "\$.customAspects.NA: is not defined in the schema and the schema does not allow additional properties",
        ]

        where:
        elementType << EntityType.ELEMENT_TYPES*.singularTerm
    }

    def "#elementType link attribute values are validated"() {
        given:
        createElementTypeDefinition(elementType)
        def element = [
            name: "JSON schema validation test subject",
            subType: "A",
            status: "A1",
            owner: [targetUri: "http://localhost/units/..."],
            links: [
                LA: [
                    [
                        target: [targetUri: "http:/localhost/persons/..."],
                        attributes: [
                            goo: "ok"
                        ]
                    ]
                ]
            ]
        ]

        expect:
        validate(element, elementType).empty

        when: "using an invalid attribute type"
        element.links.LA[0].attributes.goo = 5

        then:
        validate(element, elementType)*.message ==~ [
            "\$.links.LA[0].attributes.goo: integer found, string expected"
        ]

        where:
        elementType << EntityType.ELEMENT_TYPES*.singularTerm
    }

    def "#elementType potential impact is validated"() {
        given:
        createElementTypeDefinition(elementType)
        def element = [
            name: "impactful element",
            subType: "A",
            status: "A1",
            owner: [targetUri: "http://localhost/units/..."],
            riskValues: [
                noRiskNoFun: [
                    potentialImpacts: [
                        A: 1
                    ]
                ]
            ]
        ]

        expect:
        validate(element, elementType).empty

        when:
        element.riskValues.noRiskNoFun.potentialImpacts.A = 99
        element.riskValues.noRiskNoFun.potentialImpacts.Z = 1

        then:
        validate(element, elementType)*.message ==~ [
            "\$.riskValues.noRiskNoFun.potentialImpacts.A: does not have a value in the enumeration [0, 1]",
            "\$.riskValues.noRiskNoFun.potentialImpacts.Z: is not defined in the schema and the schema does not allow additional properties",
        ]

        where:
        elementType << EntityType.RISK_AFFECTED_TYPES*.singularTerm
    }

    def "control implementation status is validated"() {
        given:
        createElementTypeDefinition("control")
        def element = [
            name: "out ouf control",
            subType: "A",
            status: "A1",
            owner: [targetUri: "http://localhost/units/..."],
            riskValues: [
                noRiskNoFun: [
                    implementationStatus: 1
                ]
            ]
        ]

        expect:
        validate(element, "control").empty

        when:
        element.riskValues.noRiskNoFun.implementationStatus = 99

        then:
        validate(element, "control")*.message ==~ [
            "\$.riskValues.noRiskNoFun.implementationStatus: does not have a value in the enumeration [0, 1]"
        ]
    }

    def "scope risk definition is validated"() {
        given:
        createElementTypeDefinition("scope")
        def element = [
            name: "who can cope with the scope?",
            subType: "A",
            status: "A1",
            owner: [targetUri: "http://localhost/units/..."],
            riskDefinition: "noRiskNoFun"
        ]

        expect:
        validate(element, "scope").empty

        when:
        element.riskDefinition = "riskEverything"

        then:
        validate(element, "scope")*.message ==~ [
            "\$.riskDefinition: does not have a value in the enumeration [noRiskNoFun]"
        ]
    }

    private Set<ValidationMessage> validate(Map element, String elementType) {
        getSchema(domain, elementType).validate(om.valueToTree(element))
    }

    private void createElementTypeDefinition(String elementType) {
        domain.applyElementTypeDefinition(newElementTypeDefinition(domain, elementType) {
            subTypes.A = newSubTypeDefinition {
                statuses = ["A1", "A2"]
            }
            subTypes.B = newSubTypeDefinition {
                statuses = ["B1", "B2"]
            }
            customAspects.CA = newCustomAspectDefinition {
                attributeDefinitions.foo = new TextAttributeDefinition()
            }
            links.LA = new LinkDefinition().tap {
                targetType = "person"
                targetSubType = "P"
                attributeDefinitions.goo = new TextAttributeDefinition()
            }
        })
    }
}
