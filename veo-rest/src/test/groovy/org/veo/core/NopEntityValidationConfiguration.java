/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
package org.veo.core;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import org.veo.core.entity.code.EntityValidationException;
import org.veo.core.entity.specification.EntityValidator;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@TestConfiguration
@Slf4j
public class NopEntityValidationConfiguration {
  @Bean
  @Primary
  EntityValidator nopEntityValidator() {
    return new EntityValidator(null) {
      @Override
      public void validate(@NonNull Object entity) throws EntityValidationException {
        log.debug(
            "Validation is disabled by the test configuration. Not validating entity {}!", entity);
      }
    };
  }
}
