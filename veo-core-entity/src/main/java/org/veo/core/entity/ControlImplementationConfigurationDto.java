/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Jonas Jordan
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.constraints.Size;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.veo.core.entity.aspects.ElementDomainAssociation;

import io.swagger.v3.oas.annotations.media.Schema;

// TODO #3860 remove, replace with ControlImplementationConfiguration
public record ControlImplementationConfigurationDto(
    Optional<Set<@Size(max = ElementDomainAssociation.SUB_TYPE_MAX_LENGTH) String>>
        complianceControlSubTypes,
    // Swagger won't pick up max length from the optional type.
    @Schema(maxLength = ElementDomainAssociation.SUB_TYPE_MAX_LENGTH)
        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        @Deprecated(forRemoval = true)
        Optional<@Size(max = ElementDomainAssociation.SUB_TYPE_MAX_LENGTH) String>
            complianceControlSubType,
    @Nullable @Size(max = ElementDomainAssociation.SUB_TYPE_MAX_LENGTH)
        String mitigationControlSubType) {

  public ControlImplementationConfigurationDto(ControlImplementationConfiguration config) {
    this(
        Optional.of(config.complianceControlSubTypes()),
        config.complianceControlSubTypes().stream().sorted().findFirst(),
        config.mitigationControlSubType());
  }

  public ControlImplementationConfiguration toConfig(
      ControlImplementationConfiguration currentConfig) {
    var currentDto = new ControlImplementationConfigurationDto(currentConfig);
    return new ControlImplementationConfiguration(
        getComplianceSubTypes(currentDto), mitigationControlSubType);
  }

  private Set<String> getComplianceSubTypes(ControlImplementationConfigurationDto currentDto) {
    if (complianceControlSubTypes.isEmpty() && complianceControlSubType.isEmpty()) {
      return Collections.emptySet();
    }
    if (complianceControlSubTypes.isPresent()
        && !complianceControlSubTypes.equals(currentDto.complianceControlSubTypes)) {
      return complianceControlSubTypes.get();
    } else if (complianceControlSubType.isPresent()
        && !Objects.equals(complianceControlSubType, currentDto.complianceControlSubType)) {
      if (complianceControlSubType.get() == null) {
        return Collections.emptySet();
      }
      return Set.of(complianceControlSubType.get());
    }
    return currentDto.complianceControlSubTypes.get();
  }
}
