/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Alexander Koderman
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
package org.veo.service;

import java.util.Set;
import java.util.stream.Collectors;

import org.veo.core.entity.Control;
import org.veo.core.entity.Designated;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.compliance.ControlImplementation;
import org.veo.core.entity.compliance.RequirementImplementation;
import org.veo.core.repository.ControlImplementationRepository;
import org.veo.core.repository.RequirementImplementationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ensures necessary changes are made to {@link ControlImplementation}s and {@link
 * RequirementImplementation}s corresponding to other changes in the domain model.
 */
@RequiredArgsConstructor
@Slf4j
public class ControlImplementationService {
  private final ControlImplementationRepository controlImplRepo;
  private final RequirementImplementationRepository reqImplRepo;

  /**
   * Add these controls as requirements if any of their parent controls have existing
   * control-implementations.
   */
  public void addToControlImplementations(Set<Control> addedControls) {
    addedControls.forEach(this::addToControlImplementations);
  }

  /**
   * Add this control as a requirement if any of its parent controls have existing
   * control-implementations.
   */
  private void addToControlImplementations(Control control) {
    Set<Control> allParents = control.getCompositesRecursively();
    var parentImplementations = controlImplRepo.findByControls(allParents);

    log.debug(
        "Adding control {} as requirement to implementations {}.",
        control.getIdAsString(),
        parentImplementations.stream()
            .map(ci -> ci.getId().toString())
            .collect(Collectors.joining(", ")));

    parentImplementations.forEach(
        ci -> {
          var reqImpls = reqImplRepo.findAllByRef(ci.getRequirementImplementations());
          // Skip if this CI already has an RI for this control
          if (reqImpls.stream().noneMatch(ri -> ri.getControl().equals(control))) {
            ci.addRequirement(control);
          }
        });
  }

  /** If there is a controlImplementation specifically for this control, remove it. */
  public void removeControlImplementations(Set<Control> removedControls) {
    var controlImplementations = controlImplRepo.findByControls(removedControls);

    log.debug(
        "Removing {} implementations for controls {} ",
        controlImplementations.size(),
        removedControls.stream().map(Designated::getDesignator).collect(Collectors.joining(", ")));

    controlImplementations.forEach(ControlImplementation::disassociateFromOwner);
  }

  /**
   * If this control is referenced as an implemented requirement by any control-implementation,
   * remove these references.
   */
  public void removeRequirementsFromControlImplementations(Set<Control> removedControls) {
    var requirementImplementations = reqImplRepo.findByControls(removedControls);

    log.debug(
        "Removing requirements for controls {} from implementations {}",
        removedControls.stream().map(Identifiable::getIdAsString).collect(Collectors.joining(", ")),
        requirementImplementations.stream()
            .map(ri -> ri.getId().toString())
            .collect(Collectors.joining(", ")));

    requirementImplementations.forEach(
        ri -> {
          var implementationsUsingRequirement = controlImplRepo.findByRequirement(ri);
          implementationsUsingRequirement.forEach(ci -> ci.remove(ri));
        });
  }
}
