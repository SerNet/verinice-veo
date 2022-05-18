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

import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Process
import org.veo.core.entity.Scenario
import org.veo.core.entity.Scope
import org.veo.core.entity.Unit
import org.veo.core.entity.risk.ControlRiskValues
import org.veo.core.entity.risk.DomainRiskReferenceProvider
import org.veo.core.entity.risk.ImplementationStatusRef
import org.veo.core.entity.risk.PotentialProbabilityImpl
import org.veo.core.entity.risk.ProbabilityRef
import org.veo.core.entity.risk.ProcessImpactValues
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.core.repository.ControlRepository
import org.veo.core.repository.PagingConfiguration
import org.veo.core.repository.PersonRepository
import org.veo.core.repository.ScenarioRepository
import org.veo.core.usecase.domain.UpdateAllClientDomainsUseCase
import org.veo.core.usecase.domain.UpdateAllClientDomainsUseCase.InputData
import org.veo.core.usecase.unit.CreateDemoUnitUseCase
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.ScopeRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.ControlData
import org.veo.persistence.entity.jpa.ProcessData
import org.veo.persistence.entity.jpa.ScenarioData
import org.veo.persistence.entity.jpa.ScopeData

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

    @Autowired
    PersonRepository personRepository

    @Autowired
    ScenarioRepository scenarioRepository

    Client client
    Domain dsgvoDomain
    Domain dsgvoDomainV2

    def setup() {
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_UUID)
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_V2_UUID)
        executeInTransaction {
            client = newClient()
            dsgvoDomain = domainTemplateService.createDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
            dsgvoDomain.riskDefinitions = [
                "DSRA":createRiskDefinition("DSRA"),
                "xyz":createRiskDefinition("xyz")
            ]
            dsgvoDomainV2 = domainTemplateService.createDomain(client, DSGVO_DOMAINTEMPLATE_V2_UUID)
            client.addToDomains(dsgvoDomain)
            client.addToDomains(dsgvoDomainV2)
            client = clientRepository.save(client)
        }
    }

    def "Migrate an empty client"() {
        when: 'executing the UpdateAllClientDomainsUseCase'
        runUseCase(DSGVO_DOMAINTEMPLATE_V2_UUID)
        client = clientRepository.findById(client.id).get()
        then: 'the old domain is disabled'
        !client.getAllDomains().find{it.id == dsgvoDomain.id}.active
        client.domains.find{it.id == dsgvoDomainV2.id}.active
    }

    def "Migrate an empty unit"() {
        given: 'a client with an empty unit'
        Unit unit = unitRepository.save(newUnit(client) {
            addToDomains(dsgvoDomain)
        })
        when: 'executing the UpdateAllClientDomainsUseCase'
        runUseCase(DSGVO_DOMAINTEMPLATE_V2_UUID)
        unit = executeInTransaction {
            unitRepository.findById(unit.id).get().tap {
                //initialize lazy associations
                it.domains*.name
            }
        }
        then: 'the unit is moved to the new domain'
        unit.domains == [dsgvoDomainV2] as Set
    }



    def "Migrate a control with risk values"() {
        given: 'a client with an empty unit'
        RiskDefinitionRef riskDefinitionRef = new RiskDefinitionRef("xyz")
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
        runUseCase(DSGVO_DOMAINTEMPLATE_V2_UUID)
        unit = executeInTransaction {
            unitRepository.findById(unit.id).get().tap {
                //initialize lazy associations
                it.domains*.name
            }
        }
        control = executeInTransaction {
            controlRepository.findById(control.id).get().tap {
                //initialize lazy associations
                it.getRiskValues(dsgvoDomainV2)
            }
        }

        then: "the control's risk values are moved to the new domain"
        control.riskValuesAspects.size() == 1
        with(((ControlData)control).riskValuesAspects.first()) {
            it.domain == dsgvoDomainV2
            with(it.values) {
                size() == 1
                get(riskDefinitionRef).implementationStatus.ordinalValue == 42
            }
        }
    }

    def "Migrate a scope referencing a risk definition"() {
        given: 'a unit with a scope that references a risk definition'
        def unit = unitRepository.save(newUnit(client) {
            addToDomains(dsgvoDomain)
        })
        def scope = scopeRepository.save(newScope(unit) {
            addToDomains(dsgvoDomain)
            setRiskDefinition(dsgvoDomain, new RiskDefinitionRef("xyz"))
        })

        when: 'executing the UpdateAllClientDomainsUseCase'
        runUseCase(DSGVO_DOMAINTEMPLATE_V2_UUID)
        scope = executeInTransaction {
            scopeRepository.findById(scope.id).get().tap{
                // init lazy associations
                ((ScopeData)it).getRiskDefinition(dsgvoDomainV2)
            }
        }

        then: "the scope's risk definition ref is moved to the new domain"
        with(((ScopeData)scope).riskValuesAspects) {
            size() == 1
            first().domain == dsgvoDomainV2
            first().riskDefinitionRef.idRef == "xyz"
        }
    }

    def "Migrate a scenario with probability"() {
        given: 'a unit with a scenario containing a probability'
        RiskDefinitionRef riskDefinitionRef = new RiskDefinitionRef("xyz")
        ProbabilityRef probabilityRef = new ProbabilityRef(2)
        PotentialProbabilityImpl probability = new PotentialProbabilityImpl(probabilityRef)
        Map riskValues = [
            (riskDefinitionRef) : probability
        ]
        Unit unit = unitRepository.save(newUnit(client) {
            addToDomains(dsgvoDomain)
        })
        Scenario scenario = scenarioRepository.save(newScenario(unit) {
            addToDomains(dsgvoDomain)
            setPotentialProbability(dsgvoDomain, riskValues)
        })

        when: 'executing the UpdateAllClientDomainsUseCase'
        runUseCase(DSGVO_DOMAINTEMPLATE_V2_UUID)
        unit = executeInTransaction {
            unitRepository.findById(unit.id).get().tap {
                //initialize lazy associations
                it.domains*.name
            }
        }
        scenario = executeInTransaction {
            scenarioRepository.findById(scenario.id).get().tap {
                //initialize lazy associations
                it.getPotentialProbability(dsgvoDomainV2)
            }
        }

        then: "the scenario probability is moved to the new domain"
        scenario.riskValuesAspects.size() == 1
        /* TODO VEO-1171 - the next 7 lines should be replaced by:
         * scenario.getPotentialProbability(dsgvoTestDomain).orElseThrow().get(riskDefinitionRef).potentialProbability.idRef == 2
         * But scenario.getPotentialProbability(dsgvoTestDomain) does not return a result */
        with(((ScenarioData)scenario).riskValuesAspects.first()) {
            it.domain == dsgvoDomainV2
            with(it.potentialProbability) {
                size() == 1
                get(riskDefinitionRef).potentialProbability.idRef == 2
            }
        }
    }

    def "Migrate a process with risk values"() {
        given: 'a client with an empty unit'
        RiskDefinitionRef riskDefinitionRef = new RiskDefinitionRef("xyz")
        DomainRiskReferenceProvider riskreferenceProvider = DomainRiskReferenceProvider.referencesForDomain(dsgvoDomain)

        ProcessImpactValues processImpactValues = new ProcessImpactValues()
        def categoryref = riskreferenceProvider.getCategoryRef(riskDefinitionRef.getIdRef(), "C").get()
        def impactValue = riskreferenceProvider.getImpactRef(riskDefinitionRef.getIdRef(), categoryref.getIdRef(), new BigDecimal("2")).get()
        processImpactValues.potentialImpacts = [(categoryref) : impactValue]
        Map impactValues = [
            (riskDefinitionRef) : processImpactValues
        ]
        Unit unit = unitRepository.save(newUnit(client) {
            addToDomains(dsgvoDomain)
        })
        Process process = processRepository.save(newProcess(unit) {
            addToDomains(dsgvoDomain)
            setImpactValues(dsgvoDomain, impactValues)
        })
        when: 'executing the UpdateAllClientDomainsUseCase'
        runUseCase(DSGVO_DOMAINTEMPLATE_V2_UUID)
        unit = executeInTransaction {
            unitRepository.findById(unit.id).get().tap {
                //initialize lazy associations
                it.domains*.name
            }
        }
        process = executeInTransaction {
            processRepository.findById(process.id).get().tap {
                //initialize lazy associations
                it.getImpactValues(dsgvoDomainV2)
            }
        }

        then: "the control's risk values are moved to the new domain"
        process.riskValuesAspects.size() == 1
        with(((ProcessData)process).riskValuesAspects.first()) {
            it.domain == dsgvoDomainV2
            with(it.values) {
                size() == 1
                get(riskDefinitionRef) == processImpactValues
            }
        }
    }

    def "Migrate a client with the demo unit"() {
        given: 'a client with a demo unit'
        def demoUnit = createDemoUnitUseCase.execute(new CreateDemoUnitUseCase.InputData(client.id)).unit
        when: 'executing the UpdateAllClientDomainsUseCase'
        runUseCase(DSGVO_DOMAINTEMPLATE_V2_UUID)
        demoUnit = executeInTransaction {
            unitRepository.findById(demoUnit.id).get().tap {
                //initialize lazy associations
                it.domains*.name
            }
        }
        then: 'the demo unit belongs to the new domain'
        demoUnit.domains == [dsgvoDomainV2] as Set
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
        scopes.size() == 1
        with(scopes.first()) {
            domains == [dsgvoDomainV2] as Set
            customAspects.size() == 4
            customAspects.every {
                it.domains == [dsgvoDomainV2] as Set
            }
            it.links.every {
                it.domains == [dsgvoDomainV2] as Set
            }
            subTypeAspects.size() == 1
            subTypeAspects.every {
                it.domain == dsgvoDomainV2
            }
            risks.every {
                it.domains == [dsgvoDomainV2] as Set
            }
        }
        and: 'the person elements belong to the new domain'
        def persons = executeInTransaction {
            personRepository.query(client).whereOwnerIs(demoUnit).execute(PagingConfiguration.UNPAGED).resultPage.tap {
                //initialize lazy associations
                it.each {
                    it.customAspects*.domains*.name
                    it.links*.domains*.name
                }
            }
        }
        persons.size() == 2
        persons.each {
            with(it) {
                it.domains == [dsgvoDomainV2] as Set
                it.customAspects.every {
                    it.domains == [dsgvoDomainV2] as Set
                }
                it.links.every {
                    it.domains == [dsgvoDomainV2] as Set
                }
                it.subTypeAspects.every {
                    it.domain == dsgvoDomainV2
                }
            }
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