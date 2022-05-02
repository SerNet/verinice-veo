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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Transfer object for complete processes.
 *
 * <p>Contains all information of the process.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Schema(title = "process", description = "Schema for process")
public abstract class AbstractProcessDto extends CompositeEntityDto<Process> {

  @Override
  @Schema(description = "The name for the process.", example = "Two-factor authentication")
  public String getName() {
    return super.getName();
  }

  @Override
  @Schema(description = "The abbreviation for the process.", example = "2FA")
  public String getAbbreviation() {
    return super.getAbbreviation();
  }

  @Override
  @Schema(
      description = "The description for the process.",
      example = "Implement 2FA where possible.")
  public String getDescription() {
    return super.getDescription();
  }

  @Override
  @Schema(description = "The links for the process.")
  public Map<String, List<CustomLinkDto>> getLinks() {
    return super.getLinks();
  }

  @Schema(description = "The customAspects for the process.")
  @Override
  public Map<String, CustomAspectDto> getCustomAspects() {
    return super.getCustomAspects();
  }

  @Override
  public Class<? extends Identifiable> getModelInterface() {
    return Process.class;
  }

  @Override
  public void associateWithTargetDomain(String id) {
    setDomains(
        Map.of(
            id,
            getDomains().values().stream().findFirst().orElse(new ProcessDomainAssociationDto())));
  }

  @Override
  public void clearDomains() {
    domains.clear();
  }

  @Valid
  @Schema(
      description =
          "Details about this element's association with domains. Domain ID is key, association object is value.")
  private Map<String, ProcessDomainAssociationDto> domains = new HashMap<>();
}
