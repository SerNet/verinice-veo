/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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

/**
 * A sub-procedure within an {@link Action} that can be executed on an element.
 *
 * <p>Because the execution of an action step may depend upon several beans, the actual execution
 * code for action steps does not live in the action step classes themselves, but in the use case
 * layer. The sealed class structure aids in this external implementation approach.
 */
public abstract sealed class ActionStep permits AddRisksStep, ApplyLinkTailoringReferences {
  abstract void selfValidate(Domain domain, ElementType elementType);
}
