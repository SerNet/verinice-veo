/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Urs Zeidler
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
package org.veo.core.entity

import org.veo.core.entity.riskdefinition.CategoryDefinition
import org.veo.core.entity.riskdefinition.CategoryLevel
import org.veo.core.entity.riskdefinition.RiskDefinition
import org.veo.core.entity.riskdefinition.RiskValue

import spock.lang.Specification

class RiskMatrixSpec extends Specification {
    def "test RiskDefinition"() {
        when: "we create a simple RiskDefinition"

        RiskDefinition rd = new RiskDefinition()
        rd.id = "2"
        CategoryDefinition cd = new CategoryDefinition()
        rd.categories = [cd] as List

        rd.categories[0].potentialImpacts = [
            new CategoryLevel("l1", "", "", ""),
            new CategoryLevel("l2", "", "", "")
        ] as List

        then: "the ordinal value is set"
        rd.categories[0].potentialImpacts[0].ordinalValue == 0
        rd.categories[0].potentialImpacts[1].ordinalValue == 1
        when: "we add risk values"
        rd.id = "i1"
        rd.riskValues = [
            new RiskValue(10,"gering","1","Die bereits umgesetzten oder zumindest im Sicherheitskonzept vorgesehenen Sicherheitsmaßnahmen bieten einen ausreichenden Schutz. In der Praxis ist es üblich, geringe Risiken zu akzeptieren und die Gefährdung dennoch zu beobachten.","#004643","symbolic_risk_1"),
            new RiskValue(3,"mittel","2","Die bereits umgesetzten oder zumindest im Sicherheitskonzept vorgesehenen Sicherheitsmaßnahmen reichen möglicherweise nicht aus.","#234643","symbolic_risk_2"),
        ] as List

        then: "the ordial value is set"
        rd.riskValues[0].ordinalValue == 0
        rd.riskValues[1].ordinalValue == 1

        when: "we add a risk matrix for cd"
        cd.valueMatrix = [
            [
                rd.riskValues[0],
                rd.riskValues[1]
            ] as List,
            [
                rd.riskValues[0],
                rd.riskValues[1]
            ] as List
        ] as List

        then: "it validates nicely"
        rd.validateRiskDefinition()

        when: "we use an unkonwn risk value and validate"
        cd.valueMatrix = [
            [
                rd.riskValues[0],
                rd.riskValues[1]
            ] as List,
            [
                rd.riskValues[0],
                new RiskValue(5,"","","","#234643","symbolic_risk_5"),
            ] as List
        ] as List
        rd.validateRiskDefinition()
        then: "illegal Argument exception is thrown"
        IllegalArgumentException ex = thrown()
    }
}
