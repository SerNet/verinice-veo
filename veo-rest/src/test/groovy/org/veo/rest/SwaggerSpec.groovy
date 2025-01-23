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
                .collectMany{ path ->
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

    def "abstract risk is mapped correctly"() {
        given:
        def riskSchema = parsedApiDocs.components.schemas.AbstractRiskDto

        expect:
        riskSchema.properties.ownerRef == null
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
                    description == "The reason for the chosen user-defined potential impact in each category."
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
        scopeDomainAssociationSchema.properties.decisionResults.additionalProperties.'$ref' == "#/components/schemas/DecisionResult"

        def decisionResultsSchema = schemas.DecisionResult
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

    def "allowed entity schema types are listed"() {
        given: "existing entity types"
        def schemaTypes = EntityType.ELEMENT_TYPES
                .collect{
                    it.singularTerm
                }
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
        def endPointInfo = parsedApiDocs.paths["/units/{unitId}/domains/{domainId}/incarnation-descriptions"]

        then: "the information is found"
        endPointInfo != null

        and: 'it handles get requests'
        endPointInfo.get != null

        and: 'it contains information about the query parameters'
        with(endPointInfo.get.parameters[2]) {
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
        endPointInfo.responses['200'].content['application/json'].schema['$ref'] == '#/components/schemas/CatalogItemsTypeCount'

        when:
        def schema = parsedApiDocs.components.schemas.CatalogItemsTypeCount

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
        with(parsedApiDocs.components.schemas.AttributeDefinition) {
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
                items == ['type': 'string', 'format': 'uuid']
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
            it.schema == [type: 'string']
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

    def "Unit parameter in profile update endpoint is documented"() {
        when:
        def endPointInfo = parsedApiDocs.paths["/content-creation/domains/{domainId}/profiles/{profileId}"]

        then:
        with(endPointInfo.put.parameters.find{
            it.name == 'unit'
        }) {
            description == 'Pass a unit ID to overwrite all items in the profile with new profile items created from the elements in that unit. Omit unit ID to leave current profile items untouched.'
        }
    }

    def "No component schema for #type"() {
        given:
        def componentSchemas = parsedApiDocs.components.schemas

        expect:
        componentSchemas[type] == null

        where:
        type << [
            'MappingJacksonValue',
            'ProbabilityRef',
            'CategoryRef',
            'ImpactRef',
            'RiskDefinitionRef',
            'ImplementationStatusRef',
            'DecisionRef',
            'RiskRef',
            'ReqImplRef'
        ]
    }

    def "Inspection schema is complete"() {
        expect:
        with(getSchema('Inspection')) {
            it.description == '''Dynamic check to be performed on elements. An inspection can find a problem with an element, direct the user's attention to the problem and suggest actions that would fix the problem. An inspection defines a condition and some suggestions. If the inspection is run on an element and the condition is true, the suggestions are presented to the user.'''
            with(it.properties) {
                it.keySet() ==~  [
                    'severity',
                    'description',
                    'elementType',
                    'elementSubType',
                    'condition',
                    'suggestions'
                ]
                with(it.elementType) {
                    it.description == 'Element type (singular term) that this inspection applies to. If this is null, the inspection applies to all element types.'
                    it.maxLength == 32
                }
                it.condition == [$ref:'#/components/schemas/VeoExpression']
                with(it.suggestions) {
                    it.type == 'array'
                    it.items == [
                        oneOf: [
                            [$ref:'#/components/schemas/AddPartSuggestion']
                        ]
                    ]
                }
            }
        }
    }

    def "Suggestion schema is complete"() {
        expect:
        with(getSchema('Suggestion')) {
            it.description == 'Suggests an action to the user that would fix an inspection finding'
            it.properties.keySet() ==~ ['type']
            it.discriminator == [
                propertyName:'type',
                mapping:[
                    addPart:'#/components/schemas/AddPartSuggestion']
            ]
        }
    }

    def "AddPartSuggestion schema is complete"() {
        expect:
        with(getSchema('AddPartSuggestion')) {
            it.description == '''Suggests adding a part to the composite element'''
            it.properties == null
            it.allOf.size() == 2
            with(it.allOf) {
                with(it.find{it.type == 'object'}) {
                    with(it.properties) {
                        it.keySet() ==~ ['partSubType']
                        with(it.partSubType) {
                            it.description == 'Suggested sub type for the new part'
                        }
                    }
                }
                it.find{it.type != 'object'} == [$ref:'#/components/schemas/Suggestion']
            }
        }
    }

    def "AttributeDefinition schema is complete"() {
        expect:
        with(getSchema('AttributeDefinition')) {
            it.description == 'Defines validation rules for an attribute in a custom aspect or link'
            it.discriminator == [
                propertyName:'type',
                mapping:[
                    boolean:'#/components/schemas/BooleanAttributeDefinition',
                    date:'#/components/schemas/DateAttributeDefinition',
                    dateTime:'#/components/schemas/DateTimeAttributeDefinition',
                    enum:'#/components/schemas/EnumAttributeDefinition',
                    externalDocument:'#/components/schemas/ExternalDocumentAttributeDefinition',
                    integer:'#/components/schemas/IntegerAttributeDefinition',
                    list:'#/components/schemas/ListAttributeDefinition',
                    text:'#/components/schemas/TextAttributeDefinition'
                ]
            ]
        }
    }

    def "VeoExpression is well-documented"() {
        expect:
        with(getSchema('VeoExpression')) {
            it.description == 'Extract a value from an element in the context of a given domain'
            with(it.discriminator) {
                propertyName == 'type'
                mapping.and == '#/components/schemas/AndExpression'
                mapping.customAspectAttributeValue == '#/components/schemas/CustomAspectAttributeValueExpression'
                mapping.remove == '#/components/schemas/RemoveExpression'
            }
        }
    }

    def "Domain migration is well-documented"() {
        expect:
        with(getSchema('DomainMigrationStep')) {
            it.description == 'One step in the migration of the domain. A step is a set of removal and/or transform operations described by a set of old and new definitions.'
            it.properties.keySet() ==~ [
                'id',
                'description',
                'oldDefinitions',
                'newDefinitions'
            ]
            it.required ==~ [
                'id',
                'description',
                'oldDefinitions'
            ]
            with(it.properties.oldDefinitions) {
                it.type == 'array'
                it.description == 'A list of attributes in the old domain that this step handles'
                it.items ==
                        [$ref:'#/components/schemas/DomainSpecificValueLocation']
            }
            with(it.properties.newDefinitions) {
                it.type == 'array'
                it.description == 'An optional list of attributes in the new domain that this step will create. If this is omitted, the values will not be transferred into the new domain.'
                it.items == [
                    oneOf:[
                        [$ref:'#/components/schemas/CustomAspectMigrationTransformDefinition']
                    ]
                ]
            }
        }

        with(getSchema('DomainSpecificValueLocation')) {
            it.description == 'The location of a domain-specific value in an element'
            it.properties.keySet() ==~ ['type']
            it.required ==~ ['type']
            it.discriminator == [
                propertyName: 'type',
                mapping:[customAspectAttribute:'#/components/schemas/CustomAspectAttribute']
            ]
        }

        with(getSchema('MigrationTransformDefinition')) {
            it.description == 'Describes a transform operation to translate a value from the old domain to the new'
            it.properties.keySet() ==~ ['type']
            it.required ==~ ['type']
            it.discriminator == [
                propertyName: 'type',
                mapping:[customAspectAttribute:'#/components/schemas/CustomAspectMigrationTransformDefinition']
            ]
        }

        with(getSchema('CustomAspectMigrationTransformDefinition')) {
            it.required ==~ [
                'elementType',
                'customAspect',
                'attribute',
                'migrationExpression'
            ]
            it.allOf.contains( [$ref:'#/components/schemas/MigrationTransformDefinition'])
            with(it.allOf.find{it.type == 'object'}) {
                it.properties.keySet() ==~ [
                    'elementType',
                    'customAspect',
                    'attribute',
                    'migrationExpression'
                ]
                with(it.properties.elementType) {
                    it.type == 'string'
                    it.enum ==~ EntityType.ELEMENT_SINGULAR_TERMS
                    it.description == 'The element type'
                }
                with(it.properties.attribute) {
                    it.type == 'string'
                    it.description == 'The attribute in the custom aspect'
                }
                with(it.properties.migrationExpression) {
                    // TODO check description once swagger-core is able to generate it
                    // it.description == 'An expression to transform the value from the old domain'
                    it.$ref == '#/components/schemas/VeoExpression'
                }
            }
        }
        with(getSchema('VeoExpression')) {
            it.description == 'Extract a value from an element in the context of a given domain'
        }
    }

    def "parts items are properly described"() {
        expect:
        with(getSchema('FullIncidentDto')) {
            it.properties.parts != null
            with(it.properties.parts) {
                it.type == 'array'
                it.items == [$ref: '#/components/schemas/PartReference']
            }
        }
        with(getSchema('PartReference')) {
            it.description == '''A reference to an entity's part'''
            with(it.properties.displayName) {
                description == 'A friendly human readable title of the referenced entity.'
                example == 'My Entity'
            }
            with(it.properties.targetUri) {
                description == 'The resource URL of the referenced entity.'
                example == 'http://<api.example.org>/api/v1/asset/<00000000-0000-0000-0000-000000000000>'
                format == 'uri'
            }
        }
    }

    def "RiskValuesDto is well-documented"() {
        expect:
        with(getSchema('RiskValuesDto')) {
            it.description == 'A set of risk values'
            it.properties.keySet() ==~ [
                'probability',
                'impactValues',
                'riskValues'
            ]
            it.properties.probability == [$ref:'#/components/schemas/Probability']
            it.properties.impactValues == [type:'array', items:[$ref:'#/components/schemas/Impact'], description:'Values describing the impacts of this risk in different risk categories']
            it.properties.riskValues == [type:'array', items:[$ref:'#/components/schemas/DeterminedRisk'], description: 'Values describing the evaluated risk in different categories']
        }
    }

    def "Probability is well-documented"() {
        expect:
        with(getSchema('Probability')) {
            it.description == 'A collection of probability values.'
            it.properties.keySet() ==~ [
                'potentialProbability',
                'specificProbabilityExplanation',
                'specificProbability',
                'effectiveProbability'
            ]
            with(it.properties.potentialProbability) {
                it.type == 'number'
                it.example == 1
                it.description == 'The potential probability derived from the scenario associated with this risk.'
                it.minimum == 0
                it.readOnly == true
            }
            with(it.properties.effectiveProbability) {
                it.type == 'number'
                it.example == 4
                it.description == 'Either the potential probability or the specific probability where the latter takes precedence. A scalar value that matches a valid probability level from a risk-definition.'
                it.minimum == 0
            }
        }
    }

    def "Impact is well-documented"() {
        expect:
        with(getSchema('Impact')) {
            it.description == 'A collection of impact values for a risk category.'
            it.properties.keySet() ==~ [
                'category',
                'potentialImpact',
                'specificImpact',
                'effectiveImpact',
                'specificImpactExplanation'
            ]
            with(it.properties.potentialImpact) {
                it.type == 'number'
                it.example == 3
                it.description == 'The potential impact value derived from the entity associated with this risk. A scalar value that matches a valid impact level from a risk-definition.'
                it.readOnly == true
            }
            with(it.properties.specificImpactExplanation) {
                it.type == 'string'
                it.example == 'While a fire will usually damage a computer in a serious way, our server cases are made out of asbestos.'
                it.description == 'A user-provided explanation for the choice of specific impact.'
                it.maxLength == 65535
            }
        }
    }

    def "DeterminedRisk is well-documented"() {
        expect:
        with(getSchema('DeterminedRisk')) {
            it.description == 'A collection of risk values for a risk category.'
            it.properties.keySet() ==~ [
                'category',
                'inherentRisk',
                'userDefinedResidualRisk',
                'residualRiskExplanation',
                'riskTreatments',
                'riskTreatmentExplanation',
                'residualRisk'
            ]
            with(it.properties.category) {
                it.type == 'string'
                it.example == 'C'
                it.description == 'A scalar value that matches a valid risk category from a risk-definition, such as confidentiality, integrity, availability...'
                it.maxLength == 120
            }
            with(it.properties.userDefinedResidualRisk) {
                it.type == 'number'
                it.example == 3
                it.description == 'The risk that remains after taking controls into account as entered by the user. A scalar value that matches a valid risk level from a risk-definition.'
            }
            with(it.properties.residualRiskExplanation) {
                it.type == 'string'
                it.example == 'Our controls are so good, even our controls are controlled by controls.'
                it.description == '''An explanation for the user's choice of residual risk.'''
                it.maxLength == 65535
            }
            with(it.properties.riskTreatments) {
                it.type == 'array'
                it.uniqueItems == true
                it.description == 'A choice of risk-treatment options as selected by the user.'
                with(it.items) {
                    it.enum == [
                        'RISK_TREATMENT_NONE',
                        'RISK_TREATMENT_AVOIDANCE',
                        'RISK_TREATMENT_ACCEPTANCE',
                        'RISK_TREATMENT_TRANSFER',
                        'RISK_TREATMENT_REDUCTION'
                    ]
                }
            }
        }
    }

    def "TemplateItemAspects is well-documented"() {
        expect:
        with(getSchema('TemplateItemAspects')) {
            it.properties.keySet() ==~ [
                'impactValues',
                'scenarioRiskValues',
                'scopeRiskDefinition'
            ]
            with(it.properties.impactValues) {
                it.type == 'object'
                it.additionalProperties == [$ref: '#/components/schemas/ImpactValues']
            }
            with(it.properties.scenarioRiskValues) {
                it.type == 'object'
                it.additionalProperties == [$ref: '#/components/schemas/PotentialProbability']
            }
            with(it.properties.scopeRiskDefinition) {
                it.type == 'string'
                it.maxLength == 120
            }
        }
    }

    def "IncarnateTemplateItemDescriptionDtoCatalogItemDomainBase is well-documented"() {
        expect:
        with(getSchema('IncarnateTemplateItemDescriptionDtoCatalogItemDomainBase')) {
            it.description == 'Describes the incarnation parameters of one template item.'
            it.properties.keySet() ==~ [
                'item',
                'references'
            ]
            it.properties.item == [$ref: '#/components/schemas/IdRefTemplateItem']
            with(it.properties.references) {
                it.type == 'array'
                it.title == 'A list of references this element needs to set.'
                it.items == [$ref:'#/components/schemas/TailoringReferenceParameterDto']
            }
        }
    }

    def getSchema(String name) {
        def schemas = parsedApiDocs.components.schemas
        schemas[name].tap {
            assert it != null, "Schema $name not found, available schemas: ${schemas.keySet().toSorted()}"
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
