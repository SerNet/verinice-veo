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
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

import org.veo.rest.security.ApplicationUser

/**
 * Helper class to switch between multiple users in tests.
 * Use for tests that cannot rely on "@WithUserDetails".
 */
class UserSwitcher {

    def runAsUser(String username, boolean admin=false, String clientId, int maxUnits = 2, final Closure closure) {
        def currentAuth = SecurityContextHolder.getContext()
                .getAuthentication()
        try {
            var user = ApplicationUser.authenticatedUser(username,
                    clientId,
                    "veo-user", admin ? ["veo-admin"]: [], maxUnits)
            var token = new AnonymousAuthenticationToken(username, user,
                    List.of(new SimpleGrantedAuthority("SCOPE_veo-user")))
            SecurityContextHolder.getContext().setAuthentication(token)
            return closure.call()
        } finally {
            if (currentAuth != null) {
                SecurityContextHolder.getContext().setAuthentication(currentAuth)
            }
        }
    }

    def runAsAdmin(final Closure closure) {
        runAsUser('admin', true, null, closure)
    }
}
