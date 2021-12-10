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
import java.util.Objects;

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

    private static ApplicationUser user;
    private static Authentication token;

    @FunctionalInterface
    interface Task {
        void exec();
    }

    static void runInClient(Client client, final Task function) {
        final Authentication originalAuthentication = SecurityContextHolder.getContext()
                                                                           .getAuthentication();
        user = Objects.requireNonNullElse(user,
                                          ApplicationUser.authenticatedUser(SYSTEMUSER_NAME,
                                                                            client.getIdAsString(),
                                                                            VEO_USER_SCOPE,
                                                                            Collections.emptyList()));
        token = Objects.requireNonNullElse(token, new AnonymousAuthenticationToken(SYSTEMUSER_NAME,
                user, List.of(new SimpleGrantedAuthority(VEO_USER))));

        try {
            SecurityContextHolder.getContext()
                                 .setAuthentication(token);
            function.exec();
        } finally {
            SecurityContextHolder.getContext()
                                 .setAuthentication(originalAuthentication);
        }
    }
}
