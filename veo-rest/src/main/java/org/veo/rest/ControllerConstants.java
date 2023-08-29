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

import org.springframework.http.CacheControl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.presenter.api.dto.RequirementImplementationDto;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.rest.security.ApplicationUser;

/** This class contains constants for the REST controllers. */
// @SecurityRequirement(name = RestApplication.SECURITY_SCHEME_BEARER_AUTH)
public final class ControllerConstants {

  // Placeholder objects to generate method proxies for HATEOAS URLs:
  public static final Authentication ANY_AUTH = new UsernamePasswordAuthenticationToken(null, null);
  public static final ApplicationUser ANY_USER = null;
  public static final Integer ANY_INT = null;
  public static final SearchQueryDto ANY_SEARCH = null;
  public static final String ANY_STRING = null;
  public static final List<String> ANY_STRING_LIST = null;
  public static final Boolean ANY_BOOLEAN = null;
  public static final WebRequest ANY_REQUEST = null;
  public static final RequirementImplementationDto ANY_REQUIREMENT_IMPLEMENTATION = null;
  public static final String SCOPE_IDS_PARAM = "scopes";
  public static final String SCOPE_IDS_DESCRIPTION =
      "IDs of scopes that the new element should be a member of";
  static final String PARENT_PARAM = "parent";
  static final String UUID_PARAM = "uuid";
  static final String UUID_REGEX = "[a-fA-F\\d]{8}(?:-[a-fA-F\\d]{4}){3}-[a-fA-F\\d]{12}";
  static final String UUID_PARAM_SPEC = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}";
  static final String UNIT_PARAM = "unit";
  static final String DESCRIPTION_PARAM = "description";
  static final String DESIGNATOR_PARAM = "designator";
  static final String DOMAIN_PARAM = "domain";
  static final String NAME_PARAM = "name";
  static final String ELEMENT_TYPE_PARAM = "elementType";
  static final String SUB_TYPE_PARAM = "subType";
  static final String STATUS_PARAM = "status";
  static final String CHILD_ELEMENT_IDS_PARAM = "childElementIds";
  static final String HAS_CHILD_ELEMENTS_PARAM = "hasChildElements";
  static final String HAS_PARENT_ELEMENTS_PARAM = "hasParentElements";
  static final String UPDATED_BY_PARAM = "updatedBy";
  static final String DISPLAY_NAME_PARAM = "displayName";
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

  static final String UUID_DESCRIPTION =
      "A UUID conforming to RFC4122 in canonical textual representation.";
  static final String UUID_EXAMPLE = "f35b982c-8ad4-4515-96ee-df5fdd4247b9";
  static final String EMBED_RISKS_DESC = "Embed the risk values in the response.";

  static final CacheControl DEFAULT_CACHE_CONTROL = CacheControl.noCache();

  private ControllerConstants() {}
}
