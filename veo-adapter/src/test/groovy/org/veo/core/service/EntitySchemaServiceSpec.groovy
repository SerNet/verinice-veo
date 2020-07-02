/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.service

import groovy.json.JsonSlurper

import org.veo.adapter.persistence.schema.EntitySchemaServiceClassPathImpl
import spock.lang.Specification

class EntitySchemaServiceSpec extends Specification {

    EntitySchemaService entitySchemaService = new EntitySchemaServiceClassPathImpl()

    def "custom properties are contained in 'attributes' array"() {
        given: 'The control schema for with GDPR extensions'
        def schema = entitySchemaService.findSchema("control", ["GDPR"])
        def s = new JsonSlurper().parseText(schema)
        expect: 'the schema has the ControlDataProtectionObjectivesEugdpr extension'
        def controlDataProtectionObjectivesEugdpr = s.properties.customAspects.properties.ControlDataProtectionObjectivesEugdpr
        controlDataProtectionObjectivesEugdpr != null
        and: 'the custom properties added by the extension are not added to the root properties ...'
        controlDataProtectionObjectivesEugdpr.properties.controlDataProtectionObjectivesEugdprPseudonymization == null
        and: '''... but to the 'attributes' property'''
        controlDataProtectionObjectivesEugdpr.properties.attributes.properties.controlDataProtectionObjectivesEugdprPseudonymization != null
    }
}
