/*******************************************************************************
 * Copyright (c) 2017 Sebastian Hagedorn
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
 *
 * Contributors:
 *     Sebastian Hagedorn - initial API and implementation
 *     Daniel Murygin
 ******************************************************************************/
package org.veo.schema.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.EnableHypermediaSupport;

/**
 * Spring Boot application to run the model schema REST service
 *
 * @author Sebastian Hagedorn
 * @author Daniel Murygin
 */
@SpringBootApplication
@Configuration
@ComponentScan("org.veo.schema")
@EntityScan("org.veo.schema.model")
@EnableHypermediaSupport(type = {EnableHypermediaSupport.HypermediaType.HAL})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}