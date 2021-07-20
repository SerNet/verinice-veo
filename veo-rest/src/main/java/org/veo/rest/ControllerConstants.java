/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Daniel Murygin.
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
package org.veo.rest;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.veo.adapter.presenter.api.dto.ProcessSearchQueryDto;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.core.entity.Process.Status;
import org.veo.rest.security.ApplicationUser;

/**
 * This class contains constants for the REST controllers.
 */
// @SecurityRequirement(name = RestApplication.SECURITY_SCHEME_BEARER_AUTH)
public final class ControllerConstants {

    // Placeholder objects to generate method proxies for HATEOAS URLs:
    public static final Authentication ANY_AUTH = new UsernamePasswordAuthenticationToken(null,
            null);
    public static final ApplicationUser ANY_USER = null;
    public static final Integer ANY_INT = null;
    public static final SearchQueryDto ANY_SEARCH = null;
    public static final ProcessSearchQueryDto ANY_PROCESS_SEARCH = null;
    public static final String ANY_STRING = null;
    public static final List<String> ANY_STRING_LIST = null;
    public static final Status ANY_STATUS = null;

    static final String PARENT_PARAM = "parent";
    static final String UUID_PARAM = "uuid";
    static final String UUID_REGEX = "[a-fA-F\\d]{8}(?:-[a-fA-F\\d]{4}){3}-[a-fA-F\\d]{12}";
    static final String UNIT_PARAM = "unit";
    static final String DOMAIN_PARAM = "domain";
    static final String SUB_TYPE_PARAM = "subType";
    static final String DISPLAY_NAME_PARAM = "displayName";
    static final String STATUS_PARAM = "status";
    static final String PAGE_SIZE_PARAM = "size";
    static final String PAGE_SIZE_DEFAULT_VALUE = "20";
    static final String PAGE_NUMBER_PARAM = "page";
    static final String PAGE_NUMBER_DEFAULT_VALUE = "0";
    static final String SORT_COLUMN_PARAM = "sortBy";
    static final String SORT_COLUMN_DEFAULT_VALUE = "name";
    static final String SORT_ORDER_PARAM = "sortOrder";
    static final String SORT_ORDER_DEFAULT_VALUE = "asc";
    static final String SORT_ORDER_PATTERN = "[asc|desc|ASC|DESC]";
    static final String IF_MATCH_HEADER = "If-Match";

    static final String UUID_DEFINITION = "This is the normalized UUID representation:\n"
            + "* a block of 8 HEX chars followed by\n* 3 blocks of 4 HEX chars followed by\n"
            + "* a block of 12 HEX chars.";
    static final String UUID_EXAMPLE = "f35b982c-8ad4-4515-96ee-df5fdd4247b9";

    private ControllerConstants() {
    }

}
