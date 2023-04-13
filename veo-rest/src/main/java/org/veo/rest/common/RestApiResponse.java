/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.rest.common;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.core.entity.Element;

import io.swagger.v3.oas.annotations.media.Schema;

public class RestApiResponse {

  public static ResponseEntity<ApiResponseBody> ok(String message) {
    return ResponseEntity.ok().body(new ApiResponseBody(true, message));
  }

  @Schema(
      description =
          "A response body that corresponds to the performed API call. "
              + "Will include the 'Location'-header for POST requests but may be empty for other methods. ")
  public static ResponseEntity<ApiResponseBody> created(String urlBasePath, ApiResponseBody body) {
    String resourceId = body.getResourceId().orElse("");
    URI location = URI.create(urlBasePath + "/" + resourceId);
    BodyBuilder bodyBuilder = ResponseEntity.created(location);
    return bodyBuilder.body(body);
  }

  public static ResponseEntity<ApiResponseBody> created(String location, String message) {
    return ResponseEntity.created(URI.create(location)).body(new ApiResponseBody(true, message));
  }

  @Schema(
      description =
          "A response body for a new element that was created within a domain, containing resource ID, success message & Location header.")
  public static ResponseEntity<ApiResponseBody> created(
      Element element, String domainId, ReferenceAssembler refAssembler) {
    return ResponseEntity.created(
            URI.create(
                refAssembler.elementInDomainRefOf(
                    element,
                    element.getDomains().stream()
                        .filter(d -> d.getIdAsString().equals(domainId))
                        .findFirst()
                        .orElseThrow())))
        .body(CreateOutputMapper.map(element));
  }

  public static ResponseEntity<ApiResponseBody> noContent() {
    return ResponseEntity.noContent().build();
  }

  public static ResponseEntity<ApiResponseBody> badRequest() {
    return ResponseEntity.badRequest().build();
  }
}
