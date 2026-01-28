/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
package org.veo.adapter.presenter.api.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.veo.adapter.presenter.api.common.DomainBaseIdRef;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.UpdatableDomain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "A summary of available updates for a domain - a client must be updated to the next major version before it can be updated to the major version after next. Therefore, not all available updates can be performed directly.",
    accessMode = Schema.AccessMode.READ_ONLY)
public record UpdatableDomainDto(
    @NotNull DomainBaseIdRef<Domain> domain,
    @Schema(description = "All available newer versions of the domain") @NotNull
        List<DomainBaseIdRef<DomainTemplate>> allUpdates,
    @Schema(description = "All newer versions that the client can be updated to directly") @NotNull
        List<DomainBaseIdRef<DomainTemplate>> possibleUpdates,
    @Schema(description = "The latest available version of the domain") @NotNull
        DomainBaseIdRef<DomainTemplate> latestUpdate,
    @Schema(description = "The latest version that the client can be updated to directly")
        DomainBaseIdRef<DomainTemplate> latestPossibleUpdate) {

  public static UpdatableDomainDto from(
      UpdatableDomain updatableDomain, ReferenceAssembler referenceAssembler) {
    return new UpdatableDomainDto(
        DomainBaseIdRef.from(updatableDomain.domain(), referenceAssembler),
        updatableDomain.allUpdates().stream()
            .map(dt -> DomainBaseIdRef.from(dt, referenceAssembler))
            .toList(),
        updatableDomain.possibleUpdates().stream()
            .map(dt -> DomainBaseIdRef.from(dt, referenceAssembler))
            .toList(),
        DomainBaseIdRef.from(updatableDomain.latestUpdate(), referenceAssembler),
        DomainBaseIdRef.from(updatableDomain.latestPossibleUpdate(), referenceAssembler));
  }
}
