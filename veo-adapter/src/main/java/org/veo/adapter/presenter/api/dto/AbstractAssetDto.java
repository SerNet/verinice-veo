/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.Valid;

import org.veo.core.entity.Asset;
import org.veo.core.entity.state.AssetState;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** Base transfer object for assets. Contains common data for all asset DTOs. */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Schema(title = "asset", description = "Schema for asset")
public abstract class AbstractAssetDto extends CompositeEntityDto<Asset>
    implements AssetState, RiskAffectedDto<Asset> {

  @Override
  @Schema(description = "The name for the asset.", example = "Mail Server")
  public String getName() {
    return super.getName();
  }

  @Override
  @Schema(description = "The abbreviation for the asset.", example = "MS")
  public String getAbbreviation() {
    return super.getAbbreviation();
  }

  @Override
  @Schema(description = "The description for the asset.", example = "A server handling e-mail.")
  public String getDescription() {
    return super.getDescription();
  }

  @Override
  @Schema(description = "The links for the asset.")
  public Map<String, List<CustomLinkDto>> getLinks() {
    return super.getLinks();
  }

  @Schema(description = "The customAspects for the asset.")
  @Override
  public Map<String, CustomAspectDto> getCustomAspects() {
    return super.getCustomAspects();
  }

  @Override
  public Class<Asset> getModelInterface() {
    return Asset.class;
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
  private Map<String, AssetDomainAssociationDto> domains = new HashMap<>();

  @Valid private Set<ControlImplementationDto> controlImplementations = new HashSet<>();
}
