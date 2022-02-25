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

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.EntityType

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

    def "control risk values are mapped correctly"() {
        given:
        def schemas = parsedApiDocs.components.schemas

        expect:
        def controlSchema = schemas.FullControlDto
        controlSchema.properties.domains.additionalProperties.'$ref' == "#/components/schemas/ControlDomainAssociationDto"

        def controlDomainAssociationSchema = schemas.ControlDomainAssociationDto
        controlDomainAssociationSchema.properties.riskValues.additionalProperties.'$ref' == "#/components/schemas/ControlRiskValuesDto"

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

        def scenarioRiskValuesSchema = schemas.ScenarioRiskValuesDto
        scenarioRiskValuesSchema.properties.potentialProbability.type == "integer"
    }

    def "scope risk values are mapped correctly"() {
        given:
        def schemas = parsedApiDocs.components.schemas

        expect:
        def scopeSchema = schemas.FullScopeDto
        scopeSchema.properties.domains.additionalProperties.'$ref' == "#/components/schemas/ScopeDomainAssociationDto"

        def scopeDomainAssociationSchema = schemas.ScopeDomainAssociationDto
        scopeDomainAssociationSchema.properties.riskDefinition.type == "string"
    }

    def "process risk values are mapped correctly"() {
        given:
        def schemas = parsedApiDocs.components.schemas

        expect:
        def processSchema = schemas.FullProcessDto
        processSchema.properties.domains.additionalProperties.'$ref' == "#/components/schemas/ProcessDomainAssociationDto"

        def processDomainAssociationSchema = schemas.ProcessDomainAssociationDto
        processDomainAssociationSchema.description == '''Details about this element's association with domains. Domain ID is key, association object is value.'''
        processDomainAssociationSchema.properties.riskValues.additionalProperties.'$ref' == "#/components/schemas/ProcessRiskValuesDto"

        def processRiskValuesSchema = schemas.ProcessRiskValuesDto
        processRiskValuesSchema.description == '''Key is risk definition ID, value contains risk values in the context of that risk definition.'''
        def potentialImpactsSchema = processRiskValuesSchema.properties.potentialImpacts
        potentialImpactsSchema.type == "object"
        potentialImpactsSchema.description == "Potential impacts for a set of risk categories"
        potentialImpactsSchema.example == [C:2, I:3]
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
        def catalogItemDtoSchema = parsedApiDocs.components.schemas.FullCatalogItemDto
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


    def "endpoint documentation is correct for CreateDomainUseCase"() {
        when: "retrieving the information about the endpoint"
        def endPointInfo = parsedApiDocs.paths["/domaintemplates/{id}/createdomains"]
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
        def endPointInfo = parsedApiDocs.paths["/domains/{id}/createdomaintemplate/{revision}"]
        then: "the information is found"
        endPointInfo != null
        and: 'it handles post requests'
        endPointInfo.post != null
        and: 'it has a meaningful description'
        endPointInfo.post.summary == 'Creates a domaintemplate from a domain'
        and: 'parameter is described'
        with(endPointInfo.post.parameters[0]) {
            name == 'id'
            required == true
        }
        with(endPointInfo.post.parameters[1]) {
            name == 'revision'
            required == true
        }
        with(endPointInfo.post.security) {
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
