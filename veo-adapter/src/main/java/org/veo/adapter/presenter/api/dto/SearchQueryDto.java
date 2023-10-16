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
package org.veo.adapter.presenter.api.dto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * Representation of a (named) search query.
 *
 * <p>Currently only supports filtering by unit-ID. Can be extended to allow additional filter
 * functions.
 *
 * <p>Can be encoded/decoded as an ID string to be used as a named search without having to store
 * the search in the database.
 */
@Value
@NonFinal
@SuperBuilder
@Jacksonized
@Schema(title = "SearchQueryDto", description = "A search query")
@Valid
public class SearchQueryDto {

  @Schema(description = "The ID of the unit of which the searches elements must be a member.")
  UuidQueryConditionDto unitId;

  @Schema(description = "A substring of the description of an element.")
  QueryConditionDto<String> description;

  @Schema(description = "A substring of the designator of an element.")
  QueryConditionDto<String> designator;

  @Schema(description = "A substring of the name of an element.")
  QueryConditionDto<String> name;

  @Schema(description = "A substring of the name of the user who last updated an entity.")
  QueryConditionDto<String> updatedBy;

  @Schema(description = "A substring of the displayName of an element.")
  QueryConditionDto<String> displayName;

  @Schema(description = "An element's sub type.")
  QueryConditionDto<String> subType;

  @Schema(description = "An element's status.")
  QueryConditionDto<String> status;

  @Schema(description = "IDs of an element's child elements.")
  QueryConditionDto<String> childElementIds;

  @Schema(
      description = "Whether the elements must / mustn't have child elements (members or parts)")
  SingleValueQueryConditionDto<Boolean> hasChildElements;

  @Schema(
      description =
          "Whether the elements must / mustn't have parent elements (scopes or composites)")
  SingleValueQueryConditionDto<Boolean> hasParentElements;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Encodes this search query as a base64url-encoded, compressed string. This representation only
   * contains unreserved URI characters (see RFC 3986 section 2.3).
   *
   * @see SearchQueryDto#decodeFromSearchId(String)
   */
  @JsonIgnore
  public String getSearchId() throws IOException {
    return Base64.getUrlEncoder().encodeToString(toCompressedForm());
  }

  private byte[] toCompressedForm() throws IOException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try (DeflaterOutputStream deflaterOutputStream =
        new DeflaterOutputStream(stream, new Deflater(Deflater.DEFAULT_COMPRESSION, true))) {
      deflaterOutputStream.write(
          OBJECT_MAPPER.writeValueAsString(this).getBytes(StandardCharsets.UTF_8));
    }
    return stream.toByteArray();
  }

  /** Decodes a search query from a base64url-encoded, compressed string. */
  public static SearchQueryDto decodeFromSearchId(String searchId) throws IOException {
    return decodeFromSearchId(searchId, Base64.getUrlDecoder(), SearchQueryDto.class);
  }

  /**
   * Decodes full search query from the encoded string.
   *
   * <p>Can be used to provide named-searches without storing search-IDs and parameters in the
   * database.
   *
   * @param searchId the encoded string containing al search parameters
   * @return the reconstructed search query
   * @throws IOException
   */
  protected static <T extends SearchQueryDto> T decodeFromSearchId(
      String searchId, Base64.Decoder decoder, Class<T> clazz) throws IOException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try (InflaterOutputStream inflaterOutputStream =
        new InflaterOutputStream(stream, new Inflater(true))) {
      inflaterOutputStream.write(decoder.decode(searchId.getBytes(StandardCharsets.UTF_8)));
    }
    return OBJECT_MAPPER.readValue(stream.toString(StandardCharsets.UTF_8), clazz);
  }
}
