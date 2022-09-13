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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import org.veo.rest.security.ApplicationUser;

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
            "user@domain.example", TESTCLIENT_UUID, "veo-user", List.of("veo-user", "veo-write"));
    basicUser.setAuthorities(
        List.of(
            new SimpleGrantedAuthority("ROLE_veo-user"),
            new SimpleGrantedAuthority("ROLE_veo-write")));

    ApplicationUser adminUser =
        ApplicationUser.authenticatedUser(
            "admin", TESTCLIENT_UUID, "veo-admin", List.of("veo-user", "veo-admin", "veo-write"));
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
            List.of("veo-user", "veo-content-creator", "veo-write"));
    contentCreatorUser.setAuthorities(
        List.of(
            new SimpleGrantedAuthority("ROLE_veo-user"),
            new SimpleGrantedAuthority("ROLE_veo-content-creator"),
            new SimpleGrantedAuthority("ROLE_veo-write")));

    // A user with read-only access:
    ApplicationUser readOnlyUser =
        ApplicationUser.authenticatedUser(
            "read-only-user", TESTCLIENT_UUID, "veo-user", List.of("veo-user"));
    readOnlyUser.setAuthorities(List.of(new SimpleGrantedAuthority("ROLE_veo-user")));

    // A user with no rights:
    ApplicationUser noRightsUser =
        ApplicationUser.authenticatedUser(
            "no-rights-user", TESTCLIENT_UUID, "veo-user", Collections.emptyList());
    noRightsUser.setAuthorities(Collections.emptyList());

    // A content-creator with no write access:
    ApplicationUser contentCreatorUserReadonly =
        ApplicationUser.authenticatedUser(
            "content-creator-readonly",
            TESTCLIENT_UUID,
            "veo-content-creator",
            List.of("veo-user", "veo-content-creator"));
    contentCreatorUserReadonly.setAuthorities(
        List.of(
            new SimpleGrantedAuthority("ROLE_veo-user"),
            new SimpleGrantedAuthority("ROLE_veo-content-creator")));

    return new CustomUserDetailsManager(
        Arrays.asList(
            basicUser,
            adminUser,
            contentCreatorUser,
            readOnlyUser,
            noRightsUser,
            contentCreatorUserReadonly));
  }

  /**
   * In-memory implementation of a custom user details service for testing.
   *
   * <p>It returns ApplicationUser objects which hold information about the Client-ID in addition to
   * credentials, enabled-status etc.
   *
   * <p>In production, this should use a user repository or even better a separate authorization
   * (micro)service.
   *
   * @author akoderman
   */
  static class CustomUserDetailsManager extends InMemoryUserDetailsManager {

    // This would be a repository or service in production:
    private final Map<String, ApplicationUser> users = new HashMap<>();

    public CustomUserDetailsManager(Collection<ApplicationUser> appUsers) {
      super(appUsers.stream().map(u -> (UserDetails) u).toList());
      appUsers.stream().forEach(this::storeUser);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
      ApplicationUser user = users.get(username);
      if (user == null) throw new UsernameNotFoundException(username);
      return user;
    }

    private void storeUser(ApplicationUser user) {
      users.put(user.getUsername(), user);
    }
  }
}
