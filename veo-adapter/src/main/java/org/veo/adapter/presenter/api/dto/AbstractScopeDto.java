/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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

import static org.veo.adapter.presenter.api.dto.MapFunctions.renameKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.validation.Valid;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Scope;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Schema(title = "scope", description = "Schema for scope")
public abstract class AbstractScopeDto extends AbstractElementDto {

  @Schema(description = "The scope's members")
  private Set<IdRef<Element>> members = Collections.emptySet();

  @Override
  public Class<? extends Identifiable> getModelInterface() {
    return Scope.class;
  }

  @Override
  public void clearDomains() {
    domains.clear();
  }

  @Override
  public void transferToDomain(String sourceDomainId, String targetDomainId) {
    renameKey(domains, sourceDomainId, targetDomainId);
  }

  @Valid
  @Schema(
      description =
          "Details about this element's association with domains. Domain ID is key, association object is value.")
  private Map<String, ScopeDomainAssociationDto> domains = new HashMap<>();
}
