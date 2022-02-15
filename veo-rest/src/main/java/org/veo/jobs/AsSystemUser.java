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
package org.veo.jobs;

import java.util.Collections;
import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.veo.core.entity.Client;
import org.veo.rest.security.ApplicationUser;

/**
 * Runs the given task with the system user account. The system user is always
 * bound to a specific client during execution. The security context is reset to
 * its previous state after the task was executed.
 */
class AsSystemUser {

    private static final String VEO_USER = "SCOPE_veo-user";
    private static final String SYSTEMUSER_NAME = "system";
    private static final String VEO_USER_SCOPE = "veo-user";
    public static final String VEO_ADMIN_ROLE = "veo-admin";
    public static final String VEO_CONTENT_CREATOR_ROLE = "veo-content-creator";

    static void runAsAdmin(final Runnable function) {
        var user = ApplicationUser.authenticatedUser(SYSTEMUSER_NAME, null, VEO_USER_SCOPE,
                                                     List.of(VEO_ADMIN_ROLE));
        run(function, user);
    }

    static void runAsContentCreator(final Runnable function) {
        var user = ApplicationUser.authenticatedUser(SYSTEMUSER_NAME, null, VEO_USER_SCOPE,
                                                     List.of(VEO_CONTENT_CREATOR_ROLE));
        run(function, user);
    }

    static void runInClient(Client client, final Runnable function) {
        var user = ApplicationUser.authenticatedUser(SYSTEMUSER_NAME, client.getIdAsString(),
                                                     VEO_USER_SCOPE, Collections.emptyList());
        run(function, user);
    }

    private static void run(Runnable function, ApplicationUser user) {
        final Authentication originalAuthentication = SecurityContextHolder.getContext()
                                                                           .getAuthentication();
        var token = new AnonymousAuthenticationToken(SYSTEMUSER_NAME, user,
                List.of(new SimpleGrantedAuthority(VEO_USER)));

        try {
            SecurityContextHolder.getContext()
                                 .setAuthentication(token);
            function.run();
        } finally {
            SecurityContextHolder.getContext()
                                 .setAuthentication(originalAuthentication);
        }
    }
}
