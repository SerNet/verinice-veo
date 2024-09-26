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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;

import org.hibernate.annotations.GenericGenerator;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Person;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.RiskTailoringReferenceValues;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.ReferenceTargetNotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.risk.CategorizedImpactValueProvider;
import org.veo.core.entity.risk.CategorizedRiskValueProvider;
import org.veo.core.entity.risk.CategoryRef;
import org.veo.core.entity.risk.DeterminedRisk;
import org.veo.core.entity.risk.DomainRiskReferenceProvider;
import org.veo.core.entity.risk.DomainRiskReferenceValidator;
import org.veo.core.entity.risk.Impact;
import org.veo.core.entity.risk.PotentialProbability;
import org.veo.core.entity.risk.ProbabilityImpl;
import org.veo.core.entity.risk.ProbabilityValueProvider;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.risk.RiskValues;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "abstractriskdata")
@ToString(onlyExplicitlyIncluded = true)
@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@EntityListeners({VersionedEntityListener.class})
public abstract class AbstractRiskData<T extends RiskAffected<T, R>, R extends AbstractRisk<T, R>>
    extends VersionedData implements AbstractRisk<T, R> {

  @Id
  @ToString.Include
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private UUID dbId;

  @Column(name = "designator")
  @ToString.Include
  private String designator;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ControlData.class)
  @JoinColumn(name = "control_id")
  @Setter(AccessLevel.PRIVATE)
  @ToString.Exclude
  private Control mitigation;

  @NotNull
  @NonNull
  @ManyToOne(fetch = FetchType.LAZY, targetEntity = ScenarioData.class, optional = false)
  @Setter(AccessLevel.PROTECTED)
  @ToString.Exclude
  private Scenario scenario;

  @ManyToOne(fetch = FetchType.LAZY, targetEntity = PersonData.class)
  @JoinColumn(name = "person_id")
  @Setter(AccessLevel.PRIVATE)
  @ToString.Exclude
  private Person riskOwner;

  @NotNull
  @NonNull
  @ManyToOne(fetch = FetchType.LAZY, targetEntity = RiskAffectedData.class, optional = false)
  @Setter(AccessLevel.PRIVATE)
  @ToString.Exclude
  private T entity;

  @Override
  public R mitigate(@Nullable Control control) {
    if (!Objects.equals(control, mitigation)) {
      var previousMitigation = mitigation;
      setMitigation(control);
      if (previousMitigation != null
          && entity.getRisks().stream()
              .noneMatch(r -> previousMitigation.equals(r.getMitigation()))) {
        entity.disassociateControl(previousMitigation);
      }
      if (control != null) {
        entity.implementControl(control);
      }
      entity.setUpdatedAt(Instant.now());
    }
    return (R) this;
  }

  @Override
  public R appoint(@Nullable Person riskOwner) {
    setRiskOwner(riskOwner);
    return (R) this;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;

    if (this == o) return true;

    if (!(o instanceof AbstractRiskData)) return false;

    AbstractRiskData other = (AbstractRiskData) o;
    // Transient (unmanaged) entities have an ID of 'null'. Only managed
    // (persisted and detached) entities have an identity. JPA requires that
    // an entity's identity remains the same over all state changes.
    // Therefore a transient entity must never equal another entity.
    UUID dbId = getDbId();
    return dbId != null && dbId.equals(other.getDbId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  private RiskValuesAspectData getOrCreateRiskAspect(
      Domain domain, RiskDefinitionRef riskDefinitionRef) {
    return riskAspects.stream()
        .filter(ra -> ra.getDomain().equals(domain))
        .filter(ra -> ra.getRiskDefinition().equals(riskDefinitionRef))
        .findFirst()
        .orElseGet(
            () -> {
              var riskAspect = new RiskValuesAspectData(domain, this, riskDefinitionRef);
              getScenario()
                  .getPotentialProbability(domain, riskDefinitionRef)
                  .map(PotentialProbability::potentialProbability)
                  .ifPresent(riskAspect::setPotentialProbability);
              getEntity()
                  .getImpactValues(domain, riskDefinitionRef)
                  .ifPresent(
                      impactValues ->
                          impactValues.potentialImpacts().forEach(riskAspect::setPotentialImpact));
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
      targetEntity = RiskValuesAspectData.class,
      mappedBy = "owner",
      fetch = FetchType.LAZY)
  @Valid
  private Set<RiskValuesAspectData> riskAspects = new HashSet<>();

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

  private Optional<RiskValuesAspectData> findRiskAspectForDefinition(
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
        .map(RiskValuesAspectData::getRiskDefinition)
        .collect(toSet());
  }

  @Override
  public void defineRiskValues(Set<RiskValues> newValuesSet) {
    newValuesSet.forEach(
        newValues -> {
          var domain =
              getEntity().getDomains().stream()
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
                  .orElseThrow(
                      () ->
                          new ReferenceTargetNotFoundException(
                              format(
                                  "Risk definition '%s' not found in domain %s",
                                  newValues.getRiskDefinitionId().value(),
                                  domain.getIdAsString())));

          var riskDefinitionRef = RiskDefinitionRef.from(riskDefinition);

          var ra = getOrCreateRiskAspect(domain, riskDefinitionRef);
          var validator =
              new DomainRiskReferenceValidator(
                  DomainRiskReferenceProvider.referencesForDomain(domain), riskDefinitionRef);

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
  public void removeFromDomains(Domain aDomain) {
    removeRiskAspects(aDomain);
  }

  private void removeRiskAspects(Domain aDomain) {
    riskAspects.removeIf(ra -> ra.getDomain().equals(aDomain));
  }

  @Override
  public void transferToDomain(Domain oldDomain, Domain newDomain) {
    // TODO VEO-1351 Validate compatibility of risk-definitions before migration
    this.riskAspects.stream()
        .filter(ra -> ra.getDomain().equals(oldDomain))
        .forEach(ra -> ra.setDomain(newDomain));
  }

  @Override
  public boolean removeRiskDefinition(RiskDefinitionRef riskDefinition, Domain domain) {
    return findRiskAspectForDefinition(riskDefinition, domain)
        .map(riskAspects::remove)
        .orElse(false);
  }

  @Override
  public boolean removeRiskCategory(
      RiskDefinitionRef riskDefinition, CategoryRef category, Domain domain) {
    return findRiskAspectForDefinition(riskDefinition, domain)
        .map(
            ra -> {
              boolean result = ra.removeCategory(category);
              if (ra.getAvailableCategories().isEmpty()) {
                riskAspects.remove(ra);
              }
              return result;
            })
        .orElse(false);
  }

  @Override
  public boolean addRiskCategory(
      RiskDefinitionRef riskDefinition, CategoryRef category, Domain domain) {
    return findRiskAspectForDefinition(riskDefinition, domain)
        .map(
            ra -> {
              if (!ra.getAvailableCategories().contains(category)) {
                ra.addCategory(category);
                return true;
              }
              return false;
            })
        .orElse(false);
  }

  @Override
  public Map<RiskDefinitionRef, RiskTailoringReferenceValues> getTailoringReferenceValues(
      Domain domain) {
    return getRiskDefinitions(domain).stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                rd -> {
                  var probability = getProbabilityProvider(rd, domain);
                  return new RiskTailoringReferenceValues(
                      probability.getSpecificProbability(),
                      probability.getSpecificProbabilityExplanation(),
                      map(getRiskProvider(rd, domain), getImpactProvider(rd, domain)));
                }));
  }

  @Override
  public void setValues(
      Map<RiskDefinitionRef, RiskTailoringReferenceValues> riskDefinitions, Domain domain) {
    removeRiskAspects(domain);
    riskDefinitions.forEach(
        (rd, values) -> {
          var aspect = new RiskValuesAspectData(domain, this, rd);
          riskAspects.add(aspect);
          aspect.setValues(values);
        });
  }

  private Map<CategoryRef, RiskTailoringReferenceValues.CategoryValues> map(
      CategorizedRiskValueProvider riskProvider, CategorizedImpactValueProvider impactProvider) {
    return riskProvider.getAvailableCategories().stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                category ->
                    new RiskTailoringReferenceValues.CategoryValues(
                        impactProvider.getSpecificImpact(category),
                        impactProvider.getSpecificImpactExplanation(category),
                        riskProvider.getUserDefinedResidualRisk(category),
                        riskProvider.getResidualRiskExplanation(category),
                        riskProvider.getRiskTreatments(category),
                        riskProvider.getRiskTreatmentExplanation(category))));
  }
}
