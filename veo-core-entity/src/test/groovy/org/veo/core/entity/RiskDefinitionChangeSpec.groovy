/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Urs Zeidler
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
import org.veo.core.entity.riskdefinition.DiscreteValue
import org.veo.core.entity.riskdefinition.ProbabilityLevel
import org.veo.core.entity.riskdefinition.RiskDefinition
import org.veo.core.entity.riskdefinition.RiskDefinitionChange
import org.veo.core.entity.riskdefinition.RiskMethod
import org.veo.core.entity.riskdefinition.RiskValue

import spock.lang.Specification

class RiskDefinitionChangeSpec extends Specification{
    protected static final Locale DE = Locale.GERMAN

    def "change translation"() {
        given:
        def rdNew = createRiskDefinition()
        def rdOld = createRiskDefinition()

        when: "compare two equal rd"
        def changes = RiskDefinitionChange.detectChanges(rdOld, rdNew)

        then: "no changes"
        changes.isEmpty()

        when: "change translation and color"
        rdNew.riskValues.first().htmlColor = "#00000"
        rdNew.riskValues.first().getTranslations(DE).name = "a new name"

        changes = RiskDefinitionChange.detectChanges(rdOld, rdNew)

        then: "both changes"
        changes ==~ [
            new RiskDefinitionChange.TranslationDiff(),
            new RiskDefinitionChange.ColorDiff()
        ]
    }

    def "add/remove implementationState value"() {
        given:
        def rdOld = createRiskDefinition()
        def rdNew = createRiskDefinition()

        when: "add probability value"
        rdNew.implementationStateDefinition.levels.add(new CategoryLevel("color-1"))
        def changes = RiskDefinitionChange.detectChanges(rdOld, rdNew)

        then: "change is detected"
        changes ==~ [
            new RiskDefinitionChange.ImplementationStateListResize()
        ]

        when: "remove probability value"
        rdNew = createRiskDefinition()
        rdNew.implementationStateDefinition.levels.removeLast()
        changes = RiskDefinitionChange.detectChanges(rdOld, rdNew)

        then: "change is detected"
        changes ==~ [
            new RiskDefinitionChange.ImplementationStateListResize()
        ]
    }

    def "add/remove probability value"() {
        given:
        def rdOld = createRiskDefinition()
        def rdNew = createRiskDefinition()

        when: "add probability value"
        rdNew.probability.levels.add(new ProbabilityLevel("b"))
        def changes = RiskDefinitionChange.detectChanges(rdOld, rdNew)

        then: "change is detected"
        changes ==~ [
            new RiskDefinitionChange.ProbabilityListResize(rdOld)
        ]

        when: "remove probability value"
        rdNew = createRiskDefinition()
        rdNew.probability.levels.removeLast()
        changes = RiskDefinitionChange.detectChanges(rdOld, rdNew)

        then: "change is detected"
        changes ==~ [
            new RiskDefinitionChange.ProbabilityListResize(rdOld)
        ]
    }

    def "add/remove risk value"() {
        given:
        def rdOld = createRiskDefinition()
        def rdNew = createRiskDefinition()

        when: "add risk value"
        rdNew = createRiskDefinition()
        rdNew.riskValues.add( new RiskValue(2, "#A0CF11", "symbolic_risk_3", new Translated([
            (DE): new DiscreteValue.NameAbbreviationAndDescription("gering", "1","hh")
        ])))
        def changes = RiskDefinitionChange.detectChanges(rdOld, rdNew)

        then: "change is detected"
        changes ==~ [
            new RiskDefinitionChange.RiskValueListResize()
        ]

        when: "remove risk value"
        rdNew = createRiskDefinition()
        rdNew.riskValues.removeLast()
        changes = RiskDefinitionChange.detectChanges(rdOld, rdNew)

        then: "change is detected"
        changes ==~ [
            new RiskDefinitionChange.RiskValueListResize()
        ]
    }

    def "change risk matrix value"() {
        given:
        def rdNew = createRiskDefinition()
        def rdOld = createRiskDefinition()

        when: "add risk matrix"
        rdNew.categories.find { it.id=="5" }.valueMatrix[0][0] = rdNew.riskValues[1]
        def changes = RiskDefinitionChange.detectChanges(rdOld, rdNew)

        then: "change is detected"
        changes.size() ==~ 1
        changes[0] instanceof RiskDefinitionChange.RiskMatrixDiff
        changes[0].cat.id == "5"
    }

    def "add/remove risk matrix"() {
        given:
        def rdNew = createRiskDefinition()
        def rdOld = createRiskDefinition()

        when: "add risk matrix"
        rdNew.categories.find { it.id=="6" }.valueMatrix = [rdNew.riskValues[0]]
        def changes = RiskDefinitionChange.detectChanges(rdOld, rdNew)

        then: "change is detected"
        changes.size() == 1
        changes[0] instanceof RiskDefinitionChange.RiskMatrixAdd
        changes[0].cat.id == "6"

        when: "remove risk matrix"
        rdNew = createRiskDefinition()
        rdNew.categories.find { it.id=="5" }.valueMatrix = null
        changes = RiskDefinitionChange.detectChanges(rdOld, rdNew)

        then: "change is detected"
        changes.size() == 1
        changes[0] instanceof RiskDefinitionChange.RiskMatrixRemove
        changes[0].cat.id == "5"
    }

    def "add/remove risk category"() {
        given:
        def rdNew = createRiskDefinition()
        def rdOld = createRiskDefinition()

        when: "add risk category"
        rdNew.categories.add(new CategoryDefinition("7", null,  [
            new CategoryLevel("l1"),
            new CategoryLevel("l2")
        ]))
        def changes = RiskDefinitionChange.detectChanges(rdOld, rdNew)

        then: "change is detected"
        changes.size() == 1
        changes[0] instanceof RiskDefinitionChange.CategoryListAdd
        changes[0].newCat.id == "7"

        when: "remove risk category"
        rdNew = createRiskDefinition()
        rdNew.categories.removeLast()
        changes = RiskDefinitionChange.detectChanges(rdOld, rdNew)

        then: "change is detected"
        changes.size() == 1
        changes[0] instanceof RiskDefinitionChange.CategoryListRemove
        changes[0].oldCat.id == "6"
    }

    def "add/remove category impact"() {
        given:
        def rdNew = createRiskDefinition()
        def rdOld = createRiskDefinition()

        when: "add category impact"
        rdNew.categories.find { it.id=="6" }.potentialImpacts.add(new CategoryLevel("I1"))
        def changes = RiskDefinitionChange.detectChanges(rdOld, rdNew)

        then: "change is detected"
        changes.size() == 1
        changes[0] instanceof RiskDefinitionChange.ImpactListResize
        changes[0].cat.id == "6"

        when: "remove category impact"
        rdNew = createRiskDefinition()
        rdNew.categories.find { it.id=="6" }.potentialImpacts.removeLast()
        changes = RiskDefinitionChange.detectChanges(rdOld, rdNew)

        then: "change is detected"
        changes.size() == 1
        changes[0] instanceof RiskDefinitionChange.ImpactListResize
        changes[0].cat.id == "6"
    }

    private RiskDefinition createRiskDefinition() {
        RiskDefinition rd = new RiskDefinition()
        rd.id= "simple-id"
        rd.riskMethod = new RiskMethod()
        rd.probability.levels = [
            new ProbabilityLevel("a")
        ]

        rd.riskValues = [
            new RiskValue(0, "#A0CF11", "symbolic_risk_1", new Translated([
                (DE): new DiscreteValue.NameAbbreviationAndDescription("gering", "1", "hh")
            ])),
            new RiskValue(1, "#FFFF13", "symbolic_risk_2", new Translated([
                (DE): new DiscreteValue.NameAbbreviationAndDescription("mittel", "2", "Die bereits umgesetzten oder zumindest im Sicherheitskonzept vorgesehenen Sicherheitsmaßnahmen reichen möglicherweise nicht aus.")
            ])),
        ]

        def potentialImpacts = [
            new CategoryLevel("l2")
        ]

        rd.categories = [
            new CategoryDefinition("1", [
                [rd.riskValues[0]]
            ], [
                new CategoryLevel("l2")
            ]),
            new CategoryDefinition("2", [
                [rd.riskValues[0]]
            ], [
                new CategoryLevel("l2")
            ]),
            new CategoryDefinition("3", [
                [rd.riskValues[0]]
            ], [
                new CategoryLevel("l2")
            ]),
            new CategoryDefinition("4", [
                [rd.riskValues[0]]
            ], [
                new CategoryLevel("l2")
            ]),
            new CategoryDefinition("5", [
                [rd.riskValues[0]]
            ], [
                new CategoryLevel("l2")
            ]),
            new CategoryDefinition("6", null, [
                new CategoryLevel("l2")
            ])
        ]

        rd.implementationStateDefinition.levels = [
            new CategoryLevel("color-1")
        ]

        rd
    }
}
