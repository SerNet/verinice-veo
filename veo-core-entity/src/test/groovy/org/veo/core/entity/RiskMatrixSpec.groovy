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

import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator

import org.veo.core.entity.riskdefinition.CategoryDefinition
import org.veo.core.entity.riskdefinition.CategoryLevel
import org.veo.core.entity.riskdefinition.ImplementationStateDefinition
import org.veo.core.entity.riskdefinition.ProbabilityDefinition
import org.veo.core.entity.riskdefinition.ProbabilityLevel
import org.veo.core.entity.riskdefinition.RiskDefinition
import org.veo.core.entity.riskdefinition.RiskMethod
import org.veo.core.entity.riskdefinition.RiskValue
import org.veo.core.entity.specification.TranslationValidator

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import spock.lang.Shared
import spock.lang.Specification

class RiskMatrixSpec extends Specification {
    @Shared
    private RiskDefinition myRisk = createRiskDefinition()

    def "test CategoryDefinition equals"() {
        when: "comparing"
        CategoryDefinition cd = new CategoryDefinition()
        cd.id = "1"

        CategoryDefinition cd1 = new CategoryDefinition()
        cd1.id = "1"

        then: "both are equals"
        cd == cd1

        when:"id is change"
        cd.id = "2"

        then: "not equals"
        cd != cd1

        when:"content is different"
        cd.id = "1"
        cd.potentialImpacts = [
            new CategoryLevel("color-1")
        ]

        then: "not equals"
        cd != cd1

        when:"content is the same"
        cd1.potentialImpacts = [
            new CategoryLevel("color-1")
        ]

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

        def riskValues = [
            new RiskValue(10,"#004643","symbolic_risk_1"),
            new RiskValue(3,"#234643","symbolic_risk_2"),
        ]

        def probabilityLevels = [
            new ProbabilityLevel("#004643"),
            new ProbabilityLevel("#004643")
        ]
        def probabilities = new ProbabilityDefinition(probabilityLevels)

        when:"validate"
        cd.validateRiskCategory(riskValues, probabilities)

        then: "illegal Argument exception is thrown"
        IllegalArgumentException ex = thrown()
        ex.message == "Category 1 does not support risk values."

        when: "we add a risk matrix"
        def rmatrix = [
            [
                riskValues[0],
                riskValues[1]
            ],
            [
                riskValues[0],
                riskValues[1]
            ]
        ]
        cd.valueMatrix = rmatrix

        and:"validate"
        cd.validateRiskCategory(riskValues, probabilities)

        then: "illegal Argument exception is thrown"
        ex = thrown()
        ex.message == "Value matrix for category 1 does not conform to impacts."

        when: "we create and set the impact level"
        def impacts = [
            new CategoryLevel("color-1"),
            new CategoryLevel("color-2")
        ]

        cd.potentialImpacts = impacts

        then: "it validates ok"
        cd.validateRiskCategory(riskValues, probabilities)

        when: "we change the category levels"
        cd.potentialImpacts = [
            new CategoryLevel("color-1")
        ]

        and:"validate"
        cd.validateRiskCategory(riskValues, probabilities)

        then: "illegal Argument exception is thrown"
        ex = thrown()
        ex.message == "Value matrix for category 1 does not conform to impacts."

        when: "we change the risk matrix to the Category"
        cd.valueMatrix = [
            [
                riskValues[0],
                riskValues[1]
            ]
        ]

        then: "it validates ok"
        cd.validateRiskCategory(riskValues, probabilities)

        when: "we change the risk matrix "
        cd.valueMatrix = [
            [
                riskValues[0]
            ]
        ]
        cd.validateRiskCategory(riskValues, probabilities)

        then: "illegal Argument exception is thrown"
        ex = thrown()
        ex.message == "Value matrix for category 1 does not conform to probability."

        when: "we create a complete CategoryDefinition"
        CategoryDefinition cd1 = new CategoryDefinition("2",rmatrix,impacts)

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

        when: "the content is changed"
        isd1.levels = [
            new CategoryLevel("color-1")
        ]

        then: "not equals"
        isd1 != isd2

        when: "when complete constructor is used"
        isd1 =new ImplementationStateDefinition([
            new CategoryLevel("color-1")
        ])
        isd2 =new ImplementationStateDefinition([
            new CategoryLevel("color-1")
        ])

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

        when: "the content is changed"
        pd1.levels = [
            new ProbabilityLevel("color-1")
        ]

        then: "not equals"
        pd1 != pd2

        when: "when complete constructor is used"
        pd1 =new ImplementationStateDefinition([
            new CategoryLevel("color-1")
        ])
        pd2 =new ImplementationStateDefinition([
            new CategoryLevel("color-1")
        ])

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
        rd1.categories = [new CategoryDefinition()]

        then: "not equals"
        rd1 != rd2

        when: "content is the same"
        rd2.categories = [new CategoryDefinition()]

        then: "both are equals"
        rd1 == rd2

        when: "content differs"
        rd1.probability.levels = [
            new ProbabilityLevel("color-1")
        ]

        then: "not equals"
        rd1 != rd2

        when: "content is the same"
        rd2.probability = new ProbabilityDefinition( [
            new ProbabilityLevel("color-1")
        ])

        then: "both are equals"
        rd1 == rd2

        when: "content differs"
        rd1.implementationStateDefinition.levels = [
            new CategoryLevel("color-1")
        ]

        then: "not equals"
        rd1 != rd2

        when: "content is the same"
        rd2.implementationStateDefinition.levels = [
            new CategoryLevel("color-1")
        ]

        then: "both are equals"
        rd1 == rd2

        when: "content differs"
        rd1.riskMethod = new RiskMethod()
        rd1.riskMethod.translations.put(Locale.of("DE"), ["impactMethod": "sum"])

        then: "not equals"
        rd1 != rd2

        when: "content is the same"
        rd2.riskMethod = new RiskMethod()
        rd2.riskMethod.translations.put(Locale.of("DE"), ["impactMethod": "sum"])

        then: "both are equals"
        rd1 == rd2

        when: "put in set"
        def set = [rd1, rd2] as Set

        then: "only one remains"
        set.size() == 1
    }

    def "test RiskDefinition getCategory"() {
        given: "a risk definition with five categories"
        RiskDefinition rd = new RiskDefinition()
        rd.categories = [
            new CategoryDefinition("1", [], []),
            new CategoryDefinition("2", [], []),
            new CategoryDefinition("3", [], []),
            new CategoryDefinition("4", [], []),
            new CategoryDefinition("5", [], [])
        ]

        expect:"that the right elements are returned"
        rd.getCategory("1").get() == rd.categories[0]
        rd.getCategory("5").get() == rd.categories[4]
        rd.getCategory("6").isEmpty()
    }

    def "test RiskDefinition validation"() {
        when: "we create a simple RiskDefinition"
        RiskDefinition rd = new RiskDefinition()
        rd.id = "2"
        rd.riskMethod = new RiskMethod()
        CategoryDefinition cd = new CategoryDefinition()
        cd.id = "c"
        rd.categories = [cd]

        rd.categories[0].potentialImpacts = [
            new CategoryLevel("color-1"),
            new CategoryLevel("color-2")
        ]

        then: "the ordinal value is set"
        rd.categories[0].potentialImpacts[0].ordinalValue == 0
        rd.categories[0].potentialImpacts[1].ordinalValue == 1

        when: "we add risk values"
        rd.id = "i1"
        rd.riskValues = [
            new RiskValue(10,"#004643","symbolic_risk_1"),
            new RiskValue(3,"#234643","symbolic_risk_2"),
        ]

        then: "the ordinal value is set"
        rd.riskValues[0].ordinalValue == 0
        rd.riskValues[1].ordinalValue == 1

        when: "removing definitions"
        rd.probability = null
        rd.implementationStateDefinition = null

        then: "there are constraint violations"
        with(getJakartaViolations(rd)) { violations ->
            violations.size() == 2
            violations*.propertyPath*.toString() ==~ [
                "probability",
                "implementationStateDefinition"
            ]
            violations*.message ==~ [
                "must not be null",
                "must not be null"
            ]
        }

        when: "we add the missing definitions"
        rd.probability = new ProbabilityDefinition()
        rd.probability.id = ProbabilityDefinition.DIMENSION_PROBABILITY
        rd.probability.levels = [
            new ProbabilityLevel("color-1"),
            new ProbabilityLevel("color-2")
        ]
        rd.implementationStateDefinition = new ImplementationStateDefinition([
            new CategoryLevel("color-1")
        ])

        then: "no more violations are present"
        getJakartaViolations(rd).empty

        when: "calling validation"
        rd.validateRiskDefinition()

        then: "no exception is thrown"
        noExceptionThrown()

        when: "we add a risk matrix for cd"
        cd.valueMatrix = [
            [
                rd.riskValues[0],
                rd.riskValues[1]
            ],
            [
                rd.riskValues[0],
                rd.riskValues[1]
            ]
        ]

        rd.validateRiskDefinition()

        then: "it's fine"
        noExceptionThrown()

        when: "we use an unknown risk value and validate"
        cd.valueMatrix = [
            [
                rd.riskValues[0],
                rd.riskValues[1]
            ],
            [
                rd.riskValues[0],
                new RiskValue(5,"#234643","symbolic_risk_5"),
            ]
        ]
        rd.validateRiskDefinition()

        then: "illegal Argument exception is thrown"
        IllegalArgumentException iae = thrown()
        iae.message == "Invalid risk values for category c: [RiskValue(symbolicRisk=symbolic_risk_5)]"

        when: "we fix the definition"
        cd.valueMatrix = [
            [
                rd.riskValues[0],
                rd.riskValues[1]
            ],
            [
                rd.riskValues[0],
                rd.riskValues[0]
            ]
        ]

        then: "it validates nicely"
        rd.validateRiskDefinition()

        when: "wrong impact levels"
        rd.categories[0].potentialImpacts = [
            new CategoryLevel("color-1"),
            new CategoryLevel("color-2"),
            new CategoryLevel("color-3")
        ]
        rd.validateRiskDefinition()

        then: "illegal Argument exception is thrown"
        iae = thrown()
        iae.message == "Value matrix for category c does not conform to impacts."

        when: "we fix the definition"
        cd.valueMatrix = [
            [
                rd.riskValues[1],
                rd.riskValues[0]
            ],
            [
                rd.riskValues[0],
                rd.riskValues[0]
            ],
            [
                rd.riskValues[0],
                rd.riskValues[0]
            ]
        ]

        then: "it validates nicely"
        rd.validateRiskDefinition()

        and: "we get some values from the risk matrix of the category"
        cd.getRiskValue(rd.probability.levels[0], cd.potentialImpacts[0]) == rd.riskValues[1]
        cd.getRiskValue(rd.probability.levels[0], cd.potentialImpacts[2]) == rd.riskValues[0]

        when: "the probability is not right"
        ProbabilityLevel pl = new ProbabilityLevel("color-1")
        pl.ordinalValue = 10
        cd.getRiskValue(pl,  cd.potentialImpacts[0])

        then: "illegal Argument exception is thrown"
        iae = thrown()
        iae.message == "No risk value for probability: 10"

        when: "the implact level is not right"
        CategoryLevel cl = new CategoryLevel("color-1")
        cl.setOrdinalValue(10)
        cd.getRiskValue(rd.probability.levels[0], cl)

        then: "illegal Argument exception is thrown"
        iae = thrown()
        iae.message == "CategoryLevel not part of potentialImpacts: CategoryLevel(super=DiscreteValue(ordinalValue=10, htmlColor=color-1))"

        when: "the implact level is right"
        cl = new CategoryLevel("color-4")
        cl.setOrdinalValue(0)

        then: "the value is returned"
        cd.getRiskValue(rd.probability.levels[0], cl) == rd.riskValues[1]
    }

    private <T> Set<ConstraintViolation<T>> getJakartaViolations(T object) {
        Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory()
                .validator.validate(object)
    }

    def "test RiskDefinition validation categories"() {
        when: "we create a simple RiskDefinition"
        RiskDefinition rd = new RiskDefinition()
        rd.riskMethod = new RiskMethod()
        rd.probability.levels = [
            new ProbabilityLevel("a")
        ]

        rd.riskValues = [
            new RiskValue(3,"#234643","symbolic_risk_2"),
        ]

        def riskMatrix = [
            [
                rd.riskValues[0]
            ]
        ]

        def potentialImpacts = [
            new CategoryLevel("l2")
        ]

        rd.implementationStateDefinition.levels = [
            new CategoryLevel("color-1")
        ]

        then: "it validates nicely"
        rd.validateRiskDefinition()

        when: "using redundant category definitions"
        rd.categories = [
            new CategoryDefinition("2", riskMatrix, potentialImpacts),
            new CategoryDefinition("2", riskMatrix, potentialImpacts),
            new CategoryDefinition("3", riskMatrix, potentialImpacts),
        ]

        then: "illegal Argument exception is thrown"
        IllegalArgumentException ex = thrown()
        ex.message == "Categories not unique."

        when: "using valid categories"
        rd.categories = [
            new CategoryDefinition("1", riskMatrix, potentialImpacts),
            new CategoryDefinition("2", riskMatrix, potentialImpacts),
            new CategoryDefinition("3", riskMatrix, potentialImpacts),
            new CategoryDefinition("4", riskMatrix, potentialImpacts),
            new CategoryDefinition("5", riskMatrix, potentialImpacts),
            new CategoryDefinition("6", null, potentialImpacts)
        ]

        then:
        noExceptionThrown()
    }

    def "risk method must not be null"() {
        given:
        def rd = createRiskDefinition()

        when: "we remove the risk method"
        rd.riskMethod = null

        then: "there is a violation"
        with(getJakartaViolations(rd)) {violations ->
            violations*.propertyPath*.toString() == ["riskMethod"]
            violations*.message == ["must not be null"]
        }

        when: "we fix the risk definition"
        rd.riskMethod = new RiskMethod()

        then: "it validates nicely"
        getJakartaViolations(rd).empty
    }

    def "test RiskDefinition validation of symbolic risk"() {
        when: "we create a simple RiskDefinition"
        def rd = createRiskDefinition()

        then: "it validates nicely"
        rd.validateRiskDefinition()

        when: "we change make the symbolic risk not unique"
        rd.riskValues = [
            new RiskValue(3,"#234643","symbolic_risk_3"),
            new RiskValue(3,"#234643","symbolic_risk_3")
        ]

        then: "illegal Argument exception is thrown"
        IllegalArgumentException ex = thrown()
        ex.message == "SymbolicRisk not unique."
    }

    def "test RiskDefinition validation of translations"() {
        when: "we create a simple RiskDefinition"
        def rd = createRiskDefinition()
        rd.id = "my-risk-def"

        then: "it validates nicely"
        TranslationValidator.validate(rd)

        when: "we add a translation with an additional field"
        rd.riskMethod.defineTranslations("de",
                [impactMethod : "my method",
                    description  : "a description",
                    unkownfeature: "some unknown"]
                )

        TranslationValidator.validate(rd)

        then: "illegal Argument exception is thrown"
        TranslationException ex = thrown()
        ex.message =~ /SUPERFLUOUS.*unkownfeature/

        when: "we add a translation with missing fields"
        rd.riskMethod.defineTranslations("de",
                [:]
                )

        TranslationValidator.validate(rd)

        then: "illegal Argument exception is thrown"
        ex = thrown()
        ex.message =~ /MISSING.*description.*impactMethod/

        when: "we add a translation with all fields"
        rd.riskMethod.defineTranslations("de",
                [
                    impactMethod: "my method",
                    description : "a description"]
                )

        then: "it validates nicely"
        TranslationValidator.validate(rd)
    }

    def "test translation provider part for RiskDefinition"(TranslationProvider tp) {
        expect:
        TranslationValidator.validate(myRisk)

        when:"missing all translations"
        tp.defineTranslations("de", [:])

        TranslationValidator.validate(myRisk)

        then:"error message is thrown"
        TranslationException ex = thrown()
        ex.message =~ /de.*MISSING.*abbreviation.*description.*name/

        when:"missing translations name abbre..."
        tp.defineTranslations("de",
                [
                    description: "a description"]
                )

        TranslationValidator.validate(myRisk)

        then:"error message is thrown"
        ex = thrown()
        ex.message =~ /de.*MISSING.*abbreviation.*name/

        when:"missing translations name"
        tp.defineTranslations("de",
                [
                    abbreviation: "abb",
                    description : "a description"]
                )

        TranslationValidator.validate(myRisk)

        then:"error message is thrown"
        ex = thrown()
        ex.message =~ /de.*MISSING.*name/

        when:"missing translations description"
        tp.defineTranslations("de",
                [
                    abbreviation: "abb",
                    name        : "a name"]
                )

        TranslationValidator.validate(myRisk)

        then:"error message is thrown"
        ex = thrown()
        ex.message =~ /de.*MISSING.*description/

        when:"additional translations addition"
        tp.defineTranslations("de",
                [
                    addition    : "unkown field",
                    name        : "my name",
                    abbreviation: "abb",
                    description : "a description"]
                )

        TranslationValidator.validate(myRisk)

        then:"error message is thrown"
        ex = thrown()
        ex.message =~ /de.*SUPERFLUOUS.*addition/

        when:"all is good"
        tp.defineTranslations("de",
                [
                    name        : "my name",
                    abbreviation: "abb",
                    description : "a description"]
                )

        then:"it validates nicely"
        TranslationValidator.validate(myRisk)

        where:
        tp <<  myRisk.categories + myRisk.probability + myRisk.implementationStateDefinition + myRisk.riskValues
    }

    private RiskDefinition createRiskDefinition() {
        RiskDefinition rd = new RiskDefinition()
        rd.id= "simple-id"
        rd.riskMethod = new RiskMethod()
        rd.probability.levels = [
            new ProbabilityLevel("a")
        ]

        rd.riskValues = [
            new RiskValue(3,"#234643","symbolic_risk_2"),
            new RiskValue(3,"#234643","symbolic_risk_3")
        ]

        def riskMatrix = [
            [rd.riskValues[0]]
        ]

        def potentialImpacts = [
            new CategoryLevel("l2")
        ]

        rd.categories = [
            new CategoryDefinition("1", riskMatrix, potentialImpacts),
            new CategoryDefinition("2", riskMatrix, potentialImpacts),
            new CategoryDefinition("3", riskMatrix, potentialImpacts),
            new CategoryDefinition("4", riskMatrix, potentialImpacts),
            new CategoryDefinition("5", riskMatrix, potentialImpacts)
        ]

        rd.implementationStateDefinition.levels = [
            new CategoryLevel("color-1")
        ]

        rd
    }
}
