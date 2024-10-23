/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler.
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
package org.veo.core

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.entity.BreakingChange
import org.veo.core.entity.BreakingChange.ChangeType
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.definitions.CustomAspectDefinition
import org.veo.core.entity.definitions.attribute.BooleanAttributeDefinition
import org.veo.core.entity.definitions.attribute.EnumAttributeDefinition
import org.veo.core.entity.definitions.attribute.IntegerAttributeDefinition
import org.veo.core.entity.definitions.attribute.ListAttributeDefinition
import org.veo.core.entity.definitions.attribute.TextAttributeDefinition
import org.veo.core.usecase.domain.GetBreakingChangesUseCase

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

@WithUserDetails("user@domain.example")
class GetBreakingChangesUseCaseITSpec extends VeoSpringSpec {

    @Autowired
    private GetBreakingChangesUseCase getBreakingChangesUseCase

    Client client
    Domain domain

    def setup() {
        client = createTestClient()
        domain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
    }

    def "No breaking change for new domain"() {
        when:
        def result = executeInTransaction {
            getBreakingChangesUseCase.execute(
                    new GetBreakingChangesUseCase.InputData(client.id, domain.id)
                    ).breakingChanges
        }

        then:
        result.empty
    }

    def "No breaking change for added enum value"() {
        when:
        def result = getBreakingChanges {
            getElementTypeDefinition('process').customAspects.get('process_processingDetails').tap {
                attributeDefinitions.get('process_processingDetails_operatingStage').tap {
                    allowedValues.add('process_processingDetails_operatingStage_broken')
                }
            }
        }

        then:
        result.empty
    }

    def "No breaking change for added enum value in list type"() {
        when:
        def result = getBreakingChanges {
            getElementTypeDefinition('process').customAspects.get('process_dataSubjects').tap {
                attributeDefinitions.get('process_dataSubjects_dataSubjects').itemDefinition.tap {
                    allowedValues.add('process_dataSubjects_dataSubjects_pets')
                }
            }
        }

        then:
        result.empty
    }

    def "No breaking change for added CA"() {
        when:
        def result = getBreakingChanges {
            getElementTypeDefinition('asset').customAspects.put('asset_leaves', new CustomAspectDefinition().tap {
                attributeDefinitions.put('asset_leaves_numberOfLeaves', new IntegerAttributeDefinition())
            })
        }

        then:
        result.empty
    }

    def "No breaking change for added CA attribute"() {
        when:
        def result = getBreakingChanges {
            getElementTypeDefinition('control').customAspects.get('control_generalInformation').tap {
                attributeDefinitions.put('control_generalInformation_documentary', new BooleanAttributeDefinition())
            }
        }

        then:
        result.empty
    }

    def "No breaking change for changed CA attribute translation"() {
        when:
        def result = getBreakingChanges {
            getElementTypeDefinition('person').translations[Locale.of("de")].person_generalInformation_familyName = "Familienname"
        }

        then:
        result.empty
    }

    def "Breaking change for removed enum value"() {
        when:
        def result = getBreakingChanges {
            getElementTypeDefinition('process').customAspects.get('process_processingDetails').tap {
                attributeDefinitions.get('process_processingDetails_operatingStage').tap {
                    allowedValues.remove('process_processingDetails_operatingStage_test')
                }
            }
        }

        then:
        result.size() == 1
        with (result.first()) {
            change == ChangeType.MODIFICATION
            attribute == 'process_processingDetails_operatingStage'
            customAspect == 'process_processingDetails'
            elementType == 'process'
        }
    }

    def "Breaking change for removed enum value in list type"() {
        when:
        def result = getBreakingChanges {
            getElementTypeDefinition('control').customAspects.get('control_dataProtection').tap {
                attributeDefinitions.get('control_dataProtection_objectives').itemDefinition.tap {
                    allowedValues.remove('control_dataProtection_objectives_recoverability')
                }
            }
        }

        then:
        result.size() == 1
        with (result.first()) {
            change == ChangeType.MODIFICATION
            attribute == 'control_dataProtection_objectives'
            customAspect == 'control_dataProtection'
            elementType == 'control'
            with(oldValue as ListAttributeDefinition) {
                with(itemDefinition as EnumAttributeDefinition) {
                    allowedValues.contains('control_dataProtection_objectives_recoverability')
                }
            }

            with(value as ListAttributeDefinition) {
                with(itemDefinition as EnumAttributeDefinition) {
                    !allowedValues.contains('control_dataProtection_objectives_recoverability')
                }
            }
        }
    }

    def "Breaking changes for removed CA"() {
        when:
        def result = getBreakingChanges {
            getElementTypeDefinition('scope').customAspects.remove('scope_regulatoryAuthority')
        }

        then:
        result.size() == 12
        result.each {
            with(it) {
                change == ChangeType.REMOVAL
                customAspect == 'scope_regulatoryAuthority'
                elementType == 'scope'
                oldValue != null
                value == null
            }
        }
    }

    def "Breaking change for removed CA attribute"() {
        when:
        def result = getBreakingChanges {
            getElementTypeDefinition('scenario').customAspects.get('scenario_vulnerability').tap {
                attributeDefinitions.remove('scenario_vulnerability_otherType')
            }
        }

        then:
        result.size() == 1
        with (result.first()) {
            change == ChangeType.REMOVAL
            attribute == 'scenario_vulnerability_otherType'
            customAspect == 'scenario_vulnerability'
            elementType == 'scenario'
            oldValue instanceof TextAttributeDefinition
            value == null
        }
    }

    def "Breaking change for changed CA attribute type"() {
        when:
        def result = getBreakingChanges {
            getElementTypeDefinition('person').customAspects.get('person_address').tap {
                attributeDefinitions.put('person_address_postcode', new IntegerAttributeDefinition())
            }
        }

        then:
        result.size() == 1
        with (result.first()) {
            change == ChangeType.MODIFICATION
            attribute == 'person_address_postcode'
            customAspect == 'person_address'
            elementType == 'person'
            oldValue instanceof TextAttributeDefinition
            value instanceof IntegerAttributeDefinition
        }
    }

    def "Breaking change for changed list item definition enum value"() {
        when:
        def result = getBreakingChanges {
            getElementTypeDefinition('control').customAspects.get('control_dataProtection').tap {
                attributeDefinitions.get('control_dataProtection_objectives').tap {
                    itemDefinition = new TextAttributeDefinition()
                }
            }
        }

        then:
        result.size() == 1
        with (result.first()) {
            change == ChangeType.MODIFICATION
            attribute == 'control_dataProtection_objectives'
            customAspect == 'control_dataProtection'
            elementType == 'control'
            (oldValue as ListAttributeDefinition).itemDefinition instanceof EnumAttributeDefinition
            (value as ListAttributeDefinition).itemDefinition instanceof TextAttributeDefinition
        }
    }

    List<BreakingChange> getBreakingChanges( @DelegatesTo(value = Domain.class, strategy = Closure.DELEGATE_FIRST)
            @ClosureParams(value = SimpleType, options = "org.veo.core.entity.Domain") Closure<Domain> domainModification) {
        domainModification.delegate = domain
        domainModification.resolveStrategy = Closure.DELEGATE_FIRST
        domainModification.call(domain)
        domainDataRepository.save(domain)
        return executeInTransaction {
            getBreakingChangesUseCase.execute(
                    new GetBreakingChangesUseCase.InputData(client.id, domain.id)
                    ).breakingChanges
        }
    }
}