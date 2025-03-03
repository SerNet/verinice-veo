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
import org.veo.adapter.presenter.api.dto.ControlImplementationDto
import org.veo.adapter.presenter.api.dto.RequirementImplementationDto
import org.veo.adapter.presenter.api.dto.full.FullControlDto
import org.veo.adapter.presenter.api.dto.full.FullScopeDto
import org.veo.adapter.presenter.api.dto.full.FullUnitDto
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.compliance.ImplementationStatus
import org.veo.core.entity.state.UnitState
import org.veo.core.usecase.unit.UnitImportUseCase

@WithUserDetails("user@domain.example")
class UnitImportUseCaseITSpec extends VeoSpringSpec {
    Client client
    Unit unit
    Domain testDomain
    Domain dsgvoDomain

    @Autowired
    ReferenceAssembler referenceAssembler

    @Autowired
    UnitImportUseCase useCase

    def setup() {
        client = createTestClient()
        testDomain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_V2_UUID)
        client = clientRepository.save(client)
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
        def result = useCase.execute(new UnitImportUseCase.InputData(client, unitDto, elements as Set, risks as Set))
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
        def result = useCase.execute(new UnitImportUseCase.InputData(client, unitDto, elements as Set, risks as Set))
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
}
