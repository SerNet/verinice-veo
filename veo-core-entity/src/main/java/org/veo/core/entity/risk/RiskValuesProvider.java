/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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
package org.veo.core.entity.risk;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;

public interface RiskValuesProvider
    extends ProbabilityValueProvider, CategorizedImpactValueProvider, CategorizedRiskValueProvider {
  Key<UUID> getDomainId();

  Key<String> getRiskDefinitionId();

  List<Impact> getImpactCategories();

  List<DeterminedRisk> getCategorizedRisks();

  @Override
  default ProbabilityRef getPotentialProbability() {
    return getProbability().getPotentialProbability();
  }

  @Override
  default ProbabilityRef getSpecificProbability() {
    return getProbability().getSpecificProbability();
  }

  @Override
  default ProbabilityRef getEffectiveProbability() {
    return getProbability().getEffectiveProbability();
  }

  @Override
  default String getSpecificProbabilityExplanation() {
    return getProbability().getSpecificProbabilityExplanation();
  }

  @Override
  default void setPotentialProbability(ProbabilityRef potential) {
    getProbability().setPotentialProbability(potential);
  }

  @Override
  default void setSpecificProbability(ProbabilityRef specific) {
    getProbability().setSpecificProbability(specific);
  }

  @Override
  default void setSpecificProbabilityExplanation(String explanation) {
    getProbability().setSpecificProbabilityExplanation(explanation);
  }

  default void setSpecificImpact(CategoryRef impactCategory, ImpactRef specific) {
    impactCategoryById(impactCategory).setSpecificImpact(specific);
  }

  @Override
  default ImpactRef getPotentialImpact(CategoryRef impactCategory) {
    return impactCategoryById(impactCategory).getPotentialImpact();
  }

  @Override
  default ImpactRef getSpecificImpact(CategoryRef impactCategory) {
    return impactCategoryById(impactCategory).getSpecificImpact();
  }

  @Override
  default ImpactRef getEffectiveImpact(CategoryRef impactCategory) {
    return impactCategoryById(impactCategory).getEffectiveImpact();
  }

  @Override
  default String getSpecificImpactExplanation(CategoryRef impactCategory) {
    return impactCategoryById(impactCategory).getSpecificImpactExplanation();
  }

  @Override
  default RiskRef getInherentRisk(CategoryRef impactCategory) {
    return riskCategoryById(impactCategory).getInherentRisk();
  }

  @Override
  default RiskRef getUserDefinedResidualRisk(CategoryRef impactCategory) {
    return riskCategoryById(impactCategory).getUserDefinedResidualRisk();
  }

  @Override
  default void setResidualRiskExplanation(
      CategoryRef impactCategory, String residualRiskExplanation) {
    riskCategoryById(impactCategory).setResidualRiskExplanation(residualRiskExplanation);
  }

  @Override
  default String getResidualRiskExplanation(CategoryRef impactCategory) {
    return riskCategoryById(impactCategory).getResidualRiskExplanation();
  }

  @Override
  default Set<RiskTreatmentOption> getRiskTreatments(CategoryRef impactCategory) {
    return riskCategoryById(impactCategory).getRiskTreatments();
  }

  @Override
  default void setPotentialImpact(CategoryRef impactCategory, ImpactRef potential) {
    impactCategoryById(impactCategory).setPotentialImpact(potential);
  }

  @Override
  default String getRiskTreatmentExplanation(CategoryRef impactCategory) {
    return riskCategoryById(impactCategory).getRiskTreatmentExplanation();
  }

  @Override
  default void setSpecificImpactExplanation(CategoryRef impactCategory, String explanation) {
    impactCategoryById(impactCategory).setSpecificImpactExplanation(explanation);
  }

  default void setUserDefinedResidualRisk(
      CategoryRef impactCategory, RiskRef userDefinedResidualRisk) {
    riskCategoryById(impactCategory).setUserDefinedResidualRisk(userDefinedResidualRisk);
  }

  @Override
  default void setRiskTreatments(
      CategoryRef impactCategory, Set<RiskTreatmentOption> riskTreatments) {
    riskCategoryById(impactCategory).setRiskTreatments(riskTreatments);
  }

  default Impact impactCategoryById(CategoryRef categoryId) {
    return getImpactCategories().stream()
        .filter(c -> c.getCategory().equals(categoryId))
        .findFirst()
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Impact category %s does not exist for for risk aspect.",
                    categoryId.getIdRef()));
  }

  default DeterminedRisk riskCategoryById(CategoryRef categoryId) {
    return getCategorizedRisks().stream()
        .filter(c -> c.getCategory().equals(categoryId))
        .findFirst()
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Risk category %s does not exist for risk aspect.", categoryId.getIdRef()));
  }

  @Override
  default void setRiskTreatmentExplanation(
      CategoryRef impactCategory, String riskTreatmentExplanation) {
    riskCategoryById(impactCategory).setRiskTreatmentExplanation(riskTreatmentExplanation);
  }

  @Override
  default List<CategoryRef> getAvailableCategories() {
    return getImpactCategories().stream().map(Impact::getCategory).collect(toUnmodifiableList());
  }

  default List<Impact> getCategorizedImpacts() {
    return getImpactCategories().stream().map(Impact.class::cast).collect(toList());
  }

  default boolean categoryExists(CategoryRef category) {
    return getImpactCategories().stream().anyMatch(c -> c.getCategory().equals(category));
  }
}
