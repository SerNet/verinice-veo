/*
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
 */
package org.veo.core.entity

import org.veo.core.entity.condition.VeoExpression
import org.veo.core.entity.type.VeoType

import spock.lang.Specification

class ActionITSpec extends Specification {

    def 'risks cannot be created for #raType and #scenarioType as scenarios'() {
        given:
        def domain = Mock(Domain)
        def step = new AddRisksStep(Mock(VeoExpression) {
            getValueType(domain, raType) >> scenarioType
        })

        when:
        step.selfValidate(domain,raType)

        then:
        def ex = thrown(Exception)
        ex.message == expectedMessage

        where:
        raType               | scenarioType                                          | expectedMessage
        ElementType.DOCUMENT | VeoType.listOf(VeoType.element(ElementType.SCENARIO)) | "Cannot create risks for DOCUMENT"
        ElementType.PROCESS  | VeoType.nothing()                                     | "scenarios are required for risk creation: expected List<Scenario>, got Null"
        ElementType.PROCESS  | VeoType.bool()                                        | "scenarios are required for risk creation: expected List<Scenario>, got Boolean"
        ElementType.PROCESS  | VeoType.element(ElementType.SCENARIO)                 | "scenarios are required for risk creation: expected List<Scenario>, got Scenario"
        ElementType.PROCESS  | VeoType.listOf(VeoType.element(ElementType.PROCESS))  | "scenarios are required for risk creation: expected List<Scenario>, got List<Process>"
        ElementType.PROCESS  | VeoType.listOf(VeoType.nothing())                     | "scenarios are required for risk creation: expected List<Scenario>, got List<Null>"
    }
}
