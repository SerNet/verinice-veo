/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.rest.configuration;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        ApplicationUser basicUser = ApplicationUser.authenticatedUser("user@domain.example",
                                                                      TESTCLIENT_UUID, "veo-user");
        basicUser.setAuthorities(List.of(new SimpleGrantedAuthority("SCOPE_veo-user")));

        ApplicationUser adminUser = ApplicationUser.authenticatedUser("admin", TESTCLIENT_UUID,
                                                                      "veo-admin");
        adminUser.setAuthorities(List.of(new SimpleGrantedAuthority("SCOPE_veo-user"),
                                         new SimpleGrantedAuthority("SCOPE_veo-admin")));

        return new CustomUserDetailsManager(Arrays.asList(basicUser, adminUser));
    }

    /**
     * In-memory implementation of a custom user details service for testing.
     *
     * It returns ApplicationUser objects which hold information about the Client-ID
     * in addition to credentials, enabled-status etc.
     *
     * In production, this should use a user repository or even better a separate
     * authorization (micro)service.
     *
     * @author akoderman
     */
    class CustomUserDetailsManager extends InMemoryUserDetailsManager {

        // This would be a repository or service in production:
        private Map<String, ApplicationUser> users = new HashMap<String, ApplicationUser>();

        public CustomUserDetailsManager(Collection<ApplicationUser> appUsers) {
            super(appUsers.stream()
                          .map(u -> (UserDetails) u)
                          .collect(Collectors.toList()));
            appUsers.stream()
                    .forEach(this::storeUser);

        }

        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            ApplicationUser user = users.get(username);
            if (user == null)
                throw new UsernameNotFoundException(username);
            return user;
        }

        private void storeUser(ApplicationUser user) {
            users.put(user.getUsername(), user);
        }
    }
}
