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
import org.veo.core.entity.riskdefinition.ImplementationStateDefinition
import org.veo.core.entity.riskdefinition.ProbabilityDefinition
import org.veo.core.entity.riskdefinition.ProbabilityLevel
import org.veo.core.entity.riskdefinition.RiskDefinition
import org.veo.core.entity.riskdefinition.RiskMethod
import org.veo.core.entity.riskdefinition.RiskValue

import spock.lang.Specification

class RiskMatrixSpec extends Specification {
    def "test CategoryDefinition equals"() {
        when: "comparing"

        CategoryDefinition cd = new CategoryDefinition()
        cd.id = "1"
        cd.name = "name"

        CategoryDefinition cd1 = new CategoryDefinition()
        cd1.id = "1"
        cd1.name = "name"

        then: "both are equals"
        cd == cd1

        when:"name is change"
        cd.name = "other name"
        then: "still equals"
        cd == cd1

        when:"id is change"
        cd.name = "name"
        cd.id = "2"
        then: "not equals"
        cd != cd1

        when:"content is different"
        cd.name = "name"
        cd.id = "1"
        cd.potentialImpacts = [
            new CategoryLevel("l1", "", "", "")
        ] as List
        then: "not equals"
        cd != cd1

        when:"content is the same"
        cd1.potentialImpacts = [
            new CategoryLevel("l1", "", "", "")
        ] as List
        then: "both are equals"
        cd == cd1

        when: "put in set"
        def set = [cd, cd1] as Set
        then: "only one remains"
        set.size() == 1
    }

    def "test CategoryDefinition validation"() {
        given: "some raw definitions"
        CategoryDefinition cd = new CategoryDefinition()
        cd.id = "1"
        cd.name = "name"

        def riskValues = [
            new RiskValue(10,"gering","1","Die bereits umgesetzten oder zumindest im Sicherheitskonzept vorgesehenen Sicherheitsmaßnahmen bieten einen ausreichenden Schutz. In der Praxis ist es üblich, geringe Risiken zu akzeptieren und die Gefährdung dennoch zu beobachten.","#004643","symbolic_risk_1"),
            new RiskValue(3,"mittel","2","Die bereits umgesetzten oder zumindest im Sicherheitskonzept vorgesehenen Sicherheitsmaßnahmen reichen möglicherweise nicht aus.","#234643","symbolic_risk_2"),
        ] as List

        def probabilityLevels = [
            new ProbabilityLevel("a", null, null, null),
            new ProbabilityLevel("b", null, null, null)
        ] as List
        def probabilities = new ProbabilityDefinition("P", null, null, probabilityLevels)

        when:"validate"
        cd.validateRiskCategory(riskValues, probabilities)
        then: "illegal Argument exception is thrown"
        IllegalArgumentException ex = thrown()
        ex.message == "Risk matrix is empty."

        when: "we add a risk matrix"
        def rmatrix = [
            [
                riskValues[0],
                riskValues[1]
            ] as List,
            [
                riskValues[0],
                riskValues[1]
            ] as List
        ] as List
        cd.valueMatrix = rmatrix

        and:"validate"
        cd.validateRiskCategory(riskValues, probabilities)
        then: "illegal Argument exception is thrown"
        ex = thrown()
        ex.message == "Value matrix does not conform to impacts."

        when: "we create and set the impact level"
        def impacts = [
            new CategoryLevel("l1", "", "", ""),
            new CategoryLevel("l2", "", "", "")
        ] as List

        cd.potentialImpacts = impacts
        then: "it validates ok"
        cd.validateRiskCategory(riskValues, probabilities)

        when: "we change the category levels"
        cd.potentialImpacts = [
            new CategoryLevel("l2", "", "", "")
        ] as List
        and:"validate"
        cd.validateRiskCategory(riskValues, probabilities)
        then: "illegal Argument exception is thrown"
        ex = thrown()
        ex.message == "Value matrix does not conform to impacts."

        when: "we change the risk matrix to the Category"
        cd.valueMatrix = [
            [
                riskValues[0],
                riskValues[1]
            ] as List
        ] as List
        then: "it validates ok"
        cd.validateRiskCategory(riskValues, probabilities)

        when: "we change the risk matrix "
        cd.valueMatrix = [
            [
                riskValues[0]
            ] as List
        ] as List
        cd.validateRiskCategory(riskValues, probabilities)
        then: "illegal Argument exception is thrown"
        ex = thrown()
        ex.message == "Value matrix does not conform to probability."

        when: "we create a complete CategoryDefinition"
        CategoryDefinition cd1 = new CategoryDefinition("2","name-1","nn","",rmatrix,impacts)
        then: "it validates ok"
        cd1.validateRiskCategory(riskValues, probabilities)
    }

    def "test ImplementationStateDefinition equals"() {
        when: "ImplementationStateDefinition"
        def isd1 =new ImplementationStateDefinition()
        def isd2 = new ImplementationStateDefinition()

        then: "both are equals"
        isd1 == isd2

        when: "changed"
        isd1.id = "id"
        then: "not equals"
        isd1 != isd2

        when: "set same id"
        isd2.id = "id"
        then: "both are equals"
        isd1 == isd2

        when: "the name is changed"
        isd2.name = "id"
        then: "both are equals"
        isd1 == isd2

        when: "the content is changed"
        isd1.levels = [
            new CategoryLevel("l1", "", "", "")
        ] as List
        then: "not equals"
        isd1 != isd2

        when: "when complete constructor is used"
        isd1 =new ImplementationStateDefinition("","","",[
            new CategoryLevel("l1", "", "", "")
        ] as List)
        isd2 =new ImplementationStateDefinition("","","",[
            new CategoryLevel("l1", "", "", "")
        ] as List)
        then: "both are equals"
        isd1 == isd2

        when: "put in set"
        def set = [isd1, isd2] as Set
        then: "only one remains"
        set.size() == 1
    }

    def "test ProbabilityDefinition equals"() {
        when: "ImplementationStateDefinition"
        def pd1 =new ProbabilityDefinition()
        def pd2 = new ProbabilityDefinition()

        then: "both are equals"
        pd1 == pd2

        when: "changed"
        pd1.id = "id"
        then: "not equals"
        pd1 != pd2

        when: "set same id"
        pd2.id = "id"
        then: "both are equals"
        pd1 == pd2

        when: "the name is changed"
        pd2.name = "id"
        then: "both are equals"
        pd1 == pd2

        when: "the content is changed"
        pd1.levels = [
            new ProbabilityLevel("l1", "", "", "")
        ] as List
        then: "not equals"
        pd1 != pd2

        when: "when complete constructor is used"
        pd1 =new ImplementationStateDefinition("","","",[
            new CategoryLevel("l1", "", "", "")
        ] as List)
        pd2 =new ImplementationStateDefinition("","","",[
            new CategoryLevel("l1", "", "", "")
        ] as List)
        then: "both are equals"
        pd1 == pd2

        when: "put in set"
        def set = [pd1, pd2] as Set
        then: "only one remains"
        set.size() == 1
    }

    def "test RiskDefinition equals"() {
        when: "two empty risk definitions"
        RiskDefinition rd1 = new RiskDefinition()
        RiskDefinition rd2 = new RiskDefinition()

        then: "both are equals"
        rd1 == rd2

        when: "one id is changed"
        rd1.id = "id"
        then: "not equals"
        rd1 != rd2

        when: "one id is the same"
        rd2.id = "id"
        then: "both are equals"
        rd1 == rd2

        when: "content differs"
        rd1.categories = [new CategoryDefinition()] as List
        then: "not equals"
        rd1 != rd2

        when: "content is the same"
        rd2.categories = [new CategoryDefinition()] as List
        then: "both are equals"
        rd1 == rd2

        when: "content differs"
        rd1.probability = new ProbabilityDefinition("test", null, null, [
            new ProbabilityLevel("l1", "", "", "")
        ] as List)
        then: "not equals"
        rd1 != rd2

        when: "content is the same"
        rd2.probability = new ProbabilityDefinition("test", null, null, [
            new ProbabilityLevel("l1", "", "", "")
        ] as List)
        then: "both are equals"
        rd1 == rd2

        when: "content differs"
        rd1.implementationStateDefinition = new ImplementationStateDefinition("test", null, null, [
            new CategoryLevel("l1", "", "", "")
        ] as List)
        then: "not equals"
        rd1 != rd2

        when: "content is the same"
        rd2.implementationStateDefinition = new ImplementationStateDefinition("test", null, null, [
            new CategoryLevel("l1", "", "", "")
        ] as List)
        then: "both are equals"
        rd1 == rd2

        when: "content differs"
        rd1.riskMethod = new RiskMethod()
        rd1.riskMethod.impactMethod = "sum"
        then: "not equals"
        rd1 != rd2

        when: "content is the same"
        rd2.riskMethod = new RiskMethod()
        rd2.riskMethod.impactMethod = "sum"
        then: "both are equals"
        rd1 == rd2

        when: "put in set"
        def set = [rd1, rd2] as Set
        then: "only one remains"
        set.size() == 1
    }

    def "test RiskDefinition validation"() {
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

        when: "we validate"
        rd.validateRiskDefinition()
        then: "illegal Argument exception is thrown"
        thrown(IllegalArgumentException)

        when: "we add the probability levels"
        rd.probability = new ProbabilityDefinition()
        rd.probability.levels = [
            new ProbabilityLevel("a", null, null, null),
            new ProbabilityLevel("b", null, null, null)
        ] as List

        rd.validateRiskDefinition()
        then: "illegal Argument exception is thrown"
        thrown(IllegalArgumentException)

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
        thrown(IllegalArgumentException)

        when: "we fix the definition"
        cd.valueMatrix = [
            [
                rd.riskValues[0],
                rd.riskValues[1]
            ] as List,
            [
                rd.riskValues[0],
                rd.riskValues[0]
            ] as List
        ] as List
        then: "it validates nicely"
        rd.validateRiskDefinition()

        when: "wrong impact levels"
        rd.categories[0].potentialImpacts = [
            new CategoryLevel("l1", "", "", ""),
            new CategoryLevel("l2", "", "", ""),
            new CategoryLevel("l3", "", "", "")
        ] as List
        rd.validateRiskDefinition()
        then: "illegal Argument exception is thrown"
        thrown(IllegalArgumentException)

        when: "we fix the definition"
        cd.valueMatrix = [
            [
                rd.riskValues[1],
                rd.riskValues[0]
            ] as List,
            [
                rd.riskValues[0],
                rd.riskValues[0]
            ] as List,
            [
                rd.riskValues[0],
                rd.riskValues[0]
            ] as List
        ] as List
        then: "it validates nicely"
        rd.validateRiskDefinition()

        and: "we get some values from the risk matrix of the category"
        cd.getRiskValue(rd.probability.levels[0], cd.potentialImpacts[0]) == rd.riskValues[1]
        cd.getRiskValue(rd.probability.levels[0], cd.potentialImpacts[2]) == rd.riskValues[0]

        when: "the probability is not right"
        ProbabilityLevel pl = new ProbabilityLevel("a", null, null, null)
        pl.ordinalValue = 10
        cd.getRiskValue(pl,  cd.potentialImpacts[0])
        then: "illegal Argument exception is thrown"
        thrown(IllegalArgumentException)

        when: "the implact level is not right"
        CategoryLevel cl = new CategoryLevel("l1", "", "", "")
        cl.setOrdinalValue(10)
        cd.getRiskValue(rd.probability.levels[0], cl)
        then: "illegal Argument exception is thrown"
        thrown(IllegalArgumentException)

        when: "the implact level is still not right but in range"
        cl = new CategoryLevel("l1", "", "", "")
        cl.setOrdinalValue(2)
        cd.getRiskValue(rd.probability.levels[0], cl)
        then: "illegal Argument exception is thrown"
        thrown(IllegalArgumentException)

        when: "the implact level is right"
        cl = new CategoryLevel("l1", "", "", "")
        cl.setOrdinalValue(0)
        then: "the value is returned"
        cd.getRiskValue(rd.probability.levels[0], cl) == rd.riskValues[1]
    }

    def "test RiskDefinition validation categories"() {
        when: "we create a simple RiskDefinition"

        RiskDefinition rd = new RiskDefinition()
        rd.probability = new ProbabilityDefinition()
        rd.probability.levels = [
            new ProbabilityLevel("a", null, null, null)
        ] as List

        rd.riskValues = [
            new RiskValue(3,"mittel","2","","#234643","symbolic_risk_2"),
        ] as List

        def riskMatrix = [
            [
                rd.riskValues[0]
            ] as List
        ] as List

        def potentialImpacts = [
            new CategoryLevel("l2", "", "", "")
        ] as List

        rd.categories = [
            new CategoryDefinition("1", null, null, null, riskMatrix, potentialImpacts),
            new CategoryDefinition("2", null, null, null, riskMatrix, potentialImpacts),
            new CategoryDefinition("3", null, null, null, riskMatrix, potentialImpacts),
            new CategoryDefinition("4", null, null, null, riskMatrix, potentialImpacts),
            new CategoryDefinition("5", null, null, null, riskMatrix, potentialImpacts)
        ] as List

        then: "it validates nicely"
        rd.validateRiskDefinition()

        when: "we change make the ids not unique"
        rd.categories[0].id = "2"
        rd.validateRiskDefinition()
        then: "illegal Argument exception is thrown"
        IllegalArgumentException ex = thrown()
        ex.message == "Categories not unique."
    }

    def "test RiskDefinition validation of symbolic risk"() {
        when: "we create a simple RiskDefinition"

        RiskDefinition rd = new RiskDefinition()
        rd.probability = new ProbabilityDefinition()
        rd.probability.levels = [
            new ProbabilityLevel("a", null, null, null)
        ] as List

        rd.riskValues = [
            new RiskValue(3,"mittel","2","","#234643","symbolic_risk_2"),
            new RiskValue(3,"mittel","2","","#234643","symbolic_risk_3")
        ] as List

        def riskMatrix = [
            [
                rd.riskValues[0]
            ] as List
        ] as List

        def potentialImpacts = [
            new CategoryLevel("l2", "", "", "")
        ] as List

        rd.categories = [
            new CategoryDefinition("1", null, null, null, riskMatrix, potentialImpacts),
            new CategoryDefinition("2", null, null, null, riskMatrix, potentialImpacts),
            new CategoryDefinition("3", null, null, null, riskMatrix, potentialImpacts),
            new CategoryDefinition("4", null, null, null, riskMatrix, potentialImpacts),
            new CategoryDefinition("5", null, null, null, riskMatrix, potentialImpacts)
        ] as List

        then: "it validates nicely"
        rd.validateRiskDefinition()

        when: "we change make the sybolic risk not unique"
        rd.riskValues[0].symbolicRisk = "symbolic_risk_3"
        rd.validateRiskDefinition()
        then: "illegal Argument exception is thrown"
        IllegalArgumentException ex = thrown()
        ex.message == "SymbolicRisk not unique."

        when: "make unique again"
        rd.riskValues[1].symbolicRisk = "new"
        then: "it validates nicely"
        rd.validateRiskDefinition()
    }

    def "test RiskDefinition getCategory"() {
        when: ""
        RiskDefinition rd = new RiskDefinition()
        rd.categories = [
            new CategoryDefinition("1", null, null, null, [], []),
            new CategoryDefinition("2", null, null, null, [], []),
            new CategoryDefinition("3", null, null, null, [], []),
            new CategoryDefinition("4", null, null, null, [], []),
            new CategoryDefinition("5", null, null, null, [], [])
        ] as List

        then:"we get the right elements back"
        rd.getCategory("1").get() == rd.categories[0]
        rd.getCategory("5").get() == rd.categories[4]
        rd.getCategory("6").isEmpty()
    }
}
