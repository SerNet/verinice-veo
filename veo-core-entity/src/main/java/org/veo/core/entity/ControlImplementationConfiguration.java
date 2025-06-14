/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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

import java.util.Collections;
import java.util.Set;

import jakarta.validation.constraints.Size;

import javax.annotation.Nullable;

import org.veo.core.entity.aspects.ElementDomainAssociation;

public record ControlImplementationConfiguration(
    Set<@Size(max = ElementDomainAssociation.SUB_TYPE_MAX_LENGTH) String> complianceControlSubTypes,
    @Nullable @Size(max = ElementDomainAssociation.SUB_TYPE_MAX_LENGTH)
        String mitigationControlSubType) {
  public ControlImplementationConfiguration() {
    this(Collections.emptySet(), null);
  }
}
