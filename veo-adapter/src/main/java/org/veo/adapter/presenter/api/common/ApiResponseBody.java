/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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
package org.veo.adapter.presenter.api.common;

import java.util.Optional;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Returns the response body for methods that only provide status messages instead of entity data.
 */
@Data
@Schema(accessMode = Schema.AccessMode.READ_ONLY)
public class ApiResponseBody {

  private final boolean success;
  private Optional<String> resourceId;
  private final String message;

  public ApiResponseBody(
      boolean success, Optional<String> resourceId, String messageTemplate, Object... arguments) {
    this.success = success;
    this.resourceId = resourceId;
    this.message = createMessage(messageTemplate, arguments);
  }

  private String createMessage(String messageTemplate, Object[] arguments) {
    if (arguments == null || arguments.length == 0) return messageTemplate;
    else return String.format(messageTemplate, arguments);
  }

  public ApiResponseBody(boolean success, String messageTemplate, Object... arguments) {
    this.success = success;
    this.message = createMessage(messageTemplate, arguments);
  }

  public ApiResponseBody(boolean success, String message) {
    this.success = success;
    this.message = message;
  }
}
