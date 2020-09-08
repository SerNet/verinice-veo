/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.adapter.presenter.api.dto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import org.veo.core.entity.GroupType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * Representation of a (named) search query.
 * <p>
 * Currently only supports filtering by unit-ID. Can be extended to allow
 * additional filter functions.
 * <p>
 * Can be encoded/decoded as an ID string to be used as a named search without
 * having to store the search in the database.
 */
@Value
@Builder
@Schema(title = "SearchQueryDto", description = "A search query")
@Valid
@JsonDeserialize(builder = SearchQueryDto.SearchQueryDtoBuilder.class)
public class SearchQueryDto {

    @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
             message = "ID must be a valid UUID string following RFC 4122.")
    @Schema(description = "The ID of the unit of which the searches elements must be a member.")
    String unitId;

    @Schema(description = "For group searches: the type of the entity that is being searched. "
            + "Ignored for searches returning single entities.")
    GroupType groupType;

    @Size(min = 3, max = 255)
    @Schema(description = "A substring of the displayName of an entity.")
    String displayName;

    // TODO VEO-38 implement search by displayName and possibly other fields

    public SearchQueryDto(String unitId, GroupType entityType, String displayName) {
        this.unitId = unitId;
        this.groupType = entityType;
        this.displayName = displayName;
    }

    /**
     * Encodes this search query as a base64-encoded, compressed string.
     *
     * @return the encoded string containing all search parameters
     * @throws IOException
     */
    @JsonIgnore
    public String getSearchId() throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(stream,
                new Deflater(Deflater.DEFAULT_COMPRESSION, true));
        deflaterOutputStream.write(new ObjectMapper().writeValueAsString(this)
                                                     .getBytes("UTF-8"));
        deflaterOutputStream.close();
        return Base64.getEncoder()
                     .encodeToString(stream.toByteArray());
    }

    /**
     * Decodes full search query from the encoded string.
     * <p>
     * Can be used to provide named-searches without storing search-IDs and
     * parameters in the database.
     *
     * @param searchId
     *            the encoded string containing al search parameters
     * @return the reconstructed search query
     * @throws IOException
     */
    public static SearchQueryDto decodeFromSearchId(String searchId) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        InflaterOutputStream inflaterOutputStream = new InflaterOutputStream(stream,
                new Inflater(true));
        inflaterOutputStream.write(Base64.getDecoder()
                                         .decode(searchId.getBytes("UTF-8")));
        inflaterOutputStream.close();
        return new ObjectMapper().readValue(stream.toString("UTF-8"), SearchQueryDto.class);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class SearchQueryDtoBuilder {
        // required for Jackson deserialization
    }

}
