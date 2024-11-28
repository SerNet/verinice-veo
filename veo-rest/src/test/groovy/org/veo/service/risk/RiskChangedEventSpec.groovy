/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Alexander Koderman
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
package org.veo.service.risk

import static org.veo.core.entity.event.RiskEvent.ChangedValues.*

import org.veo.core.entity.AbstractRisk
import org.veo.core.entity.Client
import org.veo.core.entity.Identifiable
import org.veo.core.entity.RiskAffected
import org.veo.core.entity.Scenario
import org.veo.core.entity.event.RiskChangedEvent
import org.veo.test.VeoSpec

import spock.lang.Unroll

class RiskChangedEventSpec extends VeoSpec{

    final static def STABLE_UUID = 'f4eaa570-547c-4986-895d-5d7c6d40b063'

    @Unroll
    def "Should return #expected when event domain #eventDomain is compared with domain #checkDomain"() {
        given: "A RiskChangedEvent with specified changes and domain ID"
        def mockRisk = Stub(AbstractRisk) {
            getEntity() >> Stub(RiskAffected) {
                getId() >> UUID.randomUUID()
                getOwningClient() >> Optional.of(Stub(Client) {
                    getId() >> UUID.randomUUID()
                })
            }
            getScenario() >> Stub(Scenario) {
                getId() >> UUID.randomUUID()
            }
        }
        def event = new RiskChangedEvent(mockRisk, new Object())
        event = event.withDomainId(eventDomain)
        changes.each { event.addChange(it) }

        def domain = Stub(Identifiable) {
            getId() >> checkDomain
        }

        expect: "shouldReevaluate returns the expected value"
        event.shouldReevaluate(domain) == expected

        where:
        changes               | eventDomain               | checkDomain                  | expected
        [RISK_CREATED]        | null                      | null                      | true
        [RISK_DELETED]        | null                      | null                      | true
        [RISK_VALUES_CHANGED] | null                      | null                      | true
        [RISK_VALUES_CHANGED] | UUID.randomUUID()             | UUID.randomUUID()             | false
        [RISK_VALUES_CHANGED] | UUID.randomUUID()             | null                      | false
        [RISK_VALUES_CHANGED] | null                      | UUID.randomUUID()             | false
        [RISK_VALUES_CHANGED] | UUID.fromString(STABLE_UUID) | UUID.fromString(STABLE_UUID) | true
    }
}
