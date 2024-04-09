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

import java.util.Set;

import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.dto.AbstractProcessDto;
import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.core.VeoConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@Data
public class FullProcessDto extends AbstractProcessDto implements IdentifiableDto {

  @Pattern(regexp = Patterns.UUID, message = VeoConstants.UUID_MESSAGE)
  @Schema(
      description = VeoConstants.UUID_MESSAGE,
      example = "adf037f1-0089-48ad-9177-92269918758b",
      format = "uuid")
  @ToString.Include
  private String id;

  @JsonProperty(access = READ_ONLY)
  @Schema(description = "An optional map of all risks and risk-values for this process.")
  private Set<ProcessRiskDto> risks;

  @Override
  public String getSelfId() {
    return id;
  }
}
