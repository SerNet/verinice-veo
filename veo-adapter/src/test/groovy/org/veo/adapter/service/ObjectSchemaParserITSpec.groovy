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
package org.veo.adapter.service

import com.fasterxml.jackson.databind.ObjectMapper

import org.veo.core.entity.EntityType
import org.veo.core.entity.definitions.ElementTypeDefinition
import org.veo.core.entity.transform.EntityFactory

import spock.lang.Specification

class ObjectSchemaParserITSpec extends Specification {

    EntityFactory entityFactory = Mock()
    ObjectSchemaParser objectSchemaParser = new ObjectSchemaParser(entityFactory)
    static ObjectMapper objectMapper = new ObjectMapper()


    def "parse an object schema into an ElementTypeDefinition"() {
        given:
        def schemaNode = objectMapper.readTree(ObjectSchemaParserITSpec.getResourceAsStream('/os_process.json'))
        ElementTypeDefinition elementDefinition = Mock()
        when:
        elementDefinition = objectSchemaParser.parseTypeDefinitionFromObjectSchema(EntityType.PROCESS, schemaNode)
        then:
        1 * entityFactory.createElementTypeDefinition('process', null) >> elementDefinition
        1 * elementDefinition.setSubTypes({
            it.keySet() == [
                'PRO_DataTransfer',
                'PRO_DataProcessing'
            ] as Set
            it.PRO_DataTransfer.statuses==[
                'IN_PROGRESS',
                'NEW',
                'RELEASED',
                'FOR_REVIEW',
                'ARCHIVED'
            ] as Set
            it.PRO_DataProcessing.statuses==[
                'IN_PROGRESS',
                'NEW',
                'RELEASED',
                'FOR_REVIEW',
                'ARCHIVED'
            ] as Set
        })

        1 * elementDefinition.setCustomAspects({
            it.size() == 12
            with( it.process_dataProcessing.attributeSchemas) {
                with (process_dataProcessing_legalBasis) {
                    it.type == 'array'
                    with(it.items.enum) {
                        size() == 21
                        it.contains('process_dataProcessing_legalBasis_Art6Abs1litaDSGVO')
                    }
                }
            }

            with(it.process_opinionDPO.attributeSchemas) {
                process_opinionDPO_document == [format : 'uri', type : 'string', pattern : '^(https?|ftp)://' ]
            }
        })
        1 * elementDefinition.setLinks({
            it.size() == 12
            with( it.process_requiredApplications) {
                it.targetType == 'asset'
                it.targetSubType == 'AST_Application'
                it.attributeSchemas == [:]
            }
            with( it.process_dataType) {
                it.targetType == 'asset'
                it.targetSubType == 'AST_Datatype'
                with( it.attributeSchemas) {
                    with (process_dataType_comment) {
                        it.type == 'string'
                    }
                    with (process_dataType_deletionPeriod) {
                        with(it.enum) {
                            size() == 4
                            it.contains('process_dataType_deletionPeriod_immediately')
                        }
                    }
                }
            }
        })
    }
}
