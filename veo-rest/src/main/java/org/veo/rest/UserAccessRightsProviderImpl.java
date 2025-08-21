/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Jochen Kemnade
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

import org.springframework.security.core.context.SecurityContextHolder;

import org.veo.core.UserAccessRights;
import org.veo.core.service.UserAccessRightsProvider;
import org.veo.rest.security.ApplicationUser;

public class UserAccessRightsProviderImpl implements UserAccessRightsProvider {
  @Override
  public UserAccessRights getAccessRights() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    return ApplicationUser.authenticatedUser(auth.getPrincipal());
  }
}
