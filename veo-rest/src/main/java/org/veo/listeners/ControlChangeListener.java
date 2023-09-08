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
package org.veo.listeners;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Control;
import org.veo.core.entity.Element;
import org.veo.core.entity.event.ControlPartsChangedEvent;
import org.veo.service.ControlImplementationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Trigger required actions when any controls change. */
@RequiredArgsConstructor
@Component
@Slf4j
public class ControlChangeListener {

  private final ControlImplementationService ciService;

  @EventListener
  @Transactional(propagation = REQUIRED)
  public void handle(ControlPartsChangedEvent event) {
    if (!event.getEntityType().isAssignableFrom(Control.class)) {
      return;
    }
    handleRemovedParts(event.getOldRecursiveParts(), event.getNewRecursiveParts());
    handleAddedParts(event.getOldRecursiveParts(), event.getNewRecursiveParts());
  }

  private void handleAddedParts(Set<Control> oldRecursiveParts, Set<Element> newRecursiveParts) {
    var addedParts = new HashSet<>(newRecursiveParts);
    addedParts.removeAll(oldRecursiveParts);
    ciService.addToControlImplementations(
        addedParts.stream().map(Control.class::cast).collect(Collectors.toSet()));
  }

  private void handleRemovedParts(Set<Control> oldRecursiveParts, Set<Element> newRecursiveParts) {
    var removedParts = new HashSet<>(oldRecursiveParts);
    removedParts.removeAll(newRecursiveParts);
    var removedControls =
        removedParts.stream().map(Control.class::cast).collect(Collectors.toSet());

    ciService.removeControlImplementations(removedControls);
    ciService.removeRequirementsFromControlImplementations(removedControls);
  }
}
