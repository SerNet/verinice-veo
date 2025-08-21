/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Jochen Kemnade
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

import org.veo.adapter.presenter.api.common.IdRef
import org.veo.adapter.presenter.api.common.ReferenceAssembler
import org.veo.adapter.presenter.api.common.SymIdRef
import org.veo.adapter.presenter.api.dto.ControlImplementationDto
import org.veo.adapter.presenter.api.dto.CustomAspectDto
import org.veo.adapter.presenter.api.dto.CustomLinkDto
import org.veo.adapter.presenter.api.dto.DomainAssociationDto
import org.veo.adapter.presenter.api.dto.ProcessDomainAssociationDto
import org.veo.adapter.presenter.api.dto.RequirementImplementationDto
import org.veo.adapter.presenter.api.dto.full.FullControlDto
import org.veo.adapter.presenter.api.dto.full.FullProcessDto
import org.veo.adapter.presenter.api.dto.full.FullScopeDto
import org.veo.adapter.presenter.api.dto.full.FullUnitDto
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.compliance.ImplementationStatus
import org.veo.core.entity.state.UnitState
import org.veo.core.usecase.unit.UnitImportUseCase
import org.veo.rest.security.NoRestrictionAccessRight

@WithUserDetails("user@domain.example")
class UnitImportUseCaseITSpec extends VeoSpringSpec {
    Client client
    Unit unit
    Domain testDomain
    Domain testDomain2
    UserAccessRights user = Mock()

    @Autowired
    ReferenceAssembler referenceAssembler

    @Autowired
    UnitImportUseCase useCase

    def setup() {
        client = createTestClient()
        testDomain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_V2_UUID)
        testDomain2 = createTestDomain(client, DSGVO_DOMAINTEMPLATE_V2_UUID)
        client = clientRepository.save(client)
        user.clientId() >> client.id
    }

    def "Import a scope with an RI"() {
        given:
        UnitState unitDto = new FullUnitDto().tap {
            id = UUID.randomUUID()
            name = 'Super unit'
        }
        def controlId = UUID.randomUUID()
        def scopeId = UUID.randomUUID()
        def elements = [
            new FullControlDto().tap {
                id = controlId
                name = 'My control'
            },
            new FullScopeDto().tap {
                id = scopeId
                name = 'My scope'
                requirementImplementations.add(new RequirementImplementationDto().tap {
                    it.control = IdRef.fromUri("/controls/${controlId}", referenceAssembler)
                    it.status = ImplementationStatus.YES
                })
            }
        ]
        def risks = []

        when:
        def result = useCase.execute(new UnitImportUseCase.InputData( 2, unitDto, elements as Set, risks as Set), NoRestrictionAccessRight.from(client.idAsString))
        def unit = result.unit
        def scopes = executeInTransaction{
            scopeDataRepository.findAll().tap{
                it*.requirementImplementations*.control*.name
            }
        }

        then:
        scopes.size() == 1
        with(scopes.first()) {
            requirementImplementations.size() == 1
            with (requirementImplementations.first()) {
                it.control.name == 'My control'
                it.status == ImplementationStatus.YES
            }
        }
    }

    def "RIs missing from the import are created as unedited"() {
        given:
        UnitState unitDto = new FullUnitDto().tap {
            id = UUID.randomUUID()
            name = 'Super unit'
        }
        def controlId = UUID.randomUUID()
        def scopeId = UUID.randomUUID()
        def controlPartId = UUID.randomUUID()
        def elements = [
            new FullControlDto().tap {
                id = controlId
                name = 'My control'
                parts = [
                    IdRef.fromUri("/controls/${controlPartId}", referenceAssembler)
                ]
            },
            new FullControlDto().tap {
                id = controlPartId
                name = 'My control part'
            },
            new FullScopeDto().tap {
                id = scopeId
                name = 'My scope'
                controlImplementations.add(new ControlImplementationDto().tap {
                    it.control = IdRef.fromUri("/controls/${controlId}", referenceAssembler)
                })
            }
        ]
        def risks = []

        when:
        def result = useCase.execute(new UnitImportUseCase.InputData( 2, unitDto, elements as Set, risks as Set),NoRestrictionAccessRight.from(client.idAsString))
        def unit = result.unit
        def scopes = executeInTransaction{
            scopeDataRepository.findAll().tap{
                it*.controlImplementations*.size()
                it*.requirementImplementations*.control*.name
            }
        }

        then:
        scopes.size() == 1
        with(scopes.first()) {
            controlImplementations.size() == 1
            requirementImplementations*.control*.name ==~ [
                'My control',
                'My control part'
            ]
        }
    }

    def "RIs are created if control comes after target"() {
        given:
        UnitState unitDto = new FullUnitDto().tap {
            id = UUID.randomUUID()
            name = 'Super unit'
        }
        def controlId = UUID.randomUUID()
        def scopeId = UUID.randomUUID()
        def controlPartId = UUID.randomUUID()
        def elements = [
            new FullScopeDto().tap {
                id = scopeId
                name = 'My scope'
                controlImplementations.add(new ControlImplementationDto().tap {
                    it.control = IdRef.fromUri("/controls/${controlId}", referenceAssembler)
                })
            },
            new FullControlDto().tap {
                id = controlId
                name = 'My control'
                parts = [
                    IdRef.fromUri("/controls/${controlPartId}", referenceAssembler)
                ]
            },
            new FullControlDto().tap {
                id = controlPartId
                name = 'My control part'
            }
        ]
        def risks = []

        when:
        def result = useCase.execute(new UnitImportUseCase.InputData( 2, unitDto, elements as Set, risks as Set),NoRestrictionAccessRight.from(client.idAsString))
        def unit = result.unit
        def scopes = executeInTransaction{
            scopeDataRepository.findAll().tap{
                it*.controlImplementations*.size()
                it*.requirementImplementations*.control*.name
            }
        }

        then:
        scopes.size() == 1
        with(scopes.first()) {
            controlImplementations.size() == 1
            requirementImplementations*.control*.name ==~ [
                'My control',
                'My control part'
            ]
        }
    }

    def "Import a control with a catalog item reference"() {
        given:
        UnitState unitDto = new FullUnitDto().tap {
            id = UUID.randomUUID()
            name = 'My unit'
            domains = [
                IdRef.fromUri("/domains/${testDomain.id}", referenceAssembler)
            ]
        }
        def controlId = UUID.randomUUID()
        def elements = [
            new FullControlDto().tap {
                id = controlId
                name = 'TOM zur Verschlüsselung'
                domains = [(testDomain.id):new DomainAssociationDto().tap {
                        subType = 'CTL_TOM'
                        status = 'NEW'
                        appliedCatalogItem = SymIdRef.fromUri("/domains/${testDomain.id}/catalog-items/7a60f2cc-fb20-4669-9eda-d76863a29602", referenceAssembler)
                    }]
            }
        ]
        def risks = []

        when:
        def result = executeInTransaction{
            useCase.execute(new UnitImportUseCase.InputData( 2, unitDto, elements as Set, risks as Set), NoRestrictionAccessRight.from(client.idAsString))
        }
        def unit = result.unit
        def controls = executeInTransaction{
            controlDataRepository.findAll().tap{
                it*.domainAssociations*.appliedCatalogItem*.name
            }
        }

        then:
        controls.size() == 1
        with(controls.first()) {
            domainAssociations.size() == 1
            with(domainAssociations.first()) {
                it.appliedCatalogItem.name == 'TOM zur Verschlüsselung'
            }
        }
    }

    def "Import a multi-domain unit"() {
        given:
        def domain1Ref = IdRef.fromUri("/domains/${testDomain.id}", referenceAssembler)
        def domain2Ref = IdRef.fromUri("/domains/${testDomain2.id}", referenceAssembler)
        UnitState unitDto = new FullUnitDto().tap {
            id = UUID.randomUUID()
            name = 'My unit'
            domains = [
                domain1Ref,
                domain2Ref
            ]
        }
        def controlId = UUID.randomUUID()
        def processDomain1Id = UUID.randomUUID()
        def processDomain2Id = UUID.randomUUID()

        def control = new FullControlDto().tap {
            id = controlId
            name = 'My control'
            domains = [
                (testDomain.id) : new DomainAssociationDto().tap {
                    subType = 'CTL_TOM'
                    status = 'NEW'
                },
                (testDomain2.id): new DomainAssociationDto().tap {
                    subType = 'CTL_TOM'
                    status = 'NEW'
                }
            ]
            customAspects = [
                control_revision: new CustomAspectDto().tap {
                    domains = [domain1Ref, domain2Ref]
                    attributes = [control_revision_comment: 'All is well.']
                }
            ]
        }

        def processDomain1 = new FullProcessDto().tap {
            id = processDomain1Id
            name = 'Process 1'
            domains = [
                (testDomain.id): new ProcessDomainAssociationDto().tap {
                    subType = 'PRO_DataProcessing'
                    status = 'NEW'
                }
            ]
            links = [process_tom: [
                    new CustomLinkDto().tap {
                        domains = [domain1Ref]
                        target = IdRef.fromUri("/domains/${testDomain2.id}/controls/${controlId}", referenceAssembler)
                    }
                ]]
            customAspects = [
                process_intendedPurpose: new CustomAspectDto().tap {
                    domains = [domain1Ref]
                    attributes = [process_intendedPurpose_intendedPurpose: 'Earn some money']
                }
            ]
        }
        def processDomain2 = new FullProcessDto().tap {
            name = 'Process 2'
            id = processDomain2Id
            domains = [
                (testDomain2.id): new ProcessDomainAssociationDto().tap {
                    subType = 'PRO_DataProcessing'
                    status = 'NEW'
                }
            ]
            links = [process_tom: [
                    new CustomLinkDto().tap {
                        domains = [domain2Ref]
                        target = IdRef.fromUri("/domains/${testDomain2.id}/controls/${controlId}", referenceAssembler)
                    }
                ]]
            customAspects = [
                process_accessAuthorization: new CustomAspectDto().tap {
                    domains = [domain2Ref]
                    attributes = [process_accessAuthorization_description: 'Help yourself']
                }
            ]
        }

        def elements = [
            control,
            processDomain1,
            processDomain2
        ]
        def risks = []

        when:
        def result = executeInTransaction { useCase.execute(new UnitImportUseCase.InputData( 2, unitDto, elements as Set, risks as Set),NoRestrictionAccessRight.from(client.idAsString)) }
        def unit = result.unit
        def processes = executeInTransaction {
            processDataRepository.findAll().tap {
                it.domainAssociations*.domain*.name
                it.links*.target*.name
                it.customAspects*.type
            }
        }
        def controls = executeInTransaction {
            controlDataRepository.findAll().tap {
                it.domainAssociations*.domain*.name
                it.customAspects*.type
            }
        }

        then:
        processes.size() == 2
        controls.size() == 1
        with(controls.first()) {
            it.domains*.id ==~ [testDomain.id, testDomain2.id]
            it.customAspects.size() == 2
            with(it.customAspects.find {it.domain.id == testDomain.id }) {
                it.type == 'control_revision'
                it.attributes == [control_revision_comment: 'All is well.']
            }
            with(it.customAspects.find {it.domain.id == testDomain2.id }) {
                it.type == 'control_revision'
                it.attributes == [control_revision_comment: 'All is well.']
            }
        }
        with(processes.find {it.name == 'Process 1'}) {
            it.domains*.id ==~ [testDomain.id]
            it.links.size() == 1
            with(it.links.first()) {
                it.domain.id == testDomain.id
                it.type == 'process_tom'
                it.target.name == control.name
            }
            it.customAspects.size() == 1
            with(it.customAspects.first()) {
                it.domain.id == testDomain.id
                it.type == 'process_intendedPurpose'
                it.attributes == [process_intendedPurpose_intendedPurpose: 'Earn some money']
            }
        }
        with(processes.find {it.name == 'Process 2'}) {
            it.domains*.id ==~ [testDomain2.id]
            it.links.size() == 1
            with(it.links.first()) {
                it.domain.id == testDomain2.id
                it.type == 'process_tom'
                it.target.name == control.name
            }
            it.customAspects.size() == 1
            with(it.customAspects.first()) {
                it.domain.id == testDomain2.id
                it.type == 'process_accessAuthorization'
                it.attributes == [process_accessAuthorization_description: 'Help yourself']
            }
        }
    }
}
