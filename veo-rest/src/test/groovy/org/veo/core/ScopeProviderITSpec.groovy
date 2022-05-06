/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
import org.springframework.transaction.annotation.Transactional

import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.risk.RiskDefinitionRef
import org.veo.core.repository.ClientRepository
import org.veo.core.repository.DomainRepository
import org.veo.core.repository.ProcessRepository
import org.veo.core.repository.ScopeRepository
import org.veo.core.repository.UnitRepository
import org.veo.core.usecase.base.ScopeProvider

@Transactional
class ScopeProviderITSpec extends VeoSpringSpec{
    @Autowired
    ScopeProvider scopeProvider

    @Autowired
    ClientRepository clientRepository

    @Autowired
    DomainRepository domainRepository

    @Autowired
    ProcessRepository processRepo

    @Autowired
    ScopeRepository scopeRepository

    @Autowired
    UnitRepository unitRepository

    Unit unit
    Domain domain
    RiskDefinitionRef rd1
    RiskDefinitionRef rd2

    def setup() {
        def client = createTestClient()
        unit = unitRepository.save(newUnit(client))
        domain = domainRepository.save(newDomain(client) {
            riskDefinitions = [
                rd1: createRiskDefinition("rd1"),
                rd2: createRiskDefinition("rd2"),
            ]
        })
        rd1 = RiskDefinitionRef.from(domain.riskDefinitions.rd1)
        rd2 = RiskDefinitionRef.from(domain.riskDefinitions.rd2)
        clientRepository.save(client)
    }

    def "direct scope member can use risk def"() {
        given: "a process that is a member of a scope that has risk def 2"
        def process = processRepo.save(newProcess(unit))
        scopeRepository.save(newScope(unit) {
            members = [process]
            setRiskDefinition(domain, rd1)
        })
        scopeRepository.save(newScope(unit) {
            members = [process]
            setRiskDefinition(domain, rd2)
        })

        expect: "process to be able to use risk def 2"
        scopeProvider.canUseRiskDefinition(process, domain, rd2)
    }

    def "element with no matching scope cannot use risk def"() {
        given: "a process that is a member of two scopes none of which have risk def 2"
        def process = processRepo.save(newProcess(unit))
        scopeRepository.save(newScope(unit) {
            members = [process]
            setRiskDefinition(domain, rd1)
        })
        scopeRepository.save(newScope(unit) {
            members = [process]
        })

        expect: "process to be unable to use risk def 2"
        !scopeProvider.canUseRiskDefinition(process, domain, rd2)
    }

    def "indirect scope member can use risk def"() {
        given: "a process whose grand parent process has risk def 2"
        def process = processRepo.save(newProcess(unit))
        processRepo.save(newProcess(unit) {
            parts = [process]
        })
        def parentProcess = processRepo.save(newProcess(unit) {
            parts = [process]
        })
        def grandParentProcess = processRepo.save(newProcess(unit) {
            parts = [parentProcess]
        })

        scopeRepository.save(newScope(unit) {
            members = [grandParentProcess]
            setRiskDefinition(domain, rd2)
        })

        expect: "process to be able to use risk def 2"
        scopeProvider.canUseRiskDefinition(process, domain, rd2)
    }

    def "second-degree relative cannot legitimize risk definition usage"() {
        given: "a process with an aunt/uncle process that has risk def 2"
        def process = processRepo.save(newProcess(unit))
        def parentProcess = processRepo.save(newProcess(unit) {
            parts = [process]
        })
        def auntOrUncleProcess = processRepo.save(newProcess(unit))
        processRepo.save(newProcess(unit) {
            parts = [
                parentProcess,
                auntOrUncleProcess
            ]
        })

        scopeRepository.save(newScope(unit) {
            members = [auntOrUncleProcess]
            setRiskDefinition(domain, rd2)
        })

        expect: "process to be unable to use risk def 2"
        !scopeProvider.canUseRiskDefinition(process, domain, rd2)
    }

    def "positive case with circular structure terminates"() {
        given: "an unusual process that is its own grand parent and whose great great grand parent process has risk def 2"
        def process = processRepo.save(newProcess(unit))
        def parentProcess = processRepo.save(newProcess(unit) {
            parts = [process]
        })
        // My family tree is a circle!
        process.parts.add(parentProcess)
        processRepo.save(process)

        def grandParentProcess = processRepo.save(newProcess(unit) {
            parts = [parentProcess]
        })
        def greatGrandParentProcess = processRepo.save(newProcess(unit) {
            parts = [grandParentProcess]
        })
        def greatGreatGrandParentProcess = processRepo.save(newProcess(unit) {
            parts = [greatGrandParentProcess]
        })

        scopeRepository.save(newScope(unit) {
            members = [greatGreatGrandParentProcess]
            setRiskDefinition(domain, rd2)
        })

        expect: "process to be able to use risk def 2"
        scopeProvider.canUseRiskDefinition(process, domain, rd2)
    }

    def "negative case with circular structure terminates"() {
        given: "an unusual process that is its own great grand parent"
        def process = processRepo.save(newProcess(unit))
        def parentProcess = processRepo.save(newProcess(unit) {
            parts = [process]
        })
        def grandParentProcess = processRepo.save(newProcess(unit) {
            parts = [parentProcess]
        })
        // My family tree is a circle!
        process.parts.add(grandParentProcess)
        processRepo.save(process)

        expect: "process to be unable to use risk def 2"
        !scopeProvider.canUseRiskDefinition(process, domain, rd2)
    }

    def "element with transient scope memberships can use risk definition"() {
        given: "scopes for rd1 & rd2 and a process"
        def rd1Scope = scopeRepository.save(newScope(unit) {
            setRiskDefinition(domain, rd1)
        })
        def rd2Scope = scopeRepository.save(newScope(unit) {
            setRiskDefinition(domain, rd2)
        })
        def process = processRepo.save(newProcess(unit))

        expect: "the process to be able to use rd2 with transient memberships in both scopes"
        scopeProvider.canUseRiskDefinition(process, domain, rd2, [rd1Scope, rd2Scope] as Set)

        and: "the process to be unable to use rd2 with transient membership in the rd1 scope"
        !scopeProvider.canUseRiskDefinition(process, domain, rd2, [rd1Scope] as Set)

        and:  "the process to be unable to use rd2 without transient memberships"
        !scopeProvider.canUseRiskDefinition(process, domain, rd2, [] as Set)

        when: "persisting a membership to the rd1 scope"
        rd2Scope.addMember(process)
        rd2Scope = scopeDataRepository.save(rd2Scope)

        then: "the process is able to use rd2 regardless of transient memberships"
        scopeProvider.canUseRiskDefinition(process, domain, rd2, [rd1Scope] as Set)
    }
}
