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
 ******************************************************************************/
package org.veo.rest;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import org.veo.service.VeoConfigurationService;

/**
 * This controller shall provide access to static resource on the file system.
 */
@RestController
public class StaticController {
    private static final Logger logger = LoggerFactory.getLogger(StaticController.class);

    private static final MediaType JSON_SCHEMA_UTF8 = MediaType.valueOf("application/schema+json; charset=utf-8");

    @Autowired
    private VeoConfigurationService configuration;

    @RequestMapping(value = "/schemas",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Deprecated
    public ResponseEntity<String[]> getSchemasDeprecated() throws IOException {
        return listFiles("classpath:schemas/elements/*.json", getSchemaDirectory());
    }

    @RequestMapping(value = "/schemas/elements",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String[]> getSchemas() throws IOException {
        return listFiles("classpath:schemas/elements/*.json", getSchemaDirectory());
    }

    @RequestMapping(value = "/schemas/links",
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String[]> getlinkSchemas() throws IOException {
        return listFiles("classpath:schemas/links/*.json", getLinkSchemaDirectory());

    }

    @RequestMapping(value = "/schemas/{name:.+}" /* accept dots in name */,
                    method = RequestMethod.GET)
    @Deprecated
    public ResponseEntity<Resource> getSchemaDeprecated(@PathVariable("name") String schemaName) {
        return getSchema(schemaName);
    }

    @RequestMapping(value = "/schemas/elements/{name:.+}" /* accept dots in name */,
                    method = RequestMethod.GET)
    public ResponseEntity<Resource> getSchema(@PathVariable("name") String schemaName) {

        Resource resource = getSchemaResource(schemaName, "/schemas/elements/",
                                              getSchemaDirectory());
        if (!resource.exists()) {
            logger.info("Caught request to non-existent schema {}", schemaName);
            return ResponseEntity.notFound()
                                 .build();
        }
        return ResponseEntity.ok()
                             .contentType(JSON_SCHEMA_UTF8)
                             .body(resource);
    }

    @RequestMapping(value = "/schemas/links/{name:.+}" /* accept dots in name */,
                    method = RequestMethod.GET)
    public ResponseEntity<Resource> getLinkSchema(@PathVariable("name") String schemaName) {
        Resource resource = getSchemaResource(schemaName, "/schemas/links/",
                                              getLinkSchemaDirectory());
        if (!resource.exists()) {
            logger.info("Caught request to non-existent schema {}", schemaName);
            return ResponseEntity.notFound()
                                 .build();
        }
        return ResponseEntity.ok()
                             .contentType(JSON_SCHEMA_UTF8)
                             .body(resource);
    }

    private ResponseEntity<String[]> listFiles(String locationPattern, File schemaDir)
            throws IOException {
        if (schemaDir.exists() && schemaDir.isDirectory()) {
            return ResponseEntity.ok()
                                 .body(schemaDir.list());

        } else {
            PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
            Resource[] schemaResources = resourcePatternResolver.getResources(locationPattern);
            String[] resourceFileNames = Stream.of(schemaResources)
                                               .map(Resource::getFilename)
                                               .toArray(String[]::new);
            return ResponseEntity.ok()
                                 .body(resourceFileNames);
        }
    }

    private @NotNull Resource getSchemaResource(String schemaName, String schemaPath,
            File schemaDir) {
        if (schemaDir.exists() && schemaDir.isDirectory()) {
            logger.info("Providing schemas from {}.", schemaDir.getAbsolutePath());
            File schemaFile = new File(schemaDir, schemaName);
            return new FileSystemResource(schemaFile);
        } else {
            logger.info("Providing schemas from class path resources.");
            return new ClassPathResource(schemaPath.concat(schemaName));
        }
    }

    private File getSchemaDirectory() {
        return new File(configuration.getBaseDir(), "schemas/elements");
    }

    private File getLinkSchemaDirectory() {
        return new File(configuration.getBaseDir(), "schemas/links");
    }
}
