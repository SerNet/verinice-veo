/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import org.veo.core.entity.SystemMessage;
import org.veo.core.entity.SystemMessage.MessageLevel;
import org.veo.core.entity.TranslatedText;
import org.veo.core.entity.state.SystemMessageState;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SystemMessageDto implements SystemMessageState {

  @JsonProperty(access = Access.READ_ONLY)
  private Long id;

  private TranslatedText message = TranslatedText.empty();

  @Schema(
      description = "A timestamp acc. to RFC 3339 specifying when this entity was created.",
      example = "1990-12-31T23:59:60Z")
  @JsonProperty(access = Access.READ_ONLY)
  private Instant createdAt;

  @Schema(description = "The time at which the message should be visible to the user.")
  @NotNull
  private Instant publication;

  @Schema(description = "The time at which the change described by the message will take effect.")
  @Nullable
  private Instant effective;

  @NotNull private MessageLevel level;

  public static SystemMessageDto of(SystemMessage message) {
    return new SystemMessageDto(
        message.getId(),
        message.getMessage(),
        message.getCreatedAt(),
        message.getPublication(),
        message.getEffective(),
        message.getLevel());
  }
}
