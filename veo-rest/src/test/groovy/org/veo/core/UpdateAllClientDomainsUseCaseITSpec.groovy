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
package org.veo.core

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.adapter.service.domaintemplate.DomainTemplateServiceImpl
import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Scope
import org.veo.core.entity.Unit
import org.veo.core.entity.risk.ControlRiskValues
import org.veo.core.entity.risk.ImplementationStatusRef
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.core.repository.ControlRepository
import org.veo.core.repository.PagingConfiguration
import org.veo.core.usecase.domain.UpdateAllClientDomainsUseCase
import org.veo.core.usecase.domain.UpdateAllClientDomainsUseCase.InputData
import org.veo.core.usecase.unit.CreateDemoUnitUseCase
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.ScopeRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.ControlData

@WithUserDetails("user@domain.example")
class UpdateAllClientDomainsUseCaseITSpec extends VeoSpringSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private UpdateAllClientDomainsUseCase useCase

    @Autowired
    private CreateDemoUnitUseCase createDemoUnitUseCase

    @Autowired
    ScopeRepositoryImpl scopeRepository

    @Autowired
    ProcessRepositoryImpl processRepository

    @Autowired
    ControlRepository controlRepository

    Client client
    Domain dsgvoDomain
    Domain dsgvoTestDomain

    def setup() {
        executeInTransaction {
            client = newClient()
            dsgvoDomain = domainTemplateService.createDomain(client, DomainTemplateServiceImpl.DSGVO_DOMAINTEMPLATE_UUID)
            dsgvoTestDomain = domainTemplateService.createDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
            client.addToDomains(dsgvoDomain)
            client.addToDomains(dsgvoTestDomain)
            client = clientRepository.save(client)
        }
    }

    def "Migrate an empty client"() {
        when: 'executing the UpdateAllClientDomainsUseCase'
        runUseCase(DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        client = clientRepository.findById(client.id).get()
        then: 'the old domain is disabled'
        !client.domains.find{it.id == dsgvoDomain.id}.active
        client.domains.find{it.id == dsgvoTestDomain.id}.active
    }

    def "Migrate an empty unit"() {
        given: 'a client with an empty unit'
        Unit unit = unitRepository.save(newUnit(client) {
            addToDomains(dsgvoDomain)
        })
        when: 'executing the UpdateAllClientDomainsUseCase'
        runUseCase(DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        unit = executeInTransaction {
            unitRepository.findById(unit.id).get().tap {
                //initialize lazy associations
                it.domains*.name
            }
        }
        then: 'the unit is moved to the new domain'
        unit.domains == [dsgvoTestDomain] as Set
    }



    def "Migrate a control with risk values"() {
        given: 'a client with an empty unit'
        RiskDefinitionRef riskDefinitionRef = RiskDefinitionRef.builder().idRef("xyz").build()
        ImplementationStatusRef implementationStatusRef = new ImplementationStatusRef(42)
        ControlRiskValues controlRiskValues = new ControlRiskValues(implementationStatusRef)
        Map riskValues = [
            (riskDefinitionRef) : controlRiskValues
        ]
        Unit unit = unitRepository.save(newUnit(client) {
            addToDomains(dsgvoDomain)
        })
        Control control = controlRepository.save(newControl(unit) {
            addToDomains(dsgvoDomain)
            setRiskValues(dsgvoDomain, riskValues)
        })
        when: 'executing the UpdateAllClientDomainsUseCase'
        runUseCase(DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        unit = executeInTransaction {
            unitRepository.findById(unit.id).get().tap {
                //initialize lazy associations
                it.domains*.name
            }
        }
        control = executeInTransaction {
            controlRepository.findById(control.id).get().tap {
                //initialize lazy associations
                it.getRiskValues(dsgvoTestDomain)
            }
        }

        then: "the control's risk values are moved to the new domain"
        control.riskValuesAspects.size() == 1
        with(((ControlData)control).riskValuesAspects.first()) {
            it.domain == dsgvoTestDomain
            with(it.values) {
                size() == 1
                get(riskDefinitionRef).implementationStatus.ordinalValue == 42
            }
        }
    }

    def "Migrate a client with the demo unit"() {
        given: 'a client with a demo unit'
        def demoUnit = createDemoUnitUseCase.execute(new CreateDemoUnitUseCase.InputData(client.id)).unit
        when: 'executing the UpdateAllClientDomainsUseCase'
        runUseCase(DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        demoUnit = executeInTransaction {
            unitRepository.findById(demoUnit.id).get().tap {
                //initialize lazy associations
                it.domains*.name
            }
        }
        then: 'the demo unit belongs to the new domain'
        demoUnit.domains == [dsgvoTestDomain] as Set
        and: 'the scope elements belong to the new domain'
        List<Scope> scopes = executeInTransaction {
            scopeRepository.query(client).whereOwnerIs(demoUnit).execute(PagingConfiguration.UNPAGED).resultPage.tap {
                //initialize lazy associations
                it.each {
                    it.customAspects*.domains*.name
                    it.links*.domains*.name
                    it.risks*.domains*.name
                }
            }
        }
        scopes.size() == 5
        scopes.each {
            with(it) {
                it.domains == [dsgvoTestDomain] as Set
                it.customAspects.every {
                    it.domains == [dsgvoTestDomain] as Set
                }
                it.links.every {
                    it.domains == [dsgvoTestDomain] as Set
                }
                it.subTypeAspects.every {
                    it.domain == dsgvoTestDomain
                }
                it.risks.every {
                    it.domains == [dsgvoTestDomain] as Set
                }
            }
        }
        and: 'the control elements belong to the new domain'
        List<Control> controls = executeInTransaction {
            controlRepository.query(client).whereOwnerIs(demoUnit).execute(PagingConfiguration.UNPAGED).resultPage.tap {
                //initialize lazy associations
                it.each {
                    it.customAspects*.domains*.name
                    it.links*.domains*.name
                    it.getRiskValues(dsgvoTestDomain)
                }
            }
        }
        controls.size() == 16
        controls.each {
            with(it) {
                it.domains == [dsgvoTestDomain] as Set
                it.customAspects.every {
                    it.domains == [dsgvoTestDomain] as Set
                }
                it.links.every {
                    it.domains == [dsgvoTestDomain] as Set
                }
                it.subTypeAspects.every {
                    it.domain == dsgvoTestDomain
                }
                it.riskValuesAspects*.domain == [dsgvoTestDomain]
            }
        }
        and: 'there is a responsible body'
        def responsibleBody = scopes.find{
            it.subTypeAspects.find {it.domain == dsgvoTestDomain}.subType =='SCP_ResponsibleBody'
        }
        responsibleBody != null
        and: "the responsible body's custom aspects are intact"

        with(responsibleBody) {
            subTypeAspects.size() == 1
            customAspects.size() == 4
            customAspects.every {
                it.domains == [dsgvoTestDomain] as Set
            }
            customAspects.find{it.type == 'scope_contactInformation'}.attributes.'scope_contactInformation_website' == 'www.data.de'
        }
        and: 'the old domain is removed from all processes'
        processRepository.findByDomain(dsgvoDomain).empty
    }



    def runUseCase(String domainTemplateId) {
        executeInTransaction {
            useCase.execute(new InputData(Key.uuidFrom(domainTemplateId)))
        }
    }
}