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
package org.veo.rest.common

import org.springframework.beans.factory.annotation.Autowired

import org.veo.adapter.presenter.api.dto.full.AssetRiskDto
import org.veo.adapter.presenter.api.dto.full.FullAssetDto
import org.veo.adapter.presenter.api.dto.full.FullControlDto
import org.veo.adapter.presenter.api.dto.full.FullDocumentDto
import org.veo.adapter.presenter.api.dto.full.FullDomainDto
import org.veo.adapter.presenter.api.dto.full.FullIncidentDto
import org.veo.adapter.presenter.api.dto.full.FullPersonDto
import org.veo.adapter.presenter.api.dto.full.FullProcessDto
import org.veo.adapter.presenter.api.dto.full.FullScenarioDto
import org.veo.adapter.presenter.api.dto.full.FullScopeDto
import org.veo.adapter.presenter.api.dto.full.FullUnitDto
import org.veo.adapter.presenter.api.dto.full.LegacyCatalogItemDto
import org.veo.core.VeoSpringSpec
import org.veo.rest.configuration.TypeExtractor

class TypeExtractorITSpec extends VeoSpringSpec {

    @Autowired
    TypeExtractor typeExtractor

    def "The parsed DTO type for #uri is #dtoType"() {
        expect:
        typeExtractor.parseDtoType(uri).get() == dtoType

        where:
        uri                                                                                         | dtoType
        '/assets/40331ed5-be07-4c69-bf99-553811ce5454'                                              | FullAssetDto
        '/assets/40331ed5-be07-4c69-bf99-553811ce5454/risks/c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6'   | AssetRiskDto
        '/assets/40331ed5-be07-4c69-bf99-553811ce5454?foo=bar'                                      | FullAssetDto
        '/controls/c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6'                                            | FullControlDto
        '/scopes/59d3c21d-2f21-4085-950d-1273056d664a'                                              | FullScopeDto
        '/scenarios/f05ab334-c605-456e-8a78-9e1bc85b8509'                                           | FullScenarioDto
        '/incidents/7b4aa38a-117f-40c0-a5e8-ee5a59fe79ac'                                           | FullIncidentDto
        '/domains/28df429d-da5e-431a-a2d8-488c0741fb9f'                                             | FullDomainDto
        '/controls/28df429d-da5e-431a-a2d8-488c0741fb9f'                                            | FullControlDto
        '/documents/28df429d-da5e-431a-a2d8-488c0741fb9f'                                           | FullDocumentDto
        '/processes/28df429d-da5e-431a-a2d8-488c0741fb9f'                                           | FullProcessDto
        '/persons/28df429d-da5e-431a-a2d8-488c0741fb9f'                                             | FullPersonDto
        '/units/28df429d-da5e-431a-a2d8-488c0741fb9f'                                               | FullUnitDto
        '/domains/28df429d-da5e-431a-a2d8-488c0741fb9f'                                             | FullDomainDto
        '/catalogs/28df429d-da5e-431a-a2d8-488c0741fb9f/items/c37ec67f-5d59-45ed-a4e1-88b0cc5fd1a6' | LegacyCatalogItemDto
    }

    def "The invalid URI #uri is rejected"() {
        when:
        typeExtractor.parseDtoType(uri)

        then: "no type is returned for this URI"
        def error = thrown AssertionError
        error.message ==~ message

        where:
        uri                                                                                                                                       | message
        '/domains/28df429d-da5e-431a-a2d8-488c0741fb9f/templates/28df429d-da5e-431a-a2d8-488c0741fb9f'                                            | '.*No mapping found for URI.*'
        '/clients/28df429d-da5e-431a-a2d8-488c0741fb9f'                                                                                           | '.*No mapping found for URI.*'
        'http://localhost:9000/assets/00000000-0000-0000-0000-000000000000/%252e%252e/%252e%252e/processes/28df429d-da5e-431a-a2d8-488c0741fb9f'  | '.*No mapping found for URI.*'
    }
}
