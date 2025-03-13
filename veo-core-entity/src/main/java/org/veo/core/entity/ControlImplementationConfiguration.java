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

import java.util.Set;

import jakarta.validation.constraints.Size;

import javax.annotation.Nullable;

import org.veo.core.entity.aspects.ElementDomainAssociation;
import org.veo.core.entity.exception.UnprocessableDataException;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

public record ControlImplementationConfiguration(
    @Nullable @Size(max = ElementDomainAssociation.SUB_TYPE_MAX_LENGTH)
        String complianceControlSubType,
    @Nullable @Size(max = ElementDomainAssociation.SUB_TYPE_MAX_LENGTH)
        String mitigationControlSubType,
    @ArraySchema(
            arraySchema =
                @Schema(
                    description =
                        "The element types for which CIs can be created, must be set if and only if complianceControlSubType is non-null"),
            schema =
                @Schema(
                    type = "string",
                    allowableValues = {
                      Asset.SINGULAR_TERM,
                      Process.SINGULAR_TERM,
                      Scope.SINGULAR_TERM
                    }))
        @Nullable
        Set<ElementType> complianceOwnerElementTypes) {

  public ControlImplementationConfiguration() {
    this(null, null, null);
  }

  public ControlImplementationConfiguration(
      String complianceControlSubType,
      String mitigationControlSubType,
      Set<ElementType> complianceOwnerElementTypes) {
    boolean noComplianceOwnerElementTypes =
        complianceOwnerElementTypes == null || complianceOwnerElementTypes.isEmpty();
    if (complianceControlSubType != null) {
      if (noComplianceOwnerElementTypes) {
        throw new UnprocessableDataException(
            "complianceOwnerElementTypes must not be empty if complianceControlSubType is set.");
      }
      if (!ElementType.RISK_AFFECTED_TYPES.containsAll(complianceOwnerElementTypes)) {
        throw new UnprocessableDataException("complianceOwnerElementTypes contains invalid types.");
      }
    } else {
      if (!noComplianceOwnerElementTypes) {
        throw new UnprocessableDataException(
            "complianceOwnerElementTypes must be empty if complianceControlSubType is not set.");
      }
    }
    this.complianceControlSubType = complianceControlSubType;
    this.mitigationControlSubType = mitigationControlSubType;
    this.complianceOwnerElementTypes =
        noComplianceOwnerElementTypes ? null : Set.copyOf(complianceOwnerElementTypes);
  }
}
