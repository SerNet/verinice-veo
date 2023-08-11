/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade.
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

import org.veo.adapter.presenter.api.dto.full.FullDocumentDto
import org.veo.adapter.presenter.api.dto.full.FullUnitDto

import spock.lang.Specification

class SchemaProviderSpec extends Specification {

    SchemaProvider schemaProvider = SchemaProvider.getInstance()

    def "Schema contains constraints on description property"() {
        when:
        def schema = schemaProvider.schema(FullDocumentDto).get()

        then:
        schema != null

        when:
        def descriptionSchema = schema.get('properties').get('description')

        then:
        descriptionSchema != null
        descriptionSchema.get('maxLength').intValue() == 65535
    }

    def "encapsulated schemas are returned as copies"() {
        given:
        def schema = schemaProvider.schema(FullUnitDto.class)

        when: "modifying one copy of the schema"
        schema.get().put("extraField", 5)

        then: "the next copy is pristine"
        schema.get().get("extraField") == null
    }
}
