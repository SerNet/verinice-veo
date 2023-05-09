/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
package org.veo.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails

import org.veo.core.repository.ClientRepository
import org.veo.core.repository.DocumentRepository
import org.veo.core.repository.UnitRepository
import org.veo.core.usecase.unit.CreateDemoUnitUseCase

import groovy.util.logging.Log
import spock.util.concurrent.PollingConditions

@WithUserDetails("admin")
@Log
class AdminControllerMvcITSpec extends ContentSpec {

    public static final String DSGVO_NAME = "DS-GVO DS-GVO"
    @Autowired
    private ClientRepository clientRepo
    @Autowired
    private UnitRepository unitRepo
    @Autowired
    private DocumentRepository documentRepo
    @Autowired
    private CreateDemoUnitUseCase createDemoUnitUseCase

    def "deletes client"() {
        given: "a client with some units and a document"
        def client = clientRepo.save(newClient {})
        def unit1 = unitDataRepository.save(newUnit(client))
        def unit2 = unitDataRepository.save(newUnit(client))
        def document = documentRepo.save(newDocument(unit1))

        when: "deleting the client"
        delete("/admin/client/${client.id.uuidValue()}")

        then:
        !clientRepo.exists(client.id)
        !unitRepo.exists(unit1.id)
        !unitRepo.exists(unit2.id)
        !documentRepo.exists(document.id)
    }

    def "generates unit dump"() {
        given: "a unit with a bunch of elements and risks"
        def client = createTestClient()
        createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID)
        createTestDomain(client, DSGVO_TEST_DOMAIN_TEMPLATE_ID)
        def domainId = parseJson(get("/domains")).find{it.name == "DSGVO-test"}.id
        def (unitId, assetId, scenarioId, processId) = createUnitWithElements(domainId)

        when: "requesting a unit dump"
        def dump = parseJson(get("/admin/unit-dump/$unitId"))

        then: "it contains the unit and all its elements"
        with(dump) {
            unit.name == "you knit"
            domains.size() == 1
            elements*.type.sort() == [
                "asset",
                "control",
                "document",
                "incident",
                "person",
                "process",
                "scenario",
                "scope"
            ]
            risks*._self.sort() == [
                "http://localhost/assets/$assetId/risks/$scenarioId",
                "http://localhost/processes/$processId/risks/$scenarioId"
            ]
        }
    }

    def "update client domains"() {
        given: "a unit with a bunch of elements and risks"
        def client = createTestClient()
        createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        createTestDomain(client, DSGVO_DOMAINTEMPLATE_V2_UUID)
        def domainId = parseJson(get("/domains")).find{it.templateVersion=="1.4.0"}.id
        def (unitId, assetId, scenarioId, processId) = createUnitWithRiskyElements(domainId)

        when: "the process risk values are preserved before migration"
        def json = parseJson(get("/admin/unit-dump/$unitId"))
        def risk = json.risks.find{it.process != null}
        def oldDomainName = risk.domains.(domainId).reference.displayName
        def oldRiskValues = risk.domains.(domainId).riskDefinitions.DSRA.riskValues
        def oldImpactValues = risk.domains.(domainId).riskDefinitions.DSRA.impactValues
        def oldProbability = risk.domains.(domainId).riskDefinitions.DSRA.probability

        and: 'updating all clients'
        post("/admin/domaintemplates/${DSGVO_DOMAINTEMPLATE_V2_UUID}/allclientsupdate", [:], 204)

        then: 'the elements and risks are transferred to the new domain'
        new PollingConditions().within(5) {
            with(parseJson(get("/admin/unit-dump/$unitId"))) {
                domains.size() == 1
                domains.first().templateVersion == '2.0.0'
                def newDomainId = domains.first().id
                elements.size() == 8
                elements.each {
                    assert it.domains.keySet() =~ [newDomainId]
                    it.customAspects.each { type, ca ->
                        assert ca.domains*.targetUri =~ [
                            "http://localhost/domains/$domainId"
                        ]
                    }
                    it.links.each { type, linksOfType ->
                        linksOfType.each {
                            assert it.domains*.targetUri =~ [
                                "http://localhost/domains/$domainId"
                            ]
                        }
                    }
                }
                // process is present with risk values:
                with(elements.find { it.id == processId }) {
                    name == "updated process"
                    domains.(newDomainId).riskValues.DSRA.potentialImpacts.size() == 2
                    domains.(newDomainId).riskValues.DSRA.potentialImpacts.C == 0
                    domains.(newDomainId).riskValues.DSRA.potentialImpacts.I == 1
                }

                // scenario is present with risk values:
                with(elements.find { it.id == scenarioId }) {
                    name == "scenario"
                    domains.(newDomainId).riskValues.DSRA.potentialProbability == 2
                }

                // asset risk is present without risk values:
                with(risks.find { it.asset != null }) {
                    domains.size() == 1
                    domains.(newDomainId) != null
                    domains.(newDomainId).reference.displayName == DSGVO_NAME
                    domains.(newDomainId).riskDefinitions.size() == 0
                }

                // process risk is present with risk values:
                with(risks.find { it.process != null }) {
                    domains.size() == 1
                    domains.(newDomainId) != null

                    domains.(newDomainId).reference.displayName == oldDomainName
                    domains.(newDomainId).riskDefinitions.DSRA.riskValues == oldRiskValues
                    domains.(newDomainId).riskDefinitions.DSRA.impactValues == oldImpactValues
                    domains.(newDomainId).riskDefinitions.DSRA.probability == oldProbability
                }
            }
        }
    }
}
