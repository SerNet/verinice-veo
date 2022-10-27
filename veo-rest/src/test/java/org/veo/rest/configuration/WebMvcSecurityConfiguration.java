/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Alexander Koderman.
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
package org.veo.rest.configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import org.veo.persistence.CurrentUserProvider;
import org.veo.persistence.LenientCurrentUserProviderImpl;
import org.veo.rest.security.ApplicationUser;
import org.veo.rest.security.CustomUserDetailsManager;

@TestConfiguration
public class WebMvcSecurityConfiguration {

  // A randomly generated client id to use in tests.
  // (The NIL UUID is not usable as client-ID because it's used as an 'undefined'
  // key.)
  public static final String TESTCLIENT_UUID = "274af105-8d21-4f07-8019-5c4573d503e5";

  @Bean
  @Primary
  public UserDetailsService userDetailsService() {
    // A user with read and write access (used by most tests):
    ApplicationUser basicUser =
        ApplicationUser.authenticatedUser(
            "user@domain.example",
            TESTCLIENT_UUID,
            "veo-user",
            List.of("veo-user", "veo-write"),
            2);
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

    return new CustomUserDetailsManager(
        Arrays.asList(
            basicUser,
            manyUnitsCreator,
            adminUser,
            contentCreatorUser,
            readOnlyUser,
            noRightsUser,
            contentCreatorUserReadonly));
  }

  @Bean
  public InMemoryUserDetailsManager inMemoryUserDetailsManager() {
    final String nilUUID = "00000000-0000-0000-0000-000000000000";

    ApplicationUser basicUser =
        ApplicationUser.authenticatedUser("user", nilUUID, "veo-user", Collections.emptyList(), 2);
    basicUser.setAuthorities(List.of(new SimpleGrantedAuthority("SCOPE_veo-user")));

    ApplicationUser adminUser =
        ApplicationUser.authenticatedUser(
            "admin", nilUUID, "veo-admin", Collections.emptyList(), 100);
    adminUser.setAuthorities(List.of(new SimpleGrantedAuthority("SCOPE_veo-admin")));

    return new CustomUserDetailsManager(List.of(basicUser, adminUser));
  }

  @Bean
  @Primary
  public CurrentUserProvider testCurrentUserProvider(AuditorAware<String> auditorAware) {
    return new LenientCurrentUserProviderImpl(auditorAware);
  }
}
