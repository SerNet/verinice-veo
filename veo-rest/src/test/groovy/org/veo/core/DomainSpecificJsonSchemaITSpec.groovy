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
import org.veo.core.entity.ElementType
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
            })
        }
    }

    def "#elementType.singularTerm sub type and status is validated"() {
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
            '$.subType: does not have a value in the enumeration ["A", "B"]'
        ]

        when: "using a sub type that does not define the current status"
        element.subType = "B"

        then:
        validate(element, elementType)*.message ==~ [
            '$.status: does not have a value in the enumeration ["B1", "B2"]'
        ]

        where:
        elementType << ElementType.values()
    }

    def "#elementType.singularTerm custom aspects are validated"() {
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
            '$.customAspects.CA.foo: integer found, string expected',
            '''$.customAspects: property 'NA' is not defined in the schema and the schema does not allow additional properties''',
        ]

        where:
        elementType << ElementType.values()
    }

    def "#elementType.singularTerm link attribute values are validated"() {
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
            '$.links.LA[0].attributes.goo: integer found, string expected'
        ]

        where:
        elementType << ElementType.values()
    }

    def "#elementType.singularTerm potential impact is validated"() {
        given:
        createElementTypeDefinition(elementType)
        def element = [
            name: "impactful element",
            subType: "A",
            status: "A1",
            owner: [targetUri: "http://localhost/units/..."],
            riskValues: [
                noRiskNoFun: [
                    potentialImpactsCalculated: [
                        A: 0
                    ],
                    potentialImpacts: [
                        A: 1
                    ],
                    potentialImpactsEffective: [
                        A: 1,
                    ],
                    potentialImpactReasons: [
                        A: "impact_reason_distributive"
                    ],
                    potentialImpactEffectiveReasons: [
                        A: "impact_reason_distributive",
                    ],
                    potentialImpactExplanations: [
                        A: "It's the only way"
                    ],
                ]
            ]
        ]

        expect:
        validate(element, elementType).empty

        when:
        element.riskValues.noRiskNoFun.potentialImpacts.A = 99
        element.riskValues.noRiskNoFun.potentialImpactsCalculated.A = 99
        element.riskValues.noRiskNoFun.potentialImpactsEffective.A = 99
        element.riskValues.noRiskNoFun.potentialImpactReasons.A = "wrong_reason"
        element.riskValues.noRiskNoFun.potentialImpactEffectiveReasons.A = "wrong_reason"
        element.riskValues.noRiskNoFun.potentialImpactExplanations.A = 42
        element.riskValues.noRiskNoFun.potentialImpacts.Z = 1
        element.riskValues.noRiskNoFun.potentialImpactsCalculated.Z = 0
        element.riskValues.noRiskNoFun.potentialImpactsEffective.Z = 1
        element.riskValues.noRiskNoFun.potentialImpactReasons.Z = "impact_reason_manual"
        element.riskValues.noRiskNoFun.potentialImpactEffectiveReasons.Z = "impact_reason_manual"
        element.riskValues.noRiskNoFun.potentialImpactExplanations.Z = "Manuel said so"

        then:
        validate(element, elementType)*.message ==~ [
            '$.riskValues.noRiskNoFun.potentialImpacts.A: does not have a value in the enumeration [0, 1]',
            '$.riskValues.noRiskNoFun.potentialImpactsCalculated.A: does not have a value in the enumeration [0, 1]',
            '$.riskValues.noRiskNoFun.potentialImpactsEffective.A: does not have a value in the enumeration [0, 1]',
            '$.riskValues.noRiskNoFun.potentialImpactReasons.A: does not have a value in the enumeration ["impact_reason_cumulative", "impact_reason_distributive", "impact_reason_manual"]',
            '$.riskValues.noRiskNoFun.potentialImpactEffectiveReasons.A: does not have a value in the enumeration ["impact_reason_cumulative", "impact_reason_distributive", "impact_reason_manual", "impact_method_high_water_mark"]',
            '$.riskValues.noRiskNoFun.potentialImpactExplanations.A: integer found, string expected',
            '''$.riskValues.noRiskNoFun.potentialImpacts: property 'Z' is not defined in the schema and the schema does not allow additional properties''',
            '''$.riskValues.noRiskNoFun.potentialImpactsCalculated: property 'Z' is not defined in the schema and the schema does not allow additional properties''',
            '''$.riskValues.noRiskNoFun.potentialImpactsEffective: property 'Z' is not defined in the schema and the schema does not allow additional properties''',
            '''$.riskValues.noRiskNoFun.potentialImpactReasons: property 'Z' is not defined in the schema and the schema does not allow additional properties''',
            '''$.riskValues.noRiskNoFun.potentialImpactEffectiveReasons: property 'Z' is not defined in the schema and the schema does not allow additional properties''',
            '''$.riskValues.noRiskNoFun.potentialImpactExplanations: property 'Z' is not defined in the schema and the schema does not allow additional properties''',
        ]

        where:
        elementType << ElementType.RISK_AFFECTED_TYPES
    }

    def "scope risk definition is validated"() {
        given:
        createElementTypeDefinition(ElementType.SCOPE)
        def element = [
            name: "who can cope with the scope?",
            subType: "A",
            status: "A1",
            owner: [targetUri: "http://localhost/units/..."],
            riskDefinition: "noRiskNoFun"
        ]

        expect:
        validate(element, ElementType.SCOPE).empty

        when:
        element.riskDefinition = "riskEverything"

        then:
        validate(element, ElementType.SCOPE)*.message ==~ [
            '$.riskDefinition: does not have a value in the enumeration ["noRiskNoFun"]'
        ]
    }

    def "scope risk definition is validated without any risk definitions in the domain"() {
        given: "no risk definitions at all"
        domain.setRiskDefinitions([:])

        and:
        createElementTypeDefinition(ElementType.SCOPE)
        def element = [
            name: "risky scope",
            subType: "A",
            status: "A1",
            owner: [targetUri: "http://localhost/units/..."],
        ]

        expect:
        validate(element, ElementType.SCOPE).empty

        when: "a risk definition is used anyway"
        element.riskDefinition = 'illegal'

        // TODO #3211 ban additional properties

        then: "it is tolerated for now"
        validate(element, ElementType.SCOPE)*.message ==~ []
    }

    def "essential properties are required for #type.pluralTerm"() {
        given:
        createElementTypeDefinition(type)
        def element = [
            links: [
                LA: [
                    [:]
                ]
            ]
        ]

        expect:
        validate(element, type)*.message ==~ [
            '''$: required property 'name' not found''',
            '''$: required property 'subType' not found''',
            '''$: required property 'status' not found''',
            '''$: required property 'owner' not found''',
            '''$.links.LA[0]: required property 'target' not found''',
        ]

        when:
        element.name = null
        element.subType = null
        element.status = null
        element.owner = null
        element.links.LA[0].target = null

        then:
        validate(element, type)*.message ==~ [
            '$.name: null found, string expected',
            '$.owner: null found, object expected',
            '$.status: null found, string expected',
            '$.status: does not have a value in the enumeration ["A1", "A2", "B1", "B2"]',
            '$.subType: null found, string expected',
            '$.subType: does not have a value in the enumeration ["A", "B"]',
            '$.links.LA[0].target: null found, object expected',
        ]

        where:
        type << ElementType.values()
    }

    def "potential impact maps are not required for #type.pluralTerm"() {
        given:
        createElementTypeDefinition(type)
        def element = [
            name: "impactless",
            subType: "A",
            status: "A1",
            owner: [targetUri: "http://localhost/units/..."],
            riskValues: [
                noRiskNoFun: [:]
            ]
        ]

        expect:
        validate(element, type).empty

        when:
        element.riskValues.noRiskNoFun.potentialImpacts = null
        element.riskValues.noRiskNoFun.potentialImpactReasons = null

        then:
        validate(element, type)*.message ==~ [
            '$.riskValues.noRiskNoFun.potentialImpacts: null found, object expected',
            '$.riskValues.noRiskNoFun.potentialImpactReasons: null found, object expected',
        ]

        where:
        type << ElementType.RISK_AFFECTED_TYPES
    }

    private Set<ValidationMessage> validate(Map element, ElementType elementType) {
        getSchema(domain, elementType).validate(om.valueToTree(element))
    }

    private void createElementTypeDefinition(ElementType elementType) {
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
                targetType = ElementType.PERSON
                targetSubType = "P"
                attributeDefinitions.goo = new TextAttributeDefinition()
            }
        })
    }
}
