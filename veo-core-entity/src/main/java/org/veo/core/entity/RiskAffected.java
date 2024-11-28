/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman.
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
package org.veo.core.entity;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.entity.compliance.ReqImplRef;
import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.risk.ImpactValueProvider;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.risk.RiskValues;

/**
 * An entity that is affected by risks resulting from association with scenarios. It also is the
 * carrier of the impact dimension of the risk calculation.
 */
public interface RiskAffected<T extends RiskAffected<T, R>, R extends AbstractRisk<T, R>>
    extends Element, RiskRelated, ImpactValueProvider {

  default void setRisks(Set<R> newRisks) {
    getRisks().clear();
    getRisks().addAll(newRisks);
  }

  Set<R> getRisks();

  default boolean addRisk(R risk) {
    checkRisk(risk);
    return getRisks().add(risk);
  }

  default boolean removeRisk(AbstractRisk<T, R> abstractRisk) {
    return getRisks().remove(abstractRisk);
  }

  default void removeRisks(Set<R> risks) {
    getRisks().removeAll(risks);
  }

  default void checkRisk(R risk) {
    if (!risk.getEntity().equals(this)) throw new IllegalArgumentException();
  }

  /**
   * Obtain a risk object for the given scenario. If the risk for this scenario does not yet exist,
   * it will be created, otherwise, the existing risk will be returned. This behavior makes this
   * method idempotent. It can be called with the same parameters multiple times. Subsequent calls
   * after the first one will have no effect.
   *
   * @param scenario the scenario that causes the risk
   * @return the newly created risk object or the existing one if it was previously created for the
   *     scenario
   */
  R obtainRisk(Scenario scenario);

  default Set<R> obtainRisks(Set<Scenario> scenarios, Set<RiskDefinitionRef> riskDefinitions) {
    return getOrCreateRisks(scenarios, riskDefinitions);
  }

  default Set<R> getOrCreateRisks(Set<Scenario> scenarios, Set<RiskDefinitionRef> riskDefinitions) {
    scenarios.forEach(s -> s.checkSameClient(this));

    return scenarios.stream().map(s -> obtainRisk(s)).collect(Collectors.toSet());
  }

  default Optional<R> getRisk(Scenario scenario) {
    return getRisks().stream().filter(risk -> risk.getScenario().equals(scenario)).findFirst();
  }

  default Optional<R> getRisk(UUID scenarioRef) {
    return getRisks().stream()
        .filter(risk -> risk.getScenario().getId().equals(scenarioRef))
        .findFirst();
  }

  /**
   * Updates an existing risk with new values. Increases the version number of the risk.
   *
   * @param existingRisk the existing risk value that will be updated with new values
   * @param mitigation the new control to mitigate this risk
   * @param riskOwner the new person to appoint the risk to
   * @return the updated risk entity
   */
  default R updateRisk(
      R existingRisk,
      @Nullable Control mitigation,
      @Nullable Person riskOwner,
      Set<RiskValues> riskValuesSet) {

    var riskToUpdate =
        getRisk(existingRisk.getScenario())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        String.format("The risk is not know to this object: %s", existingRisk)));

    riskToUpdate.defineRiskValues(riskValuesSet);

    return riskToUpdate.mitigate(mitigation).appoint(riskOwner);
  }

  /**
   * Add a control implementation to this element for the specified control. This will also document
   * all implemented requirements of that control.
   *
   * <p>If an implementation for this control already exists, return it instead.
   *
   * @return the newly created implementation or the existing one that was already present
   */
  ControlImplementation implementControl(Control control);

  /** Specify that a control is no longer being implemented by this element. */
  void disassociateControl(Control control);

  void removeRequirementImplementation(Control control);

  Set<ControlImplementation> getControlImplementations();

  Set<RequirementImplementation> getRequirementImplementations();

  /**
   * Returns an implementation of a requirement for the given reference.
   *
   * @throws org.veo.core.entity.exception.NotFoundException if no implementation matching the
   *     reference is found on the element
   */
  RequirementImplementation getRequirementImplementation(ReqImplRef reqImplRef);

  /**
   * Returns an existing implementation of this element for the given control.
   *
   * @throws org.veo.core.entity.exception.NotFoundException if no implementation exists for this
   *     control
   */
  ControlImplementation getImplementationFor(Control control);

  default RequirementImplementation getRequirementImplementation(Control control) {
    return findRequirementImplementation(control)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "%s %s contains no requirement implementation for control %s"
                        .formatted(getModelType(), getIdAsString(), control.getIdAsString())));
  }

  default Optional<RequirementImplementation> findRequirementImplementation(Control control) {
    return getRequirementImplementations().stream()
        .filter(ri -> ri.getControl().equals(control))
        .findAny();
  }
}
