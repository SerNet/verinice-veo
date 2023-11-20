/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.adapter.presenter.api.common.Ref;
import org.veo.core.entity.TranslatedText;
import org.veo.core.entity.inspection.Severity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

@Data
@Schema(accessMode = Schema.AccessMode.READ_ONLY)
public class ShortInspectionDto {
  private String id;
  private Severity severity;
  private TranslatedText description;
  private String elementType;

  @JsonIgnore
  @Getter(AccessLevel.NONE)
  private Ref selfRef;

  @JsonProperty(value = "_self")
  public String getSelf() {
    return selfRef.getTargetUri();
  }
}
