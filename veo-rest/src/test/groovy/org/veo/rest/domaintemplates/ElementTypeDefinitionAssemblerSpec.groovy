/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
package org.veo.rest.domaintemplates

import org.veo.rest.domaintemplates.ElementTypeDefinitionAssembler

import spock.lang.Specification

class ElementTypeDefinitionAssemblerSpec extends Specification{
    def elementTypeDefinitionAssembler = new ElementTypeDefinitionAssembler()

    def "loads asset type definition from files"() {
        when:
        def definition = elementTypeDefinitionAssembler.loadDefinition(testTypesDir)
        then:
        with(definition.customAspects) {
            size() == 2
            get("asset_details").attributeSchemas.asset_details_number.type == "integer"
            get("asset_generalInformation").attributeSchemas.asset_generalInformation_document.format == "uri"
        }
        with(definition.links) {
            size() == 2
            get("asset_designer").attributeSchemas.redesign.type == "boolean"
            get("asset_manual").targetType == "document"
        }
        with(definition.subTypes) {
            size() == 2
            get("AST_Application").statuses.toSorted() == [
                "IN_PROGRESS",
                "NEW",
                "RELEASED"
            ]
            get("AST_Datatype").statuses.toSorted() == [
                "DEPRECATED",
                "SPECIFIED",
                "SUPPORTED"
            ]
        }
    }

    private File getTestTypesDir() {
        new File(getClass().classLoader.getResource("testDomainTemplate/types/asset").toURI())
    }
}
