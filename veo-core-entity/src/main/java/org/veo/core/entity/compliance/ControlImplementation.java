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
package org.veo.core.entity.compliance;

import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.Control;
import org.veo.core.entity.Person;
import org.veo.core.entity.RiskAffected;

public interface ControlImplementation {

  /** The asset/process/scope for which the control is applicable. */
  RiskAffected<?, ?> getOwner();

  /** The applicable control. */
  Control getControl();

  /** A person responsible for this implementation of the control/control-composite. */
  Person getResponsible();

  void setResponsible(Person responsible);

  /** Reasoning and circumstances of why and how this control is applicable to this system. */
  String getDescription();

  void setDescription(String description);

  /**
   * How does this system fulfil the control's requirements - by itself or by relying on underlying
   * systems.
   */
  Set<ReqImplRef> getRequirementImplementations();

  UUID getId();

  void remove(RequirementImplementation ri);

  void addRequirement(Control control);
}
