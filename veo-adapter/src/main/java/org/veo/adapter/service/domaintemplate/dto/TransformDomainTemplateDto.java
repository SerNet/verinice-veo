/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.adapter.service.domaintemplate.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.dto.AbstractCatalogDto;
import org.veo.adapter.presenter.api.dto.AbstractDomainTemplateDto;
import org.veo.adapter.presenter.api.dto.ElementTypeDefinitionDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

// we accept also dto which conforms to the domain schema, so we need to ignore the additional
// fields it contains
@JsonIgnoreProperties("domainTemplate")
@EqualsAndHashCode(callSuper = true)
@Data
public class TransformDomainTemplateDto extends AbstractDomainTemplateDto
    implements IdentifiableDto {

  @Pattern(regexp = Patterns.UUID, message = "ID must be a valid UUID string following RFC 4122.")
  @Schema(
      description =
          "ID must be a valid UUID string following RFC 4122. "
              + " Offical templates should have a name base uuid defined in sec. 4.3",
      example = "f8ed22b1-b277-56ec-a2ce-0dbd94e24824",
      format = "uuid")
  @ToString.Include
  private String id;

  @JsonDeserialize(contentAs = TransformCatalogDto.class)
  @Override
  public void setCatalogs(Set<AbstractCatalogDto> catalogs) {
    super.setCatalogs(catalogs);
  }

  private Map<String, ElementTypeDefinitionDto> elementTypeDefinitions = new HashMap<>();
}
