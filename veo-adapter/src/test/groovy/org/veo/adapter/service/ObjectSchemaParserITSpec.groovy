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
import org.veo.core.entity.definitions.attribute.BooleanAttributeDefinition
import org.veo.core.entity.definitions.attribute.DateAttributeDefinition
import org.veo.core.entity.definitions.attribute.DateTimeAttributeDefinition
import org.veo.core.entity.definitions.attribute.EnumAttributeDefinition
import org.veo.core.entity.definitions.attribute.ExternalDocumentAttributeDefinition
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition
import org.veo.core.entity.definitions.attribute.ListAttributeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.core.entity.transform.EntityFactory

import spock.lang.Specification

class ObjectSchemaParserITSpec extends Specification {

    EntityFactory entityFactory = Mock()
    ObjectSchemaParser objectSchemaParser = new ObjectSchemaParser(entityFactory)
    static ObjectMapper objectMapper = new ObjectMapper()

    def "parse an object schema into an ElementTypeDefinition"() {
        given:
        def schemaNode = objectMapper.readTree(ObjectSchemaParserITSpec.getResourceAsStream('/os_process.json'))
        def elementDefinition = Mock(ElementTypeDefinition)
        when:
        def result = objectSchemaParser.parseTypeDefinitionFromObjectSchema(EntityType.PROCESS, schemaNode)
        then:
        result == elementDefinition
        1 * entityFactory.createElementTypeDefinition('process', null) >> elementDefinition
        1 * elementDefinition.setSubTypes({
            it.keySet() == [
                'PRO_DataTransfer',
                'PRO_DataProcessing'
            ] as Set
            it.PRO_DataTransfer.statuses==[
                'NEW',
                'IN_PROGRESS',
                'FOR_REVIEW',
                'RELEASED',
                'ARCHIVED'
            ]
            it.PRO_DataProcessing.statuses==[
                'NEW',
                'IN_PROGRESS',
                'FOR_REVIEW',
                'RELEASED',
                'ARCHIVED'
            ]
        })

        1 * elementDefinition.setCustomAspects({
            it.size() == 12
            with(it.process_dataProcessing.attributeDefinitions) {
                with (process_dataProcessing_legalBasis) {
                    it instanceof ListAttributeDefinition
                    itemDefinition instanceof EnumAttributeDefinition
                    with(itemDefinition.allowedValues) {
                        size() == 21
                        it.contains('process_dataProcessing_legalBasis_Art6Abs1litaDSGVO')
                    }
                }
            }

            with(it.process_opinionDPO.attributeDefinitions) {
                with(process_opinionDPO_document) {
                    it instanceof ExternalDocumentAttributeDefinition
                }
            }

            with(it.process_processingDetails.attributeDefinitions) {
                with(process_processingDetails_surveyConductedOn) {
                    it instanceof DateAttributeDefinition
                }
            }

            with(it.process_accessAuthorization.attributeDefinitions) {
                with(process_accessAuthorization_concept) {
                    it instanceof BooleanAttributeDefinition
                }
                with(process_accessAuthorization_reviewTime) {
                    it instanceof DateTimeAttributeDefinition
                }
            }
        })
        1 * elementDefinition.setLinks({
            it.size() == 12
            with( it.process_requiredApplications) {
                it.targetType == 'asset'
                it.targetSubType == 'AST_Application'
                it.attributeDefinitions == [:]
            }
            with( it.process_dataType) {
                it.targetType == 'asset'
                it.targetSubType == 'AST_Datatype'
                with( it.attributeDefinitions) {
                    with (process_dataType_comment) {
                        it instanceof TextAttributeDefinition
                    }
                    with (process_dataType_deletionPeriod) {
                        it instanceof EnumAttributeDefinition
                        with(it.allowedValues) {
                            size() == 4
                            it.contains('process_dataType_deletionPeriod_immediately')
                        }
                    }
                    with(process_dataType_deletionPeriodCount) {
                        it instanceof IntegerAttributeDefinition
                    }
                }
            }
        })

        1 * elementDefinition.setTranslations({
            it.size() == 2
            with( it.de) {
                it.process_controller == 'Auftraggeber nach Art. 30 II DS-GVO'
            }
        })
    }
}
