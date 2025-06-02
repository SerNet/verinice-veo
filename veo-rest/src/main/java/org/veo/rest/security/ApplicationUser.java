/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2018  Alexander Ben Nasrallah.
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** ApplicationUser describes an authenticated user received from the user details service. */
@Getter
@Data
@RequiredArgsConstructor
public class ApplicationUser implements UserDetails {
  private static final String UUID_REGEX = "[a-fA-F\\d]{8}(?:-[a-fA-F\\d]{4}){3}-[a-fA-F\\d]{12}";

  private String password = null; // unused but required by UserDetails
  private final String username;
  private final String clientId;
  private final String scopes;
  private final String email;
  private final String name;
  private final String givenName;
  private final String familyName;
  private final List<String> groups;
  private final List<String> roles;
  private final Integer maxUnits;

  private Collection<? extends GrantedAuthority> authorities = Collections.emptyList();
  private boolean accountNonExpired = !false;
  private boolean accountNonLocked = !false;
  private boolean credentialsNonExpired = !false;
  private boolean enabled = true;
  private Map<String, Object> claims;

  private ApplicationUser(Jwt jwt) {
    this(
        jwt.getClaimAsString("preferred_username"),
        extractClientId(jwt.getClaimAsStringList("groups")),
        jwt.getClaimAsString("scope"),
        jwt.getClaimAsString("email"),
        jwt.getClaimAsString("name"),
        jwt.getClaimAsString("given_name"),
        jwt.getClaimAsString("family_name"),
        jwt.getClaimAsStringList("groups"),
        jwt.getClaimAsStringList("roles"),
        Optional.ofNullable(jwt.getClaimAsString("max_units")).map(Integer::parseInt).orElse(null));
    this.claims = jwt.getClaims();
  }

  private static String extractClientId(List<String> groups) {
    List<String> clientIDs =
        Optional.ofNullable(groups).orElseGet(Collections::emptyList).stream()
            .filter(g -> g.matches("^/veo_client:" + UUID_REGEX + "$"))
            .map(g -> g.replaceFirst("/veo_client:", ""))
            .toList();

    if (clientIDs.size() != 1) {
      throw new IllegalArgumentException(
          String.format("Expected 1 client for the account. Got %d.", clientIDs.size()));
    }

    return clientIDs.get(0);
  }

  public static ApplicationUser authenticatedUser(Object principal) {
    if (principal instanceof Jwt jwt) return new ApplicationUser(jwt);
    else if (principal instanceof ApplicationUser applicationUser) return applicationUser;
    throw new IllegalArgumentException("Principal does not represent an authenticated user.");
  }

  public static ApplicationUser authenticatedUser(
      String username, String clientId, String scopes, List<String> roles, Integer maxUnits) {
    return new ApplicationUser(
        username, clientId, scopes, "", "", "", "", Collections.emptyList(), roles, maxUnits);
  }

  public boolean isAdmin() {
    return roles.contains("veo-admin");
  }
}
