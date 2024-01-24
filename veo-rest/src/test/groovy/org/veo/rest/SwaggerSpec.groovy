/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.rest

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc

import com.fasterxml.jackson.annotation.JsonSubTypes

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.EntityType
import org.veo.core.entity.definitions.attribute.AttributeDefinition

import groovy.json.JsonSlurper
import groovy.transform.Memoized

@AutoConfigureMockMvc
class SwaggerSpec extends VeoSpringSpec {

    @Autowired
    private MockMvc mvc

    def "Swagger documentation is available"() {
        when:
        def response = mvc.perform(get('/swagger-ui.html')).andReturn().response
        def redirectedUrl = response.redirectedUrl

        then:
        redirectedUrl != null

        when:
        response = mvc.perform(get(redirectedUrl)).andReturn().response

        then:
        response.contentAsString.contains('Swagger UI')

        and:
        apiDocsString.contains('verinice.VEO')
    }

    def "Swagger index page does not load Petstore API"() {
        when:
        def response = mvc.perform(get('/swagger-ui/index.html')).andReturn().response
        def pageContent = response.contentAsString

        then:
        pageContent.contains('Swagger UI')
        !pageContent.contains('petstore')
    }

    def "response DTO contains links property"() {
        when:
        def assetDtoSchema = parsedApiDocs.components.schemas.FullAssetDto

        then:
        assetDtoSchema.properties.links != null
        assetDtoSchema.properties.links.description == 'The links for the asset.'
    }

    def "operation documentation is complete"() {
        given:
        def ops = parsedApiDocs
                .paths
                .entrySet()
                .collectMany{path ->
                    path.value.entrySet().collect{
                        [
                            path: path.key,
                            method: it.key,
                            documentation: it.value
                        ]
                    }
                }

        expect:
        ops.forEach{
            assert it.documentation.summary != null
        }
    }

    def "createdAt and updatedAt are read-only"() {
        when:
        def assetDtoSchema = parsedApiDocs.components.schemas.FullAssetDto

        then:
        assetDtoSchema.properties.createdAt != null
        assetDtoSchema.properties.createdAt.readOnly == true
        assetDtoSchema.properties.createdAt != null
        assetDtoSchema.properties.updatedAt.readOnly == true
    }

    def "displayName is not required for parts when putting composite elements"() {
        when:
        def scenarioDtoSchema = parsedApiDocs.components.schemas.FullScenarioDto

        then:
        scenarioDtoSchema.properties.parts != null
        scenarioDtoSchema.properties.parts.type == 'array'
        scenarioDtoSchema.properties.parts.items != null
        scenarioDtoSchema.properties.parts.items.'$ref' == '#/components/schemas/PartReference'

        when:
        def partReferenceSchema = parsedApiDocs.components.schemas.PartReference

        then:
        !partReferenceSchema.required.contains('displayName')
    }

    def "domain association schemas contain the expected fields"() {
        given:
        def schemas = parsedApiDocs.components.schemas

        expect:
        // TODO #2542 in the legacy DTO schema, the property list must remain the same. But in the new DTO schema, 'customAspects' and
        //  'links' must also be present
        def domainAssociationSchema = schemas.DomainAssociationDto
        domainAssociationSchema.properties.keySet() ==~ [
            'subType',
            'status',
            'decisionResults'
        ]
    }

    def "control risk values are mapped correctly"() {
        given:
        def schemas = parsedApiDocs.components.schemas

        expect:
        def controlSchema = schemas.FullControlDto
        controlSchema.properties.domains.additionalProperties.'$ref' == "#/components/schemas/ControlDomainAssociationDto"

        def controlDomainAssociationSchema = schemas.ControlDomainAssociationDto
        controlDomainAssociationSchema.properties.riskValues.additionalProperties.'$ref' == "#/components/schemas/ControlRiskValuesDto"

        // TODO #2542 in the legacy DTO schema, the property list must remain the same. But in the new DTO schema, 'customAspects' and
        //  'links' must also be present
        controlDomainAssociationSchema.properties.keySet() ==~ [
            'subType',
            'status',
            'decisionResults',
            'riskValues',
        ]

        def controlRiskValuesSchema = schemas.ControlRiskValuesDto
        controlRiskValuesSchema.properties.implementationStatus.type == "integer"
    }

    def "scenario risk values are mapped correctly"() {
        given:
        def schemas = parsedApiDocs.components.schemas

        expect:
        def scenarioSchema = schemas.FullScenarioDto
        scenarioSchema.properties.domains.additionalProperties.'$ref' == "#/components/schemas/ScenarioDomainAssociationDto"

        def scenarioDomainAssociationSchema = schemas.ScenarioDomainAssociationDto
        scenarioDomainAssociationSchema.properties.riskValues.additionalProperties.'$ref' == "#/components/schemas/ScenarioRiskValuesDto"

        // TODO #2542 in the legacy DTO schema, the property list must remain the same. But in the new DTO schema, 'customAspects' and
        //  'links' must also be present
        scenarioDomainAssociationSchema.properties.keySet() ==~ [
            'subType',
            'status',
            'decisionResults',
            'riskValues',
        ]

        def scenarioRiskValuesSchema = schemas.ScenarioRiskValuesDto
        scenarioRiskValuesSchema.properties.potentialProbability.type == "number"
    }

    def "scope risk values are mapped correctly"() {
        given:
        def schemas = parsedApiDocs.components.schemas

        expect:
        def scopeSchema = schemas.FullScopeDto
        scopeSchema.properties.domains.additionalProperties.'$ref' == "#/components/schemas/ScopeDomainAssociationDto"

        def scopeDomainAssociationSchema = schemas.ScopeDomainAssociationDto
        scopeDomainAssociationSchema.properties.riskDefinition.type == "string"

        // TODO #2542 in the legacy DTO schema, the property list must remain the same. But in the new DTO schema, 'customAspects' and
        //  'links' must also be present
        scopeDomainAssociationSchema.properties.keySet() ==~ [
            'subType',
            'status',
            'decisionResults',
            'riskDefinition',
            'riskValues',
        ]
    }

    def "process risk values are mapped correctly"() {
        given:
        def schemas = parsedApiDocs.components.schemas

        expect:
        def processSchema = schemas.FullProcessDto
        processSchema.properties.domains.additionalProperties.'$ref' == "#/components/schemas/ProcessDomainAssociationDto"

        def processDomainAssociationSchema = schemas.ProcessDomainAssociationDto
        processDomainAssociationSchema.description == '''Details about this element's association with domains. Domain ID is key, association object is value.'''
        processDomainAssociationSchema.properties.riskValues.additionalProperties.'$ref' == "#/components/schemas/ImpactValuesDto"

        // TODO #2542 in the legacy DTO schema, the property list must remain the same. But in the new DTO schema, 'customAspects' and
        //  'links' must also be present
        processDomainAssociationSchema.properties.keySet() ==~ [
            'subType',
            'status',
            'decisionResults',
            'riskValues',
        ]

        def processImpactValuesSchema = schemas.ImpactValuesDto
        processImpactValuesSchema.description == '''Key is risk definition ID, value contains impact values in the context of that risk definition.'''
        def potentialImpactsSchema = processImpactValuesSchema.properties.potentialImpacts
        potentialImpactsSchema.type == "object"
        potentialImpactsSchema.description == "Potential impacts for a set of risk categories. These are specific values entered by the user directly."
        potentialImpactsSchema.example == [C:2, I:3]
    }

    def "asset risk values are mapped correctly"() {
        given:
        def schemas = parsedApiDocs.components.schemas

        expect:
        def assetSchema = schemas.FullAssetDto
        assetSchema.properties.domains.additionalProperties.'$ref' == "#/components/schemas/AssetDomainAssociationDto"

        def assetDomainAssociationSchema = schemas.AssetDomainAssociationDto
        assetDomainAssociationSchema.description == '''Details about this element's association with domains. Domain ID is key, association object is value.'''
        assetDomainAssociationSchema.properties.riskValues.additionalProperties.'$ref' == "#/components/schemas/ImpactValuesDto"

        // TODO #2542 in the legacy DTO schema, the property list must remain the same. But in the new DTO schema, 'customAspects' and
        //  'links' must also be present
        assetDomainAssociationSchema.properties.keySet() ==~ [
            'subType',
            'status',
            'decisionResults',
            'riskValues',
        ]

        with(schemas.ImpactValuesDto) {
            description == "Key is risk definition ID, value contains impact values in the context of that risk definition."
            with(properties) {
                with(potentialImpacts) {
                    type == "object"
                    description == "Potential impacts for a set of risk categories. These are specific values entered by the user directly."
                    example == [C:2, I:3]
                }
                with(potentialImpactsCalculated) {
                    type == "object"
                    description == "Potential impacts for a set of risk categories. These are calculated values based on the high water mark."
                }
                with(potentialImpactsEffective) {
                    type == "object"
                    description == "Potential impacts for a set of risk categories. These are either the specific or the calculated values."
                }
                with(potentialImpactReasons) {
                    type == "object"
                    description == "An optional reason for the chosen specific potential impact in each category."
                }
                with(potentialImpactExplanations) {
                    type == "object"
                    description == "An optional explanation for the user-entered specific potential impact."
                }
            }
        }
    }

    def "decision rule ref values are mapped correctly"() {
        given:
        def schemas = parsedApiDocs.components.schemas

        expect:
        def scopeDomainAssociationSchema = schemas.ScopeDomainAssociationDto
        scopeDomainAssociationSchema.properties.decisionResults.additionalProperties.'$ref' == "#/components/schemas/DecisionResultsSchema"

        def decisionResultsSchema = schemas.DecisionResultsSchema
        decisionResultsSchema.properties.decisiveRule.type == "integer"
        decisionResultsSchema.properties.agreeingRules.items.type == "integer"
    }

    def "targetUri is required for parts when putting composite elements"() {
        when:
        def scenarioDtoSchema = parsedApiDocs.components.schemas.FullScenarioDto

        then:
        scenarioDtoSchema.properties.parts != null
        scenarioDtoSchema.properties.parts.type == 'array'
        scenarioDtoSchema.properties.parts.items != null
        scenarioDtoSchema.properties.parts.items.'$ref' == '#/components/schemas/PartReference'

        when:
        def partReferenceSchema = parsedApiDocs.components.schemas.PartReference

        then:
        partReferenceSchema.required.contains('targetUri')
    }

    def "link attributes are not required"() {
        given:
        def customLinkDtoSchema = parsedApiDocs.components.schemas.CustomLinkDto

        expect:
        !customLinkDtoSchema.required.contains("attributes")
    }

    def "targetUri is required for scope owner"() {
        when:
        def scopeDtoSchema = parsedApiDocs.components.schemas.FullScopeDto

        then:
        scopeDtoSchema.properties.owner.'$ref' == '#/components/schemas/OwnerReference'

        when:
        def ownerReferenceSchema = parsedApiDocs.components.schemas.OwnerReference

        then:
        ownerReferenceSchema.required.contains('targetUri')
    }

    def "catalog item element is a reference"() {
        when:
        def catalogItemDtoSchema = parsedApiDocs.components.schemas.LegacyCatalogItemDto

        then:
        catalogItemDtoSchema.properties.element.'$ref' == '#/components/schemas/CatalogItemElement'

        when:
        def elementReferenceSchema = parsedApiDocs.components.schemas.CatalogItemElement

        then:
        elementReferenceSchema.required.contains('targetUri')
    }

    def "allowed entity schema types are listed"() {
        given: "existing entity types"
        def schemaTypes = EntityType.ELEMENT_TYPES
                .collect{it.singularTerm}
                .sort()

        when: "fetching allowed schemas from OpenAPI parameter doc"
        List<String> allowedTypes = parsedApiDocs.paths["/schemas/{type}"].get.parameters[0].schema.enum

        then: "they also contain all entity types"
        allowedTypes.sort() == schemaTypes
    }

    def "endpoint documentation is correct for CreateAssetUseCase"() {
        when: "retrieving the endpoint docs"
        def endPointInfo = parsedApiDocs.paths["/assets"].post

        then: "it contains a summary and a parameter schema"
        endPointInfo != null
        endPointInfo.summary == "Creates an asset"
        with(endPointInfo.parameters[0]) {
            name == "scopes"
            schema.type == "array"
            schema.items.type == "string"
        }
    }

    def "endpoint documentation is correct for GetIncarnationDescriptionUseCase"() {
        when: "retrieving the information about the endpoint"
        def endPointInfo = parsedApiDocs.paths["/units/{unitId}/incarnations"]

        then: "the information is found"
        endPointInfo != null

        and: 'it handles get requests'
        endPointInfo.get != null

        and: 'it contains information about the query parameters'
        with(endPointInfo.get.parameters[1]) {
            name == 'itemIds'
            it.in == 'query'
            required == true
            with(schema) {
                type == 'array'
                items == ['type': 'string']
            }
        }
    }

    def "endpoint documentation is correct for translation controller"() {
        when: "retrieving the information about the endpoint"
        def endPointInfo = parsedApiDocs.paths["/translations"]

        then: "the information is found"
        endPointInfo != null

        and: 'it handles get requests'
        endPointInfo.get != null

        and: 'it contains information about the query parameters'
        with(endPointInfo.get) {
            summary == 'Retrieves a map of UI translation key-value pairs.'
        }

        when:
        def translationsSchema = parsedApiDocs.components.schemas.TranslationsDto

        then:
        with(translationsSchema) {
            it.description == 'Translations for an entity type'
            it.properties.containsKey('lang')
            with (it.properties.lang) {
                it.description == 'The keys are language codes, the values are the translations'
                it.additionalProperties.'$ref' == '#/components/schemas/TranslationDto'
            }
        }

        when:
        def translationSchema = parsedApiDocs.components.schemas.TranslationDto

        then:
        with(translationSchema) {
            it.description == 'Translations for an entity type in a specific language'
        }
    }

    def "endpoint documentation is correct for GET /domains/{id}/catalog-items/type-count"() {
        given: "the endpoint docs"
        def endPointInfo = parsedApiDocs.paths["/domains/{id}/catalog-items/type-count"].get

        expect: "that the correct schema is used"
        endPointInfo.responses['200'].content['application/json'].schema['$ref'] == '#/components/schemas/CatalogItemsTypeCountSchema'

        when:
        def schema = parsedApiDocs.components.schemas.CatalogItemsTypeCountSchema

        then:
        schema.readOnly
        schema.example instanceof Map
    }

    def "endpoint documentation is correct for GET /domain-templates"() {
        given: "the endpoint docs"
        def endPointInfo = parsedApiDocs.paths["/domain-templates"].get

        expect: "that the correct schema is used"
        endPointInfo.responses['200'].content['application/json'].schema['$ref'] == '#/components/schemas/DomainTemplateMetadataDto'

        and: "that the schema only contains metadata"
        with(parsedApiDocs.components.schemas.DomainTemplateMetadataDto) {
            properties.name != null
            properties.riskDefinitions == null
        }
    }

    def "attribute definition types are mapped"() {
        given: "correct map of sub types extracted from the jackson annotation"
        def expectedSubTypeMap = AttributeDefinition
                .getAnnotation(JsonSubTypes)
                .value()
                .collectEntries {
                    [
                        it.name(),
                        "#/components/schemas/${it.value().simpleName}"
                    ]
                }

        expect: "that the docs contain the expected mapping"
        with(parsedApiDocs.components.schemas.AttributeDefinitionSchema) {
            discriminator.mapping == expectedSubTypeMap
        }
    }

    def "endpoint documentation is correct for CreateDomainUseCase"() {
        when: "retrieving the information about the endpoint"
        def endPointInfo = parsedApiDocs.paths["/domain-templates/{id}/createdomains"]

        then: "the information is found"
        endPointInfo != null

        and: 'it handles post requests'
        endPointInfo.post != null

        and: 'it has a meaningful description'
        endPointInfo.post.summary == 'Creates domains from a domain template'

        and: 'it contains information about the query parameters'
        with(endPointInfo.post.parameters[1]) {
            name == 'clientids'
            it.in == 'query'
            required == false
            with(schema) {
                type == 'array'
                items == ['type': 'string']
            }
        }
    }

    def "endpoint documentation is correct for ExportDomainUseCase"() {
        when: "retrieving the information about the endpoint"
        def endPointInfo = parsedApiDocs.paths["/domains/{id}/export"]

        then: "the information is found"
        endPointInfo != null

        and: 'it handles get requests'
        endPointInfo.get != null

        and: 'it has a meaningful description'
        endPointInfo.get.summary == 'Export a domain'

        and: 'it contains information about the query parameters'
        with(endPointInfo.get.parameters[0]) {
            name == 'id'
            it.in == 'path'
            required == true
            with(schema) {
                ['type': 'string']
            }
        }
    }

    def "endpoint documentation is correct for CreateDomainTemplateUseCase"() {
        when: "retrieving the information about the endpoint"
        def endPointInfo = parsedApiDocs.paths["/content-creation/domains/{id}/template"]

        then: "the information is found"
        endPointInfo != null

        and: 'it handles post requests'
        endPointInfo.post != null

        and: 'it has a meaningful description'
        endPointInfo.post.summary == 'Creates a domain template from a domain'

        and: 'parameter is described'
        with(endPointInfo.post.parameters[0]) {
            name == 'id'
            required == true
        }
        with(endPointInfo.post.requestBody) {
            required == true
        }
    }

    def "endpoint documentation is correct for GetElementStatusCountUseCase"() {
        when: "retrieving the information about the endpoint"
        def endPointInfo = parsedApiDocs.paths["/domains/{id}/element-status-count"]

        then: "the information is found"
        endPointInfo != null

        and: 'it handles get requests'
        endPointInfo.get != null

        and: 'it has a meaningful description'
        endPointInfo.get.summary == 'Retrieve element counts grouped by subType and status'

        and: 'parameter is described'
        with(endPointInfo.get.parameters[0]) {
            name == 'id'
            it.in == 'path'
            required == true
        }
        with(endPointInfo.get.parameters[1]) {
            name == 'unit'
            it.in == 'query'
            required == true
            description ==~ /UUID of the containing unit.*/
        }
    }

    def "security is configured globally"() {
        expect:
        with(parsedApiDocs.security) {
            size() == 1
            it[0].OAuth2.size() == 0
        }
    }

    @Memoized
    String getApiDocsString() {
        mvc.perform(get('/v3/api-docs')).andReturn().response.contentAsString
    }

    def getParsedApiDocs() {
        new JsonSlurper().parseText(apiDocsString)
    }
}
