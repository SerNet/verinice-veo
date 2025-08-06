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

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import org.veo.persistence.CurrentUserProvider;
import org.veo.persistence.LenientCurrentUserProviderImpl;
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
    return new CustomUserDetailsManager();
  }

  @Bean
  public InMemoryUserDetailsManager inMemoryUserDetailsManager() {
    return (InMemoryUserDetailsManager) userDetailsService();
  }

  @Bean
  @Primary
  public CurrentUserProvider testCurrentUserProvider(AuditorAware<String> auditorAware) {
    return new LenientCurrentUserProviderImpl(auditorAware);
  }
}
