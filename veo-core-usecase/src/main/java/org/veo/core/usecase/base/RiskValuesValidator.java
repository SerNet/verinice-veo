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
package org.veo.core.usecase.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.veo.core.entity.ValidationError;
import org.veo.core.entity.exception.RiskConsistencyException;
import org.veo.core.entity.risk.DomainRiskReferenceProvider;
import org.veo.core.entity.risk.DomainRiskReferenceValidator;
import org.veo.core.entity.risk.ImpactValues;
import org.veo.core.entity.risk.PotentialProbability;
import org.veo.core.entity.risk.RiskDefinitionRef;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class RiskValuesValidator {
  static void validateScenarioRiskValues(
      Map<RiskDefinitionRef, PotentialProbability> riskValues,
      DomainRiskReferenceProvider refProvider) {
    ValidationError.throwOnErrors(getScenarioRiskValueErrors(riskValues, refProvider));
  }

  static void validateImpactValues(
      Map<RiskDefinitionRef, ImpactValues> impactValues, DomainRiskReferenceProvider refProvider) {
    ValidationError.throwOnErrors(getImpactValueErrors(impactValues, refProvider));
  }

  static List<ValidationError> getScenarioRiskValueErrors(
      Map<RiskDefinitionRef, PotentialProbability> riskValues,
      DomainRiskReferenceProvider refProvider) {
    var errors = new ArrayList<ValidationError>();
    riskValues.forEach(
        (riskDefRef, probability) -> {
          var validator = new DomainRiskReferenceValidator(refProvider, riskDefRef);
          try {
            validator.validate(probability.potentialProbability());
          } catch (RiskConsistencyException ex) {
            errors.add(new ValidationError.Generic(ex.getMessage()));
          }
        });
    return errors;
  }

  static List<ValidationError> getImpactValueErrors(
      Map<RiskDefinitionRef, ImpactValues> impactValues, DomainRiskReferenceProvider refProvider) {
    var errors = new ArrayList<ValidationError>();
    impactValues.forEach(
        (riskDefRef, impact) -> {
          var validator = new DomainRiskReferenceValidator(refProvider, riskDefRef);
          impact
              .potentialImpacts()
              .forEach(
                  (category, impactRef) -> {
                    try {
                      validator.validate(category, impactRef);
                    } catch (RiskConsistencyException ex) {
                      errors.add(new ValidationError.Generic(ex.getMessage()));
                    }
                  });
          // TODO #2663 Add a test for this. This validation becomes relevant when the automatism
          // that sets all missing reasons to MANUAL is removed.
          impact
              .potentialImpacts()
              .keySet()
              .forEach(
                  cat -> {
                    if (!impact.potentialImpactReasons().containsKey(cat)) {
                      errors.add(
                          new ValidationError.Generic(
                              "Reason missing for user-defined impact value in category '%s'"
                                  .formatted(cat.getIdRef())));
                    }
                  });
          impact
              .potentialImpactReasons()
              .keySet()
              .forEach(
                  cat -> {
                    if (!impact.potentialImpacts().containsKey(cat)) {
                      errors.add(
                          new ValidationError.Generic(
                              "Cannot set impact reason for category '%s' (user-defined impact value absent)"
                                  .formatted(cat.getIdRef())));
                    }
                  });
          impact
              .potentialImpactExplanations()
              .keySet()
              .forEach(
                  cat -> {
                    if (!impact.potentialImpacts().containsKey(cat)) {
                      errors.add(
                          new ValidationError.Generic(
                              "Cannot set impact explanation for category '%s' (user-defined impact value absent)"
                                  .formatted(cat.getIdRef())));
                    }
                  });
        });
    return errors;
  }
}
