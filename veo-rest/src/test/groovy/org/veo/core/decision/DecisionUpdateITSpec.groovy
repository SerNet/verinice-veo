/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.core.decision

import org.springframework.beans.factory.annotation.Autowired

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Unit
import org.veo.core.entity.condition.Condition
import org.veo.core.entity.condition.GreaterThanMatcher
import org.veo.core.entity.condition.PartCountExpression
import org.veo.core.entity.decision.DecisionRef
import org.veo.core.entity.event.ControlPartsChangedEvent
import org.veo.core.repository.ControlRepository
import org.veo.core.usecase.decision.Decider

import jakarta.transaction.Transactional

@Transactional
class DecisionUpdateITSpec extends VeoSpringSpec {
    @Autowired
    Decider decider

    @Autowired
    ControlRepository controlRepository

    Client client
    Unit unit
    Domain domain

    def setup() {
        client = createTestClient()
        domain = createTestDomain(client, TEST_DOMAIN_TEMPLATE_ID)
        domain.decisions.put("isGroup", newDecision("control", "CTL_TOM") {
            rules.add(newRule(true) {
                conditions.add(new Condition(new PartCountExpression("CTL_TOM"), new GreaterThanMatcher(BigDecimal.ZERO)))
            })
            defaultResultValue = false
        })
        domainDataRepository.save(domain)
        clientRepository.save(client)
        unit = unitDataRepository.save(newUnit(client))
    }

    def "updates decision on event"() {
        given: "a control in the domain"
        def control = controlDataRepository.save(newControl(unit) {
            associateWithDomain(domain, "CTL_TOM", "NEW")
        })

        when: "adding a part and updating decisions"
        def part = controlDataRepository.save(newControl(unit) {
            associateWithDomain(domain, "CTL_TOM", "NEW")
        })
        control.addPart(part)
        controlDataRepository.save(control)
        decider.updateDecisions(new ControlPartsChangedEvent(control, [] as Set))

        then: "the result for the part-dependent decision has been updated"
        with(controlDataRepository.findById(control.id, client.id).get()) {
            it.getDecisionResults(domain).get(new DecisionRef("isGroup")).value
        }

        when: "deleting the control and passing an event"
        control.removePart(part)
        controlDataRepository.delete(control)
        decider.updateDecisions(new ControlPartsChangedEvent(control, [part] as Set))

        then:
        notThrown(Exception)
    }
}
