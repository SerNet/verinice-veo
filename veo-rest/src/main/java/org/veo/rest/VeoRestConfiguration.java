/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2018  Jochen Kemnade.
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
package org.veo.rest;

import java.time.Duration;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import org.veo.core.usecase.common.ETag;

import lombok.Getter;

@Configuration
@ComponentScan("org.veo")
public class VeoRestConfiguration {
    public static final String PROFILE_BACKGROUND_TASKS = "background-tasks";

    @Value("${veo.etag.salt}")
    private String eTagSalt;

    @Value("${veo.messages.publishing.lockExpirationMs:20000}")
    @Getter
    private Duration messagePublishingLockExpiration;

    @PostConstruct
    public void configureETagSalt() {
        ETag.setSalt(eTagSalt);
    }
}
