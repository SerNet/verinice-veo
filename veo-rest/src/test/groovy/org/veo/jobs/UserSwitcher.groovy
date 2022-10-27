/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman
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
package org.veo.jobs

import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

import org.veo.rest.security.ApplicationUser

/**
 * Helper class to switch between multiple users in tests.
 * Use for tests that cannot rely on "@WithUserDetails".
 */
class UserSwitcher {

    Authentication originalAuthentication

    void revokeUser() {
        if (originalAuthentication != null)
            SecurityContextHolder.getContext().setAuthentication(originalAuthentication)
    }

    void switchToUser(String username, String clientId, int maxUnits = 2) {
        this.originalAuthentication = SecurityContextHolder.getContext()
                .getAuthentication()
        var user = ApplicationUser.authenticatedUser(username,
                clientId,
                "veo-user", Collections.emptyList(), maxUnits)
        var token = new AnonymousAuthenticationToken(username, user,
                List.of(new SimpleGrantedAuthority("SCOPE_veo-user")))
        SecurityContextHolder.getContext().setAuthentication(token)
    }
}
