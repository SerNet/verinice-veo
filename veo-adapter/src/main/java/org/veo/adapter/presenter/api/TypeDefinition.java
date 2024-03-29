/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
package org.veo.adapter.presenter.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@Schema(accessMode = Schema.AccessMode.READ_ONLY)
@AllArgsConstructor
public class TypeDefinition {
  @Schema(
      description = "GET all entities of this type at this location.",
      example = "http://<api.example.org>/api/v1/assets",
      format = "uri")
  private String collectionUri;

  @Schema(
      description = "POST an entity search at this location.",
      example = "http://<api.example.org>/api/v1/assets/searches",
      format = "uri")
  private String searchUri;

  @Schema(
      description = "GET the JSON schema for this type at this location.",
      example = "http://<api.example.org>/api/v1/schemas/asset",
      format = "uri")
  private String schemaUri;
}
