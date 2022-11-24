/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.persistence.entity.jpa;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.ReferenceTargetNotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.risk.CategorizedImpactValueProvider;
import org.veo.core.entity.risk.CategorizedRiskValueProvider;
import org.veo.core.entity.risk.DeterminedRisk;
import org.veo.core.entity.risk.DomainRiskReferenceProvider;
import org.veo.core.entity.risk.DomainRiskReferenceValidator;
import org.veo.core.entity.risk.Impact;
import org.veo.core.entity.risk.PotentialProbabilityImpl;
import org.veo.core.entity.risk.ProbabilityImpl;
import org.veo.core.entity.risk.ProbabilityValueProvider;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.risk.RiskValues;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Entity(name = "processrisk")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ProcessRiskData extends AbstractRiskData<Process, ProcessRisk> implements ProcessRisk {

  // see https://github.com/rzwitserloot/lombok/issues/1134
  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
  ProcessRiskData(@NotNull @NonNull Process process, @NotNull Scenario scenario) {
    super(scenario, process);
  }

  private ProcessRiskValuesAspectData getOrCreateRiskAspect(
      Domain domain, RiskDefinitionRef riskDefinitionRef) {
    return riskAspects.stream()
        .filter(ra -> ra.getDomain().equals(domain))
        .filter(ra -> ra.getRiskDefinition().equals(riskDefinitionRef))
        .findFirst()
        .orElseGet(
            () -> {
              var riskAspect = new ProcessRiskValuesAspectData(domain, this, riskDefinitionRef);
              getScenario()
                  .getPotentialProbability(domain)
                  .map(probablities -> probablities.get(riskDefinitionRef))
                  .map(PotentialProbabilityImpl::getPotentialProbability)
                  .ifPresent(riskAspect::setPotentialProbability);
              getEntity()
                  .getImpactValues(domain, riskDefinitionRef)
                  .ifPresent(
                      impactValues ->
                          impactValues
                              .getPotentialImpacts()
                              .forEach(riskAspect::setPotentialImpact));
              riskAspects.add(riskAspect);
              return riskAspect;
            });
  }

  /**
   * There may be different risk aspects for different risk-definitions. NOTE: this risk object is
   * scoped to a list of domains, so the risk aspects in this {@code riskAspects} set must all
   * belong to one of these domains.
   */
  @Column(name = "risk_aspects")
  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      targetEntity = ProcessRiskValuesAspectData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  private Set<ProcessRiskValuesAspectData> riskAspects = new HashSet<>();

  @Override
  public ProbabilityValueProvider getProbabilityProvider(
      RiskDefinitionRef riskDefinition, Domain domain) {
    return findRiskAspectForDefinition(riskDefinition, domain)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "No probability values found for process"
                        + " %s and scenario %s with "
                        + "risk-definition"
                        + " %s",
                    getEntity().getDisplayName(),
                    getScenario().getDisplayName(),
                    riskDefinition.getIdRef()));
  }

  @Override
  public CategorizedImpactValueProvider getImpactProvider(
      RiskDefinitionRef riskDefinition, Domain domain) {
    return findRiskAspectForDefinition(riskDefinition, domain)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "No risk impacts found for process %s "
                        + "and scenario %s with "
                        + "risk-definition %s",
                    getEntity().getDisplayName(),
                    getScenario().getDisplayName(),
                    riskDefinition.getIdRef()));
  }

  private Optional<ProcessRiskValuesAspectData> findRiskAspectForDefinition(
      RiskDefinitionRef riskDefinition, Domain domain) {
    return riskAspects.stream()
        .filter(a -> a.getRiskDefinition().equals(riskDefinition) && a.getDomain().equals(domain))
        .findFirst();
  }

  @Override
  public CategorizedRiskValueProvider getRiskProvider(
      RiskDefinitionRef riskDefinition, Domain domain) {
    return findRiskAspectForDefinition(riskDefinition, domain)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "No risk values found for process %s "
                        + "and scenario %s with "
                        + "risk-definition %s",
                    getEntity().getDisplayName(),
                    getScenario().getDisplayName(),
                    riskDefinition.getIdRef()));
  }

  @Override
  public Set<RiskDefinitionRef> getRiskDefinitions(Domain domain) {
    return riskAspects.stream()
        .filter(ra -> ra.getDomain().equals(domain))
        .map(ProcessRiskValuesAspectData::getRiskDefinition)
        .collect(toSet());
  }

  @Override
  public void defineRiskValues(Set<RiskValues> newValuesSet) {
    newValuesSet.forEach(
        newValues -> {
          var domain =
              getDomains().stream()
                  .filter(d -> d.getId().equals(newValues.getDomainId()))
                  .findFirst()
                  .orElseThrow(
                      () ->
                          new UnprocessableDataException(
                              "Cannot assign risk values for unknown domain '"
                                  + newValues.getDomainId()
                                  + "'"));
          var riskDefinition =
              domain
                  .getRiskDefinition(newValues.getRiskDefinitionId().value())
                  .map(RiskDefinitionRef::from)
                  .orElseThrow(
                      () ->
                          new ReferenceTargetNotFoundException(
                              format(
                                  "Risk definition '%s' not found in domain %s",
                                  newValues.getRiskDefinitionId().value(),
                                  domain.getIdAsString())));

          var ra = getOrCreateRiskAspect(domain, riskDefinition);
          var validator =
              new DomainRiskReferenceValidator(
                  DomainRiskReferenceProvider.referencesForDomain(domain), riskDefinition);

          var probability = ra.getProbability();
          updateProbability(probability, newValues, validator);

          var impacts = ra.getImpactCategories();
          updateImpacts(impacts, newValues, validator);

          var risks = ra.getCategorizedRisks();
          updateRisks(risks, newValues, validator);
        });

    // increase optimistic locking version:
    if (!newValuesSet.isEmpty()) {
      this.setUpdatedAt(Instant.now());
    }
  }

  private void updateRisks(
      List<DeterminedRisk> risks,
      RiskValues newRiskValues,
      DomainRiskReferenceValidator validator) {
    risks.forEach(
        risk -> {
          var category = risk.getCategory();
          // handle partial inputs correctly - if not all categories are present in the
          // new value set, leave the existing categories as they are:
          if (newRiskValues.getCategorizedRisks() != null
              && newRiskValues.categoryExists(category)) {
            risk.setUserDefinedResidualRisk(
                validator.validate(category, newRiskValues.getUserDefinedResidualRisk(category)));
            risk.setResidualRiskExplanation(newRiskValues.getResidualRiskExplanation(category));
            risk.setRiskTreatments(newRiskValues.getRiskTreatments(category));
            risk.setRiskTreatmentExplanation(newRiskValues.getRiskTreatmentExplanation(category));
          }
        });
  }

  private void updateProbability(
      ProbabilityImpl probability, RiskValues newAspect, DomainRiskReferenceValidator validator) {
    // only change existing probability values if they are present in the input:
    if (newAspect.getProbability() == null) return;
    probability.setSpecificProbability(validator.validate(newAspect.getSpecificProbability()));
    probability.setSpecificProbabilityExplanation(newAspect.getSpecificProbabilityExplanation());
  }

  private void updateImpacts(
      List<Impact> impacts, RiskValues newImpactValues, DomainRiskReferenceValidator validator) {
    impacts.forEach(
        impact -> {
          var category = impact.getCategory();
          // handle partial inputs correctly - if not all categories are present in the
          // new value set, leave the existing categories as they are:
          if (newImpactValues.getImpactCategories() != null
              && newImpactValues.categoryExists(category)) {
            impact.setSpecificImpact(
                validator.validate(category, newImpactValues.getSpecificImpact(category)));
            impact.setSpecificImpactExplanation(
                newImpactValues.getSpecificImpactExplanation(category));
          }
        });
  }

  @Override
  public boolean removeFromDomains(Domain aDomain) {
    var success = super.removeFromDomains(aDomain);
    removeRiskAspects(aDomain);
    return success;
  }

  private void removeRiskAspects(Domain aDomain) {
    riskAspects.removeIf(ra -> ra.getDomain().equals(aDomain));
  }

  @Override
  public void transferToDomain(Domain oldDomain, Domain newDomain) {
    super.transferToDomain(oldDomain, newDomain);
    // TODO VEO-1351 Validate compatibility of risk-definitions before migration
    this.riskAspects.stream()
        .filter(ra -> ra.getDomain().equals(oldDomain))
        .forEach(ra -> ra.setDomain(newDomain));
  }
}
