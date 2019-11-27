/*******************************************************************************
 * Copyright (c) 2017 Daniel Murygin.
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
package org.veo.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

import org.veo.core.VeoCoreConfiguration;

/**
 * @author Daniel Murygin dm[at]sernet[dot]de
 */
@SpringBootApplication(scanBasePackages = {
        "org.veo.adapter.usecase.interactor.UseCaseInteractor" })
@Import(VeoCoreConfiguration.class)
@SecurityScheme(name = RestApplication.SECURITY_SCHEME_BEARER_AUTH,
                type = SecuritySchemeType.HTTP,
                scheme = "bearer",
                bearerFormat = "jwt")
@OpenAPIDefinition(info = @Info(title = "verinice.VEO REST API",
                                description = "OpenAPI documentation for verinice.VEO.",
                                license = @License(name = "GNU Lesser General Public License",
                                                   url = "https://www.gnu.org/licenses/lgpl-3.0.de.html"),
                                contact = @Contact(url = "http://verinice.com",
                                                   email = "verinice@sernet.de")))
public class RestApplication {

    public static final String SECURITY_SCHEME_BEARER_AUTH = "BearerAuth";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    public static void main(String[] args) {
        SpringApplication.run(RestApplication.class, args);
    }

}
