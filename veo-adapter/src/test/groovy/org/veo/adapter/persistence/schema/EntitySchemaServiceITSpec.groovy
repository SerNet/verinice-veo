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
package org.veo.adapter.persistence.schema

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.Schema
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import com.networknt.schema.dialect.DialectId

import org.veo.categories.MapGetProperties
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.definitions.ControlImplementationDefinition
import org.veo.core.entity.definitions.CustomAspectDefinition
import org.veo.core.entity.definitions.ElementTypeDefinition
import org.veo.core.entity.definitions.LinkDefinition
import org.veo.core.entity.definitions.SubTypeDefinition
import org.veo.core.entity.definitions.attribute.BooleanAttributeDefinition
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.core.entity.riskdefinition.CategoryDefinition
import org.veo.core.entity.riskdefinition.CategoryLevel
import org.veo.core.entity.riskdefinition.ImplementationStateDefinition
import org.veo.core.entity.riskdefinition.ProbabilityDefinition
import org.veo.core.entity.riskdefinition.ProbabilityLevel
import org.veo.core.entity.riskdefinition.RiskDefinition
import org.veo.core.service.EntitySchemaService

import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.util.mop.Use
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

/**
 * Tests {@link EntitySchemaService} , {@link EntitySchemaGenerator} & {@link SchemaExtender} by letting them produce
 * schemas for two test domains and verifying the produced schemas.
 */
class EntitySchemaServiceITSpec extends Specification {

    static EntitySchemaService entitySchemaService = new EntitySchemaServiceImpl(new EntitySchemaGenerator(new SchemaExtender(), new ObjectMapper()))
    public static final String PROPS = "properties"

    def "entity schema is a valid schema"() {
        given:
        def schema201909 = getMetaSchemaV2019_09()
        def schema = getSchema(Set.of(getTestDomain()),  ElementType.ASSET)

        expect:
        schema201909.validate(schema).empty
    }

    def "schema for a single domain allows other domains"() {
        given:
        def schema = getSchema(Set.of(testDomain), ElementType.ASSET)

        expect:
        with(schema.get(PROPS).get("domains")) {d->
            d.get(EntitySchemaServiceITSpec.PROPS).size() == 1
            // additional properties are allowed by default
            additionalProperties == null
        }
    }

    def "designator is marked read-only in entity schema #schema.title"() {
        expect:
        def schema = getSchema(Set.of(getTestDomain()), ElementType.ASSET)
        schema.get(PROPS).get("designator").get("readOnly").booleanValue()
    }

    def "_self is marked read-only in entity schema #schema.title"() {
        given:
        def schema = getSchema(Set.of(getTestDomain()), ElementType.ASSET)

        expect:
        schema.get(PROPS).get("_self").get("readOnly").booleanValue()
    }

    def "scenario schema domain association is complete"() {
        given:
        def testDomain = getTestDomain()

        when:
        def schema = getSchema(Set.of(testDomain), ElementType.SCENARIO)

        then:
        def riskValueProps = schema.get(PROPS).get("domains").get(PROPS).get(testDomain.idAsString).get(PROPS).get("riskValues").get(PROPS)
        riskValueProps.get("riskDefA").get(PROPS).get("potentialProbability").get("enum").asList()*.asInt() == [0, 1, 2]
        riskValueProps.get("riskDefB").get(PROPS).get("potentialProbability").get("enum").asList()*.asInt() == [0, 1]
    }

    def "scope schema domain association is complete"() {
        given:
        def testDomain = getTestDomain()
        def extraTestDomain = getExtraTestDomain()

        when:
        def schema = getSchema(Set.of(testDomain, extraTestDomain), ElementType.SCOPE)

        then:
        schema.get(PROPS).domains.get(PROPS).get(testDomain.idAsString).get(PROPS).riskDefinition.enum*.asString() ==~ ["riskDefA", "riskDefB"]
        schema.get(PROPS).domains.get(PROPS).get(extraTestDomain.idAsString).get(PROPS).riskDefinition.enum*.asString() ==~ ["extraRiskDef"]
    }

    @Use(MapGetProperties)
    def "process schema domain association is complete"() {
        given:
        def testDomain = getTestDomain()

        when:
        def schema = new JsonSlurper().parseText(entitySchemaService.getSchema(ElementType.PROCESS, Set.of(testDomain)))

        then:
        def riskValueProps = schema.properties.domains.properties.(testDomain.idAsString).properties.riskValues.properties
        riskValueProps.riskDefA.properties.potentialImpacts.properties.C.enum ==~ [0, 1]
    }

    def "definitions from multiple domains are composed"() {
        given:
        def testDomain = getTestDomain()
        def extraTestDomain = getExtraTestDomain()
        def schema = getSchema(Set.of(testDomain, extraTestDomain), ElementType.ASSET)
        // we cannot access the constant from within the `with` https://issues.apache.org/jira/browse/GROOVY-10604
        def p = PROPS

        expect:
        with(schema.get(PROPS).get("customAspects").get(PROPS)) { ap->
            ap.get("test").get(p).get("attributes").get(p).get("testAttr").get("type").asString() == "string"
            ap.get("extraTest").get(p).get("attributes").get(p).get("extraTestAttr").get("type").asString() == "boolean"
        }
        with(schema.get(PROPS).get("links").get(PROPS)) {
            get("test").get("items").get(p).get("attributes").get(p).get("linkTestAttr").get("type").asString() == "string"
            get("test").get("items").get(p).get("target").get(p).get("type").get("enum")*.asString() == ["process"]
            get("test").get("items").get(p).get("target").get(p).get("subType").get("enum")*.asString() == ["otherSubType"]

            get("extraTest").get("items").get(p).get("attributes").get(p).get("extraLinkTestAttr").get("type").asString() == "integer"
            get("extraTest").get("items").get(p).get("target").get(p).get("type").get("enum")*.asString() == ["scenario"]
            get("extraTest").get("items").get(p).get("target").get(p).get("subType").get("enum")*.asString() == ["extraSubType"]
        }
        with(schema.get(PROPS).get("domains").get(PROPS)) {
            get(testDomain.idAsString).get(p).get("subType").get("enum")*.asString() == ["testSubType"]
            get(testDomain.idAsString).get(p).get("status").get("enum")*.asString() ==~ ["NEW", "OLD"]
            get(testDomain.idAsString).get("allOf").first().get("if").get(p).get("subType").get("const").asString() == "testSubType"
            get(testDomain.idAsString).get("allOf").first().get("then").get(p).get("status").get("enum")*.asString() == ["NEW", "OLD"]

            with(get(extraTestDomain.idAsString)) {
                get(p).get("subType").get("enum")*.asString() ==~ [
                    "extraTestSubType",
                    "extraSuperSubType"
                ]
                get(p).get("status").get("enum")*.asString() ==~ [
                    "EXTRA_NEW",
                    "EXTRA_OLD",
                    "SUPER_NEW",
                    "SUPER_OLD"
                ]

                with(get("allOf")) { JsonNode allOf ->
                    allOf.size() == 2
                    with(allOf.find { it.get("if").get(p).get("subType").get("const").asString() == "extraTestSubType" }) {
                        get("then").get(p).get("status").get("enum")*.asString() ==~ ["EXTRA_NEW", "EXTRA_OLD"]
                    }
                    with(allOf.find { it.get("if").get(p).get("subType").get("const").asString() == "extraSuperSubType" }) {
                        get("then").get(p).get("status").get("enum")*.asString() ==~ ["SUPER_NEW", "SUPER_OLD"]
                    }
                }
            }
        }
    }

    def "CI CAs are included"() {
        given:
        def assetSchema = getSchema(testDomain, ElementType.ASSET)
        def scopeSchema = getSchema(testDomain, ElementType.SCOPE)
        def processSchema = getSchema(testDomain, ElementType.PROCESS)
        def controlSchema = getSchema(testDomain, ElementType.CONTROL)
        // we cannot access the constant from within the `with` https://issues.apache.org/jira/browse/GROOVY-10604
        def p = PROPS

        expect:
        with(
                assetSchema.get(p).get("controlImplementations").get("items")) {
                    with(it.get(p).get("customAspects").get(p)) {
                        it.get("assetCiTest").get(p).get("assetCiTestAttr").get("type").asString() == "string"
                        !it.has("scopeCiTest")
                    }
                }
        with(
                scopeSchema.get(p).get("controlImplementations").get("items")) {
                    with(it.get(p).get("customAspects").get(p)) {
                        it.get("scopeCiTest").get(p).get("scopeCiTestAttr").get("type").asString() == "boolean"
                        !it.has("assetCiTest")
                    }
                }
        with(
                processSchema.get(p).get("controlImplementations").get("items")) {
                    !it.get(p).has("customAspects")
                }
        !controlSchema.get(p).has("controlImplementations")
    }

    private static Schema getMetaSchemaV2019_09() throws IOException {
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2019_09)
                .getSchema(SchemaLocation.of(DialectId.DRAFT_2019_09))
    }

    private static JsonNode getSchema(Set<Domain> domains, ElementType type) {
        JsonMapper.shared().readTree(entitySchemaService.getSchema(type, domains))
    }

    private static JsonNode getSchema(Domain domain, ElementType type) {
        JsonMapper.shared().readTree(entitySchemaService.getSchema(type, domain))
    }

    private Domain getTestDomain() {
        return Mock(Domain) {
            idAsString >> UUID.randomUUID()
            getElementTypeDefinition(ElementType.ASSET) >> Mock(ElementTypeDefinition) {
                customAspects >> [
                    test: Mock(CustomAspectDefinition) {
                        attributeDefinitions >> [
                            testAttr: new TextAttributeDefinition()
                        ]
                    }
                ]
                links >> [
                    test: Mock(LinkDefinition) {
                        attributeDefinitions >> [
                            linkTestAttr: new TextAttributeDefinition()
                        ]
                        targetType >> ElementType.PROCESS
                        targetSubType >> "otherSubType"
                    }
                ]
                subTypes >> [
                    testSubType: Mock(SubTypeDefinition) {
                        statuses >> ["NEW", "OLD"]
                    }
                ]
                controlImplementationDefinition >> Mock(ControlImplementationDefinition) {
                    customAspects >> [
                        assetCiTest: Mock(CustomAspectDefinition) {
                            attributeDefinitions >> [
                                assetCiTestAttr: new TextAttributeDefinition()
                            ]
                        }
                    ]
                }
            }
            getElementTypeDefinition(ElementType.CONTROL) >> Mock(ElementTypeDefinition) {
                customAspects >> [:]
                links >> [:]
                subTypes >> [
                    subControl: Mock(SubTypeDefinition) {
                        statuses >> ["NEW", "OLD"]
                    }
                ]
            }
            getElementTypeDefinition(ElementType.SCENARIO) >> Mock(ElementTypeDefinition) {
                customAspects >> [:]
                links >> [:]
                subTypes >> [
                    subScenario: Mock(SubTypeDefinition) {
                        statuses >> [
                            "IN_PROGRESS",
                            "NEW",
                            "RELEASED",
                            "FOR_REVIEW",
                            "ARCHIVED"
                        ]
                    }
                ]
            }
            getElementTypeDefinition(ElementType.SCOPE) >> Mock(ElementTypeDefinition) {
                customAspects >> [:]
                links >> [:]
                subTypes >> [
                    subScope: Mock(SubTypeDefinition) {
                        statuses >> ["NEW", "OLD"]
                    }
                ]
                controlImplementationDefinition >> Mock(ControlImplementationDefinition) {
                    customAspects >> [
                        scopeCiTest: Mock(CustomAspectDefinition) {
                            attributeDefinitions >> [
                                scopeCiTestAttr: new BooleanAttributeDefinition()
                            ]
                        }
                    ]
                }
            }
            getElementTypeDefinition(ElementType.PROCESS) >> Mock(ElementTypeDefinition) {
                customAspects >> [:]
                links >> [:]
                subTypes >> [
                    subControl: Mock(SubTypeDefinition) {
                        statuses >> ["NEW", "OLD"]
                    }
                ]
            }
            getRiskDefinitions() >> [
                "riskDefA": Mock(RiskDefinition) {
                    categories >> [
                        Mock(CategoryDefinition) {
                            id >> 'C'
                            potentialImpacts >> [
                                Mock(CategoryLevel) {
                                    ordinalValue >> 0
                                },
                                Mock(CategoryLevel) {
                                    ordinalValue >> 1
                                }
                            ]
                        }
                    ]
                    implementationStateDefinition >> Mock(ImplementationStateDefinition) {
                        levels >> [
                            Mock(CategoryLevel) {
                                ordinalValue >> 0
                            },
                            Mock(CategoryLevel) {
                                ordinalValue >> 1
                            },
                            Mock(CategoryLevel) {
                                ordinalValue >> 2
                            },
                        ]
                    }
                    probability >> Mock(ProbabilityDefinition) {
                        levels >> [
                            Mock(ProbabilityLevel) {
                                ordinalValue >> 0
                            },
                            Mock(ProbabilityLevel) {
                                ordinalValue >> 1
                            },
                            Mock(ProbabilityLevel) {
                                ordinalValue >> 2
                            },
                        ]
                    }
                },
                "riskDefB": Mock(RiskDefinition) {
                    categories >> []
                    implementationStateDefinition >> Mock(ImplementationStateDefinition) {
                        levels >> [
                            Mock(CategoryLevel) {
                                ordinalValue >> 0
                            },
                            Mock(CategoryLevel) {
                                ordinalValue >> 1
                            },
                        ]
                    }
                    probability >> Mock(ProbabilityDefinition) {
                        levels >> [
                            Mock(ProbabilityLevel) {
                                ordinalValue >> 0
                            },
                            Mock(ProbabilityLevel) {
                                ordinalValue >> 1
                            },
                        ]
                    }
                },
            ]
        }
    }

    private Domain getExtraTestDomain() {
        return Mock(Domain) {
            idAsString >> UUID.randomUUID()
            getElementTypeDefinition(ElementType.ASSET) >> Mock(ElementTypeDefinition) {
                customAspects >> [
                    extraTest: Mock(CustomAspectDefinition) {
                        attributeDefinitions >> [
                            extraTestAttr: new BooleanAttributeDefinition()
                        ]
                    }
                ]
                links >> [
                    extraTest: Mock(LinkDefinition) {
                        attributeDefinitions >> [
                            extraLinkTestAttr: new IntegerAttributeDefinition()
                        ]
                        targetType >> ElementType.SCENARIO
                        targetSubType >> "extraSubType"
                    }
                ]
                subTypes >> [
                    extraTestSubType: Mock(SubTypeDefinition) {
                        statuses >> ["EXTRA_NEW", "EXTRA_OLD"]
                    },
                    extraSuperSubType: Mock(SubTypeDefinition) {
                        statuses >> ["SUPER_NEW", "SUPER_OLD"]
                    },
                ]
            }
            getElementTypeDefinition(ElementType.SCOPE) >> Mock(ElementTypeDefinition) {
                customAspects >> [:]
                links >> [:]
                subTypes >> [:]
            }
            getRiskDefinitions() >> [
                "extraRiskDef": Mock(RiskDefinition) {
                    categories >> []
                }
            ]
        }
    }
}
