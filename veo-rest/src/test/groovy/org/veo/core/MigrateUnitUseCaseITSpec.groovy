/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jochen Kemnade.
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

import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer
import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.ElementType
import org.veo.core.entity.Process
import org.veo.core.entity.Scenario
import org.veo.core.entity.Scope
import org.veo.core.entity.TranslatedText
import org.veo.core.entity.Unit
import org.veo.core.entity.condition.CustomAspectAttributeValueExpression
import org.veo.core.entity.definitions.attribute.BooleanAttributeDefinition
import org.veo.core.entity.domainmigration.CustomAspectAttribute
import org.veo.core.entity.domainmigration.CustomAspectMigrationTransformDefinition
import org.veo.core.entity.domainmigration.DomainMigrationDefinition
import org.veo.core.entity.domainmigration.DomainMigrationStep
import org.veo.core.entity.risk.DomainRiskReferenceProvider
import org.veo.core.entity.risk.ImpactValues
import org.veo.core.entity.risk.PotentialProbability
import org.veo.core.entity.risk.ProbabilityRef
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.core.repository.ControlRepository
import org.veo.core.repository.PagingConfiguration
import org.veo.core.repository.PersonRepository
import org.veo.core.repository.ScenarioRepository
import org.veo.core.usecase.MigrationFailedException
import org.veo.core.usecase.catalogitem.ApplyProfileIncarnationDescriptionUseCase
import org.veo.core.usecase.catalogitem.GetProfileIncarnationDescriptionUseCase
import org.veo.core.usecase.unit.MigrateUnitUseCase
import org.veo.core.usecase.unit.MigrateUnitUseCase.InputData
import org.veo.persistence.access.AssetRepositoryImpl
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.ProcessRepositoryImpl
import org.veo.persistence.access.ScopeRepositoryImpl
import org.veo.persistence.access.UnitRepositoryImpl
import org.veo.persistence.entity.jpa.AssetData
import org.veo.persistence.entity.jpa.ProcessData
import org.veo.persistence.entity.jpa.ScenarioData
import org.veo.persistence.entity.jpa.ScopeData
import org.veo.rest.security.NoRestrictionAccessRight

@WithUserDetails("user@domain.example")
class MigrateUnitUseCaseITSpec extends VeoSpringSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository

    @Autowired
    private UnitRepositoryImpl unitRepository

    @Autowired
    private MigrateUnitUseCase useCase

    @Autowired
    private GetProfileIncarnationDescriptionUseCase getProfileIncarnationDescriptionUseCase

    @Autowired
    private ApplyProfileIncarnationDescriptionUseCase applyProfileIncarnationDescriptionUseCase

    @Autowired
    ScopeRepositoryImpl scopeRepository

    @Autowired
    AssetRepositoryImpl assetRepository

    @Autowired
    ProcessRepositoryImpl processRepository

    @Autowired
    ControlRepository controlRepository

    @Autowired
    PersonRepository personRepository

    @Autowired
    ScenarioRepository scenarioRepository

    @Autowired
    EntityToDtoTransformer t

    Client client
    Domain dsgvoDomain
    Domain dsgvoDomainV2

    def setup() {
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_UUID)
        createTestDomainTemplate(DSGVO_DOMAINTEMPLATE_V2_UUID)
        executeInTransaction {
            client = newClient()
            dsgvoDomain = domainTemplateService.createDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
            dsgvoDomain.riskDefinitions.xyz = createRiskDefinition("xyz")
            dsgvoDomainV2 = domainTemplateService.createDomain(client, DSGVO_DOMAINTEMPLATE_V2_UUID)
            dsgvoDomainV2.riskDefinitions.xyz = createRiskDefinition("xyz")//TODO: this must be done in the domain migration #3077

            client.addToDomains(dsgvoDomain)
            client.addToDomains(dsgvoDomainV2)
            client = clientRepository.save(client)
            dsgvoDomain = client.getDomains().find { it.domainTemplate.id == DSGVO_DOMAINTEMPLATE_UUID }
            dsgvoDomainV2 = client.getDomains().find { it.domainTemplate.id == DSGVO_DOMAINTEMPLATE_V2_UUID }
        }
    }

    def "Migrate an empty unit"() {
        given: 'a client with an empty unit'
        Unit unit = unitRepository.save(newUnit(client) {
            addToDomains(dsgvoDomain)
        })

        when: 'executing the UpdateAllClientDomainsUseCase'
        runUseCase(unit.id)
        unit = executeInTransaction {
            unitRepository.findById(unit.id).get().tap {
                //initialize lazy associations
                it.domains*.name
            }
        }

        then: 'the unit is moved to the new domain'
        unit.domains == [dsgvoDomainV2] as Set
    }

    def "Migrate a scope referencing a risk definition"() {
        given: 'a unit with a scope that references a risk definition'
        RiskDefinitionRef riskDefinitionRef = new RiskDefinitionRef("xyz")
        DomainRiskReferenceProvider riskreferenceProvider = DomainRiskReferenceProvider.referencesForDomain(dsgvoDomain)

        def categoryref = riskreferenceProvider.getCategoryRef(riskDefinitionRef.getIdRef(), "C")
        def impactValue = riskreferenceProvider.getImpactRef(riskDefinitionRef.getIdRef(), categoryref.getIdRef(), new BigDecimal("2"))
        ImpactValues scopeImpactValues = new ImpactValues([(categoryref) : impactValue])
        Map impactValues = [
            (riskDefinitionRef) : scopeImpactValues
        ]

        def unit = unitRepository.save(newUnit(client) {
            addToDomains(dsgvoDomain)
        })
        def scope = scopeRepository.save(newScope(unit) {
            associateWithDomain(dsgvoDomain, "SCP_Scope", "NEW")
            setRiskDefinition(dsgvoDomain, riskDefinitionRef)
            setImpactValues(dsgvoDomain, impactValues)
        })

        when: 'executing the UpdateAllClientDomainsUseCase'
        runUseCase(unit.id)
        scope = executeInTransaction {
            scopeRepository.findById(scope.id).get().tap{
                // init lazy associations
                ((ScopeData)it).getRiskDefinition(dsgvoDomainV2)
                ((ScopeData)it).getImpactValues(dsgvoDomainV2)
            }
        }

        then: "the scope's risk definition ref is moved to the new domain"
        with(((ScopeData)scope).scopeRiskValuesAspects) {
            size() == 1
            first().domain == dsgvoDomainV2
            first().riskDefinitionRef.idRef == "xyz"
        }

        and: "the scope risk values are moved to the new domain"
        scope.riskValuesAspects.size() == 1
        with(((ScopeData)scope).riskValuesAspects.first()) {
            it.domain == dsgvoDomainV2
            with(it.values) {
                size() == 1
                get(riskDefinitionRef) == scopeImpactValues
            }
        }
    }

    def "Migrate a scenario with probability"() {
        given: 'a unit with a scenario containing a probability'
        RiskDefinitionRef riskDefinitionRef = new RiskDefinitionRef("xyz")
        ProbabilityRef probabilityRef = new ProbabilityRef(2)
        PotentialProbability probability = new PotentialProbability(probabilityRef)
        Map riskValues = [
            (riskDefinitionRef) : probability
        ]
        Unit unit = unitRepository.save(newUnit(client) {
            addToDomains(dsgvoDomain)
        })
        Scenario scenario = scenarioRepository.save(newScenario(unit) {
            associateWithDomain(dsgvoDomain, "SCN_Scenario", "NEW")
            setPotentialProbability(dsgvoDomain, riskValues)
        })

        when: 'executing the UpdateAllClientDomainsUseCase'
        runUseCase(unit.id)
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

    def "Migrate an asset with risk values"() {
        given: 'a client with an empty unit'
        RiskDefinitionRef riskDefinitionRef = new RiskDefinitionRef("xyz")
        DomainRiskReferenceProvider riskreferenceProvider = DomainRiskReferenceProvider.referencesForDomain(dsgvoDomain)

        def categoryref = riskreferenceProvider.getCategoryRef(riskDefinitionRef.getIdRef(), "C")
        def impactValue = riskreferenceProvider.getImpactRef(riskDefinitionRef.getIdRef(), categoryref.getIdRef(), new BigDecimal("2"))
        ImpactValues assetImpactValues = new ImpactValues([(categoryref) : impactValue])
        Map impactValues = [
            (riskDefinitionRef) : assetImpactValues
        ]
        Unit unit = unitRepository.save(newUnit(client) {
            addToDomains(dsgvoDomain)
        })
        Asset asset = assetRepository.save(newAsset(unit) {
            associateWithDomain(dsgvoDomain, "AST_Datatype", "NEW")
            setImpactValues(dsgvoDomain, impactValues)
        })

        when: 'executing the UpdateAllClientDomainsUseCase'
        runUseCase(unit.id)
        unit = executeInTransaction {
            unitRepository.findById(unit.id).get().tap {
                //initialize lazy associations
                it.domains*.name
            }
        }
        asset = executeInTransaction {
            assetRepository.findById(asset.id).get().tap {
                //initialize lazy associations
                it.getImpactValues(dsgvoDomainV2)
                (it as AssetData).riskValuesAspects*.domain*.name
            }
        }

        then: "the asset's risk values are moved to the new domain"
        asset.riskValuesAspects.size() == 1
        with(((AssetData)asset).riskValuesAspects.first()) {
            it.domain == dsgvoDomainV2
            with(it.values) {
                size() == 1
                get(riskDefinitionRef) == assetImpactValues
            }
        }
    }

    def "Migrate a process with risk values"() {
        given: 'a client with an empty unit'
        RiskDefinitionRef riskDefinitionRef = new RiskDefinitionRef("xyz")
        DomainRiskReferenceProvider riskreferenceProvider = DomainRiskReferenceProvider.referencesForDomain(dsgvoDomain)

        def categoryref = riskreferenceProvider.getCategoryRef(riskDefinitionRef.getIdRef(), "C")
        def impactValue = riskreferenceProvider.getImpactRef(riskDefinitionRef.getIdRef(), categoryref.getIdRef(), new BigDecimal("2"))
        ImpactValues processImpactValues = new ImpactValues([(categoryref) : impactValue])
        Map impactValues = [
            (riskDefinitionRef) : processImpactValues
        ]
        Unit unit = unitRepository.save(newUnit(client) {
            addToDomains(dsgvoDomain)
        })
        Process process = processRepository.save(newProcess(unit) {
            associateWithDomain(dsgvoDomain, "PRO_DataProcessing", "NEW")
            setImpactValues(dsgvoDomain, impactValues)
        })

        when: 'executing the UpdateAllClientDomainsUseCase'
        runUseCase(unit.id)
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

        then: "the process's risk values are moved to the new domain"
        process.riskValuesAspects.size() == 1
        with(((ProcessData)process).riskValuesAspects.first()) {
            it.domain == dsgvoDomainV2
            with(it.values) {
                size() == 1
                get(riskDefinitionRef) == processImpactValues
            }
        }
    }

    def "Migrate a client with elements from a profile"() {
        given: 'a profile applied in a unit'
        def unit = executeInTransaction {
            unitRepository.save(newUnit(client)).tap{unit ->
                def profileId = dsgvoDomain.profiles.find { it.name == "Beispielorganisation" }.id
                var incarnationDescriptions = getProfileIncarnationDescriptionUseCase.execute(
                        new GetProfileIncarnationDescriptionUseCase.InputData(unit.id, dsgvoDomain.id, null, profileId, false), NoRestrictionAccessRight.from(client.idAsString)
                        ).references
                applyProfileIncarnationDescriptionUseCase.execute(
                        new ApplyProfileIncarnationDescriptionUseCase.InputData(unit.id, incarnationDescriptions), NoRestrictionAccessRight.from(client.idAsString)
                        )
            }
        }

        when: 'executing the UpdateAllClientDomainsUseCase'
        runUseCase(unit.id)
        unit = executeInTransaction {
            unitRepository.findById(unit.id).get().tap {
                //initialize lazy associations
                it.domains*.name
            }
        }

        then: 'the unit belongs to the new domain'
        unit.domains == [dsgvoDomainV2] as Set

        and: 'the scope elements belong to the new domain'
        List<Scope> scopes = executeInTransaction {
            scopeRepository.query(client).with {
                whereOwnerIs(unit)
                execute(PagingConfiguration.UNPAGED)
            }.resultPage.each {
                //initialize lazy associations
                it.customAspects*.domain.name
                it.links*.domain.name
                it.risks*.domains*.name
            }
        }
        scopes.size() == 1
        with(scopes.first()) {
            domains == [dsgvoDomainV2] as Set
            customAspects.size() == 4
            customAspects.every {
                it.domain == dsgvoDomainV2
            }
            it.links.every {
                it.domain == dsgvoDomainV2
            }
            domainAssociations.size() == 1
            domainAssociations.every {
                it.domain == dsgvoDomainV2
            }
            risks.every {
                it.domains == [dsgvoDomainV2] as Set
            }
        }

        and: 'the person elements belong to the new domain'
        def persons = executeInTransaction {
            personRepository.query(client).with {
                whereOwnerIs(unit)
                execute(PagingConfiguration.UNPAGED)
            }.resultPage.each {
                //initialize lazy associations
                it.customAspects*.domain.name
                it.links*.domain*.name
            }
        }
        persons.size() == 5
        persons.each {
            with(it) {
                it.domains == [dsgvoDomainV2] as Set
                it.customAspects.every {
                    it.domain == dsgvoDomainV2
                }
                it.links.every {
                    it.domain == dsgvoDomainV2
                }
                it.domainAssociations.every {
                    it.domain == dsgvoDomainV2
                }
            }
        }
    }

    def "Migration fails due to conflict in migrated CA attribute"() {
        given: "an attribute that is renamed in a new domain version"
        def domainA1 = domainDataRepository.save(newDomain(client) {
            templateVersion = "1.0.0"
            applyElementTypeDefinition(newElementTypeDefinition(it, ElementType.PROCESS) {
                subTypes.PRO_gram = newSubTypeDefinition {}
                customAspects.put("performance", newCustomAspectDefinition {
                    attributeDefinitions.isFast = new BooleanAttributeDefinition()
                })
            })
        })
        def domainA2 = domainDataRepository.save(newDomain(client) {
            templateVersion = "2.0.0"
            applyElementTypeDefinition(newElementTypeDefinition(it, ElementType.PROCESS) {
                subTypes.PRO_gram = newSubTypeDefinition {}
                customAspects.put("performance", newCustomAspectDefinition {
                    attributeDefinitions.isVeryFast = new BooleanAttributeDefinition()
                })
                domainMigrationDefinition = new DomainMigrationDefinition([
                    (new DomainMigrationStep("rename", new TranslatedText([:]), [
                        new CustomAspectAttribute(ElementType.PROCESS, "performance", "isFast"),
                    ], [
                        new CustomAspectMigrationTransformDefinition(
                        new CustomAspectAttributeValueExpression("performance", "isFast"),
                        new CustomAspectAttribute(ElementType.PROCESS, "performance", "isVeryFast"),
                        )
                    ]))
                ])
            })
        })

        and: "another domain that defines it just like A2"
        def domainB = domainDataRepository.save(newDomain(client) {
            applyElementTypeDefinition(newElementTypeDefinition(it, ElementType.PROCESS) {
                subTypes.PRO_gram = newSubTypeDefinition {}
                customAspects.put("performance", newCustomAspectDefinition {
                    attributeDefinitions.isVeryFast = new BooleanAttributeDefinition()
                })
            })
        })

        and: "a process with conflicted values for the CA"
        def unit = unitRepository.save(newUnit(client) {
            addToDomains(Set.of(domainA1, domainB))
        })
        def process = processRepository.save(newProcess(unit) {
            associateWithDomain(domainA1, "PRO_gram", "NEW")
            associateWithDomain(domainB, "PRO_gram", "NEW")
            applyCustomAspect(newCustomAspect("performance", domainA1) {
                attributes = [
                    isFast: false
                ]
            })
            applyCustomAspect(newCustomAspect("performance", domainB) {
                attributes = [
                    isVeryFast: true
                ]
            })
        })

        when: "migrating the process to the new version"
        runUseCase(unit.id,domainA1.id, domainA2.id)

        then:"a conflict is detected"
        thrown(MigrationFailedException)

        when: "resolving the conflict"
        process.applyCustomAspectAttribute(domainB, "performance", "isVeryFast", false)
        process = processRepository.save(process)

        and: "re-attempting the migration"
        runUseCase(unit.id,domainA1.id, domainA2.id)
        process = processRepository.findById(process.id).get()

        then: "migration succeeded"
        noExceptionThrown()
        !process.isAssociatedWithDomain(domainA1)
        process.findCustomAspect(domainA2, "performance").get().attributes["isVeryFast"] == false
        process.findCustomAspect(domainB, "performance").get().attributes["isVeryFast"] == false
    }

    def "Migration fails with conflicted CA (non-breaking change)"() {
        given: "a CA that is extended in the new DSGVO"
        def caDef = dsgvoDomainV2
                .getElementTypeDefinition(ElementType.DOCUMENT)
                .getCustomAspectDefinition("document_details")

        and: "another domain that defines it just like the new DSGVO"
        def otherDomain = domainDataRepository.save(newDomain(client) {
            applyElementTypeDefinition(newElementTypeDefinition(it, ElementType.DOCUMENT) {
                subTypes.DOC_Document = newSubTypeDefinition {}
                customAspects.put("document_details", caDef)
            })
        })

        and: "a document with conflicted values for the CA"
        def unit = unitRepository.save(newUnit(client) {
            addToDomains(Set.of(dsgvoDomain, otherDomain))
        })
        def document = documentDataRepository.save(newDocument(unit) {
            associateWithDomain(dsgvoDomain, "DOC_Document", "RELEASED")
            associateWithDomain(otherDomain, "DOC_Document", "NEW")
            applyCustomAspect(newCustomAspect("document_details", dsgvoDomain) {
                attributes = [
                    document_details_version: "draft-1"
                ]
            })
            applyCustomAspect(newCustomAspect("document_details", otherDomain) {
                attributes = [
                    document_details_version: "0.0.1"
                ]
            })
        })

        when: "migrating the document to the new DSGVO version"
        runUseCase(unit.id,dsgvoDomain.id, dsgvoDomainV2.id)

        then:
        thrown(MigrationFailedException)

        when: "resolving the conflict"
        document.applyCustomAspectAttribute(otherDomain, "document_details", "document_details_version", "draft-1")
        document = documentDataRepository.save(document)

        and: "re-attempting the migration"
        runUseCase(unit.id,dsgvoDomain.id, dsgvoDomainV2.id)
        document = documentDataRepository.findById(document.id).get()

        then: "migration succeeded"
        noExceptionThrown()
        !document.isAssociatedWithDomain(dsgvoDomain)
        document.findCustomAspect(dsgvoDomainV2, "document_details").get().attributes["document_details_version"] == "draft-1"
        document.findCustomAspect(otherDomain, "document_details").get().attributes["document_details_version"] == "draft-1"
    }

    def runUseCase(UUID unitId, UUID domainIdOld = dsgvoDomain.id, UUID domainIdNew = dsgvoDomainV2.id) {
        executeInTransaction {
            useCase.execute(new InputData(unitId, domainIdOld, domainIdNew), NoRestrictionAccessRight.from(client.idAsString))
        }
    }
}
