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

import java.util.Map;

import org.veo.core.entity.exception.RiskConsistencyException;
import org.veo.core.entity.risk.ControlRiskValues;
import org.veo.core.entity.risk.DomainRiskReferenceProvider;
import org.veo.core.entity.risk.DomainRiskReferenceValidator;
import org.veo.core.entity.risk.ImpactValues;
import org.veo.core.entity.risk.PotentialProbability;
import org.veo.core.entity.risk.RiskDefinitionRef;

class RiskValuesValidator {
  public static void validateScenarioRiskValues(
      Map<RiskDefinitionRef, PotentialProbability> riskValues,
      DomainRiskReferenceProvider refProvider) {
    riskValues.forEach(
        (riskDefRef, probability) -> {
          var validator = new DomainRiskReferenceValidator(refProvider, riskDefRef);
          validator.validate(probability.potentialProbability());
        });
  }

  public static void validateControlRiskValues(
      Map<RiskDefinitionRef, ControlRiskValues> riskValues,
      DomainRiskReferenceProvider riskRefProvider) {
    riskValues.forEach(
        (riskDefRef, values) -> {
          var validator = new DomainRiskReferenceValidator(riskRefProvider, riskDefRef);
          validator.validate(values.implementationStatus());
        });
  }

  public static void validateImpactValues(
      Map<RiskDefinitionRef, ImpactValues> impactValues, DomainRiskReferenceProvider refProvider) {
    impactValues.forEach(
        (riskDefRef, impact) -> {
          var validator = new DomainRiskReferenceValidator(refProvider, riskDefRef);
          impact.potentialImpacts().forEach(validator::validate);
          // TODO #2663 Add a test for this. This validation becomes relevant when the automatism
          // that sets all missing reasons to MANUAL is removed.
          impact
              .potentialImpacts()
              .keySet()
              .forEach(
                  cat -> {
                    if (!impact.potentialImpactReasons().containsKey(cat)) {
                      throw new RiskConsistencyException(
                          "Reason missing for user-defined impact value in category '%s'"
                              .formatted(cat.getIdRef()));
                    }
                  });
          impact
              .potentialImpactReasons()
              .keySet()
              .forEach(
                  cat -> {
                    if (!impact.potentialImpacts().containsKey(cat)) {
                      throw new RiskConsistencyException(
                          "Cannot set impact reason for category '%s' (user-defined impact value absent)"
                              .formatted(cat.getIdRef()));
                    }
                  });
          impact
              .potentialImpactExplanations()
              .keySet()
              .forEach(
                  cat -> {
                    if (!impact.potentialImpacts().containsKey(cat)) {
                      throw new RiskConsistencyException(
                          "Cannot set impact explanation for category '%s' (user-defined impact value absent)"
                              .formatted(cat.getIdRef()));
                    }
                  });
        });
  }
}
