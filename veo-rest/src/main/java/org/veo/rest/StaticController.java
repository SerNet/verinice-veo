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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.veo.service.VeoConfigurationService;

/**
 * This controller shall provide access to static resource on the file system.
 */
@RestController
@SecurityRequirement(name = RestApplication.SECURITY_SCHEME_BEARER_AUTH)
public class StaticController {
    private static final Logger logger = LoggerFactory.getLogger(StaticController.class);

    private static final MediaType JSON_SCHEMA_UTF8 = MediaType.valueOf("application/schema+json; charset=utf-8");

    @Autowired
    private VeoConfigurationService configuration;

    @GetMapping(value = "/schemas", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Operation(summary = "Loads all element schemas",
               description = "This endpoint is deprecated. Use \"/schemas/elements\" instead.")
    @Deprecated
    public ResponseEntity<String[]> getSchemasDeprecated() throws IOException {
        return listFiles("classpath:schemas/elements/*.json", getSchemaDirectory());
    }

    @GetMapping(value = "/schemas/elements", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Operation(summary = "Loads all element schemas")
    public ResponseEntity<String[]> getSchemas() throws IOException {
        return listFiles("classpath:schemas/elements/*.json", getSchemaDirectory());
    }

    @GetMapping(value = "/schemas/links", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Operation(summary = "Loads all link schemas")
    public ResponseEntity<String[]> getlinkSchemas() throws IOException {
        return listFiles("classpath:schemas/links/*.json", getLinkSchemaDirectory());

    }

    @GetMapping(value = "/schemas/{name:.+}")
    @Operation(summary = "Loads an element schema",
               description = "This endpoint is deprecated. Use \"/schemas/elements/{name:.+}\" instead.")
    @Deprecated
    public ResponseEntity<Resource> getSchemaDeprecated(
            @Parameter(name = "name",
                       description = "The file name of the element schema",
                       example = "asset.json",
                       required = true) @PathVariable("name") String schemaName) {
        return getSchema(schemaName);
    }

    @GetMapping(value = "/schemas/elements/{name:.+}")
    @Operation(summary = "Loads an element schema")
    public ResponseEntity<Resource> getSchema(
            @Parameter(name = "name",
                       description = "The file name of the element schema",
                       example = "asset.json",
                       required = true) @PathVariable("name") String schemaName) {

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

    @GetMapping(value = "/schemas/links/{name:.+}")
    @Operation(summary = "Loads a link schema")
    public ResponseEntity<Resource> getLinkSchema(
            @Parameter(name = "name",
                       description = "The file name of the link schema",
                       example = "asset_control.json",
                       required = true) @PathVariable("name") String schemaName) {
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
