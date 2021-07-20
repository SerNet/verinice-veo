/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade
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

import java.io.IOException;
import java.util.Base64;

import javax.validation.Valid;

import org.veo.core.entity.Process.Status;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

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
@Schema(title = "ProcessSearchQueryDto", description = "A search query for processes")
@Valid
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Jacksonized
public class ProcessSearchQueryDto extends SearchQueryDto {

    @Schema(description = "A process's status.")
    QueryConditionDto<Status> status;

    /**
     * Decodes a search query from a base64url-encoded, compressed string.
     */
    public static ProcessSearchQueryDto decodeFromSearchId(String searchId) throws IOException {
        return decodeFromSearchId(searchId, Base64.getUrlDecoder(), ProcessSearchQueryDto.class);
    }

}
