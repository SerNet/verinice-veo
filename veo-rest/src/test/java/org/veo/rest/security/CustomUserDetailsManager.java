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
package org.veo.rest.security;

import static org.veo.rest.configuration.WebMvcSecurityConfiguration.TESTCLIENT_UUID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Component;

/**
 * In-memory implementation of a custom user details service for testing.
 *
 * <p>It returns ApplicationUser objects which hold information about the Client-ID in addition to
 * credentials, enabled-status etc.
 *
 * <p>In production, this should use a user repository or even better a separate authorization
 * (micro)service.
 */
@Component
public class CustomUserDetailsManager extends InMemoryUserDetailsManager {

  public CustomUserDetailsManager() {
    restoreDefaultUsers();
  }

  // This would be a repository or service in production:
  private final Map<String, ApplicationUser> users = new HashMap<>();

  @Override
  public ApplicationUser loadUserByUsername(String username) throws UsernameNotFoundException {
    ApplicationUser user = users.get(username);
    if (user == null) throw new UsernameNotFoundException(username);
    return user;
  }

  private void storeUser(ApplicationUser user) {
    users.put(user.getUsername(), user);
  }

  public void restoreDefaultUsers() {
    // A user with read and write access (used by most tests):
    ApplicationUser basicUser =
        ApplicationUser.authenticatedUser(
            "user@domain.example",
            TESTCLIENT_UUID,
            "veo-user",
            new ArrayList<>(List.of("veo-user", "veo-write")),
            2,
            new HashSet<>(),
            new HashSet<>());
    basicUser.setAuthorities(
        List.of(
            new SimpleGrantedAuthority("ROLE_veo-user"),
            new SimpleGrantedAuthority("ROLE_veo-write")));

    // A user with read and write access who can create up to 50 units
    ApplicationUser manyUnitsCreator =
        ApplicationUser.authenticatedUser(
            "manyunitscreator@domain.example",
            TESTCLIENT_UUID,
            "veo-user",
            List.of("veo-user", "veo-write"),
            50);
    manyUnitsCreator.setAuthorities(
        List.of(
            new SimpleGrantedAuthority("ROLE_veo-user"),
            new SimpleGrantedAuthority("ROLE_veo-write")));

    ApplicationUser adminUser =
        ApplicationUser.authenticatedUser(
            "admin",
            TESTCLIENT_UUID,
            "veo-admin",
            List.of("veo-user", "veo-admin", "veo-write"),
            100);
    adminUser.setAuthorities(
        List.of(
            new SimpleGrantedAuthority("ROLE_veo-user"),
            new SimpleGrantedAuthority("ROLE_veo-admin"),
            new SimpleGrantedAuthority("ROLE_veo-write")));

    ApplicationUser contentCreatorUser =
        ApplicationUser.authenticatedUser(
            "content-creator",
            TESTCLIENT_UUID,
            "veo-content-creator",
            List.of("veo-user", "veo-content-creator", "veo-write"),
            2);
    contentCreatorUser.setAuthorities(
        List.of(
            new SimpleGrantedAuthority("ROLE_veo-user"),
            new SimpleGrantedAuthority("ROLE_veo-content-creator"),
            new SimpleGrantedAuthority("ROLE_veo-write")));

    // A user with read-only access:
    ApplicationUser readOnlyUser =
        ApplicationUser.authenticatedUser(
            "read-only-user", TESTCLIENT_UUID, "veo-user", List.of("veo-user"), 0);
    readOnlyUser.setAuthorities(List.of(new SimpleGrantedAuthority("ROLE_veo-user")));

    // A user with no rights:
    ApplicationUser noRightsUser =
        ApplicationUser.authenticatedUser(
            "no-rights-user", TESTCLIENT_UUID, "veo-user", Collections.emptyList(), 0);
    noRightsUser.setAuthorities(Collections.emptyList());

    // A content-creator with no write access:
    ApplicationUser contentCreatorUserReadonly =
        ApplicationUser.authenticatedUser(
            "content-creator-readonly",
            TESTCLIENT_UUID,
            "veo-content-creator",
            List.of("veo-user", "veo-content-creator"),
            0);
    contentCreatorUserReadonly.setAuthorities(
        List.of(
            new SimpleGrantedAuthority("ROLE_veo-user"),
            new SimpleGrantedAuthority("ROLE_veo-content-creator")));

    users.clear();
    Arrays.asList(
            basicUser,
            manyUnitsCreator,
            adminUser,
            contentCreatorUser,
            readOnlyUser,
            noRightsUser,
            contentCreatorUserReadonly)
        .forEach(this::storeUser);
  }
}
