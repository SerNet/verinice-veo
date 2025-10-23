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

import static org.veo.adapter.presenter.api.dto.MapFunctions.renameKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.adapter.presenter.api.dto.CompositeEntityDto;
import org.veo.adapter.presenter.api.dto.CustomAspectDto;
import org.veo.adapter.presenter.api.dto.CustomLinkDto;
import org.veo.adapter.presenter.api.dto.DomainAssociationDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.core.entity.Person;
import org.veo.core.entity.state.CompositeElementState;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** Base transfer object for persons. Contains common data for all person DTOs. */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Schema(title = "person", description = "Schema for person")
public class FullPersonDto extends CompositeEntityDto<Person>
    implements IdentifiableDto, CompositeElementState<Person> {

  @Override
  @Schema(description = "The name for the person.", example = "Mia Musterfrau")
  public String getName() {
    return super.getName();
  }

  @Override
  @Schema(description = "The abbreviation for the person.", example = "Mrs. M.M.")
  public String getAbbreviation() {
    return super.getAbbreviation();
  }

  @Override
  @Schema(
      description = "The description for the person.",
      example =
          "Mia Musterfrau is a fictional character and is not related to any real person with that name.")
  public String getDescription() {
    return super.getDescription();
  }

  @Override
  @Schema(description = "The links for the person.")
  public Map<String, List<CustomLinkDto>> getLinks() {
    return super.getLinks();
  }

  @Schema(description = "The customAspects for the person.")
  @Override
  public Map<String, CustomAspectDto> getCustomAspects() {
    return super.getCustomAspects();
  }

  @Override
  public Class<Person> getModelInterface() {
    return Person.class;
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
  private Map<UUID, DomainAssociationDto> domains = new HashMap<>();
}
