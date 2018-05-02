/*******************************************************************************
 * Copyright (c) 2018 Alexander Ben Nasrallah.
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
 *     Alexander Ben Nasrallah <an@sernet.de> - initial API and implementation
 ******************************************************************************/
package org.veo.rest;

import java.io.File;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller shall provide access to static resource on the file system.
 */
@RestController
public class StaticController {
    private static final Logger logger = LoggerFactory.getLogger(StaticController.class);

    private static final MediaType JSON_SCHEMA_UTF8 = MediaType
            .valueOf("application/schema+json; charset=utf-8");

    @Autowired
    private VeoConfigurationService configuration;

    @RequestMapping(value = "/schemas/{name:.+}" /* accept dots in name */, method = RequestMethod.GET)
    public ResponseEntity<Resource> getSchema(@PathVariable("name") String schemaName) {

        Resource resource = getSchemaResource(schemaName);
        if (!resource.exists()) {
            logger.info("Caught request to non-existent schema {}", schemaName);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(JSON_SCHEMA_UTF8).body(resource);

    }

    private @NotNull Resource getSchemaResource(String schemaName) {
        if (configuration.getSchemaDir() != null) {
            File schemaFile = new File(configuration.getSchemaDir(), schemaName);
            return new FileSystemResource(schemaFile);

        } else {
            return new ClassPathResource("/schemas/".concat(schemaName));
        }
    }
}
