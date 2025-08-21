/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.usecase.unit.GetUnitDumpUseCase

@WithUserDetails("user@domain.example")
class GetUnitDumpUseCaseITSpec extends VeoSpringSpec {
    Client client
    Unit unit
    Domain testDomain
    Domain dsgvoDomain

    @Autowired
    GetUnitDumpUseCase getUnitDumpUseCase

    def setup() {
        client = createTestClient()
        testDomain = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID)
        dsgvoDomain = createTestDomain(client, DSGVO_DOMAINTEMPLATE_UUID)
        client = clientRepository.save(client)

        unit = unitDataRepository.save(newUnit(client) {
            domains = [testDomain, dsgvoDomain]
        })
    }

    def "only exports elements in target domain"() {
        given:
        def dsgvoPerson = personDataRepository.save(newPerson(unit) {
            name = "dsgvo person"
            associateWithDomain(dsgvoDomain,"AST_Datatype", "NEW")
        })
        def testDomainAsset = personDataRepository.save(newPerson(unit) {
            name = "test domain asset"
            associateWithDomain(testDomain, "Information", "CURRENT")
        })
        processDataRepository.save(newProcess(unit) {
            name = "multi-domain process"
            associateWithDomain(testDomain, "BusinessProcess", "NEW")
            applyLink(newCustomLink(testDomainAsset, "necessaryData", testDomain))

            associateWithDomain(dsgvoDomain, "PRO_DataProcessing", "NEW")
            applyLink(newCustomLink(dsgvoPerson, "process_dataType", dsgvoDomain))
        })

        when:
        def dump = dumpUnit(testDomain)

        then:
        dump.elements.size() == 2
        dump.elements*.name ==~ [
            "test domain asset",
            "multi-domain process"
        ]
        with(dump.elements.find { it.name == "multi-domain process" }) {
            it.domains ==~ [testDomain]
            it.links*.type == ["necessaryData"]
        }
    }

    def "omits parts & members in other domain"() {
        given: "a composite and scope containing elements from different domains"
        executeInTransaction {
            def dsgvoPerson = personDataRepository.save(newPerson(unit) {
                name = "dsgvo person"
                associateWithDomain(dsgvoDomain, "PER_Person", "NEW")
            })
            def testDomainPerson = personDataRepository.save(newPerson(unit) {
                name = "test domain person"
                associateWithDomain(testDomain, "Programmer", "CODING")
            })
            personDataRepository.save(newPerson(unit) {
                name = "multi-domain team"
                associateWithDomain(testDomain, "PER_Person", "NEW")
                associateWithDomain(dsgvoDomain, "Programmer", "CODING")
                parts = [dsgvoPerson, testDomainPerson]
            })
            scopeDataRepository.save(newScope(unit) {
                name = "multi-domain company"
                associateWithDomain(testDomain, "Company", "NEW")
                associateWithDomain(dsgvoDomain, "SCP_Scope", "NEW")
                members = [dsgvoPerson, testDomainPerson]
            })
        }

        when: "exporting unit with test domain"
        def dump = dumpUnit(testDomain)

        then: "part / member from other domain is excluded"
        with(dump.elements.find { it.name == "multi-domain team" }) {
            it.domains ==~ [testDomain]
            it.parts*.name ==~ ["test domain person"]
        }
        with(dump.elements.find { it.name == "multi-domain company" }) {
            it.domains ==~ [testDomain]
            it.members*.name ==~ ["test domain person"]
        }
    }

    def "omits risks, mitigations & risk owners from other domain"() {
        given: "two risks for scenarios from different domains"
        executeInTransaction {
            def dsgvoPerson = personDataRepository.save(newPerson(unit) {
                name = "dsgvo person"
                associateWithDomain(dsgvoDomain, "PER_Person", "NEW")
            })
            def dsgvoControl = controlDataRepository.save(newControl(unit) {
                name = "dsgvo control"
                associateWithDomain(dsgvoDomain, "CTL_TOM", "NEW")
            })
            def dsgvoScenario = scenarioDataRepository.save(newScenario(unit) {
                name = "dsgvo scenario"
                associateWithDomain(dsgvoDomain, "SCN_Scenario", "NEW")
            })
            def testDomainScenario = scenarioDataRepository.save(newScenario(unit) {
                name = "test domain scenario"
                associateWithDomain(testDomain, "Attack", "NEW")
            })
            scopeDataRepository.save(newScope(unit) {
                name = "multi-domain company"
                associateWithDomain(testDomain, "Company", "NEW")
                associateWithDomain(dsgvoDomain, "SCP_Scope", "NEW")
                obtainRisk(dsgvoScenario).tap{
                    designator = "RSK-1"
                }
                obtainRisk(testDomainScenario).tap{
                    designator = "RSK-2"
                    mitigate(dsgvoControl)
                    appoint(dsgvoPerson)
                }
            })
        }

        when: "exporting unit with test domain"
        def dump = dumpUnit(testDomain)

        then: "risk, mitigation & risk owner from other domain are excluded"
        with(dump.elements.find { it.name == "multi-domain company" }) {
            risks*.scenario*.name ==~ ["test domain scenario"]
            with(risks[0]) {
                domains*.name ==~ ["test-domain"]
                mitigation == null
                riskOwner == null
            }
        }
    }

    private GetUnitDumpUseCase.OutputData dumpUnit(Domain domain) {
        executeInTransaction {
            getUnitDumpUseCase.execute(new GetUnitDumpUseCase.InputData(unit.id, domain.id), userAccessRightsProvider.accessRights)
        }
    }
}
