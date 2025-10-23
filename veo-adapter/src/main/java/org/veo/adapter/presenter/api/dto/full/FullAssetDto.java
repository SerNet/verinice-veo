/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.adapter.presenter.api.dto.full;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY;
import static org.veo.adapter.presenter.api.dto.MapFunctions.renameKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.adapter.presenter.api.dto.AssetDomainAssociationDto;
import org.veo.adapter.presenter.api.dto.CompositeEntityDto;
import org.veo.adapter.presenter.api.dto.ControlImplementationDto;
import org.veo.adapter.presenter.api.dto.CustomAspectDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.RequirementImplementationDto;
import org.veo.adapter.presenter.api.dto.RiskAffectedDtoWithRIs;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
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
public class FullAssetDto extends CompositeEntityDto<Asset>
    implements IdentifiableDto, AssetState, RiskAffectedDtoWithRIs<Asset> {

  @JsonProperty(access = READ_ONLY)
  @Schema(description = "An optional map of all risks and risk-values for this asset.")
  private Set<AssetRiskDto> risks;

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
  public void transferToDomain(UUID sourceDomainId, UUID targetDomainId) {
    renameKey(domains, sourceDomainId, targetDomainId);
  }

  @Valid
  @Schema(
      description =
          "Details about this element's association with domains. Domain ID is key, association object is value.")
  private Map<UUID, AssetDomainAssociationDto> domains = new HashMap<>();

  @Valid private Set<ControlImplementationDto> controlImplementations = new HashSet<>();
  @Valid private Set<RequirementImplementationDto> requirementImplementations = new HashSet<>();
}
