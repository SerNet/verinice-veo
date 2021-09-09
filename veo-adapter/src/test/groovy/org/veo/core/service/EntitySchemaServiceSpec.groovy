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
package org.veo.core.service

import org.veo.adapter.persistence.schema.EntitySchemaServiceClassPathImpl

import groovy.json.JsonSlurper
import spock.lang.Specification

class EntitySchemaServiceSpec extends Specification {

    private static final String SCHEMA_FILES_PATH = "/schemas/entity/";

    EntitySchemaService entitySchemaService = new EntitySchemaServiceClassPathImpl(SCHEMA_FILES_PATH)

    def "custom aspect attributes are contained in 'attributes' object"() {
        given: 'The control schema for with GDPR extensions'
        def schema = entitySchemaService.findSchema("control", ["GDPR"])
        def s = new JsonSlurper().parseText(schema)
        expect: 'the schema has the control_dataProtection extension'
        def controlDataProtection = s.properties.customAspects.properties.control_dataProtection
        controlDataProtection != null
        and: 'the attributes added by the extension are not added to the root properties ...'
        controlDataProtection.properties.control_dataProtection == null
        and: '''... but to the 'attributes' property'''
        controlDataProtection.properties.attributes.properties.control_dataProtection_objectives != null
    }
}
