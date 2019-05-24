/*******************************************************************************
 * Copyright (c) 2018 Jochen Kemnade.
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
package org.veo.ie;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import org.veo.core.VeoCoreConfiguration;

/**
 * Spring Boot application class to run the import of a VNA file. You should
 * start this application with property spring.main.web-environment=false in
 * your application.properties file to turn the starting of a web environment
 * off.
 */

@SpringBootApplication
@Import(VeoCoreConfiguration.class)
public class VnaImportConfiguration {

}
