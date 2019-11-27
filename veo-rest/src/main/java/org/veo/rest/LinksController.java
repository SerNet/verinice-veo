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
package org.veo.rest;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.veo.service.LinkMapService;

/**
 * REST service which provides methods to manage links.
 *
 * An link is a generic Map<String, Object> with just a few requirements, see
 * org.veo.service.LinkMapService.
 */
@RestController
@SecurityRequirement(name = RestApplication.SECURITY_SCHEME_BEARER_AUTH)

public class LinksController {

    private static final String UUID = "uuid";

    private static final String UUID_DESCRIPTION = "UUID of the link";
    private static final String UUID_DEFINITION = "This is the normalized UUID representation:\n"
            + "* a block of 8 HEX chars followed by\n* 3 blocks of 4 HEX chars followed by\n"
            + "* a block of 12 HEX chars.";
    private static final String UUID_EXAMPLE = "f35b982c-8ad4-4515-96ee-df5fdd4247b9";
    // @formatter:off
    private static final String LINK_DATA_EXAMPLE = "{\n" +
            "    \"id\": \"a5f4a759-cce0-4b1f-9c56-f9f56a9c0037\",\n" +
            "    \"schema\": {\n" +
            "        \"$ref\": \"/schemas/link.json\"\n" +
            "    },\n" +
            "    \"source\": {\n" +
            "        \"$ref\": \"/elements/b90e5a21-dd30-4f74-8db0-158bc311b3fc\"\n" +
            "    },\n" +
            "    \"target\": {\n" +
            "        \"$ref\": \"/elements/c241e047-3857-4939-af3e-1e48d053581a\"\n" +
            "    }\n" +
            "}";
    // @formatter:on

    @Autowired
    private LinkMapService mapService;

    public LinksController() {
        super();
    }

    public LinksController(LinkMapService mapRepository) {
        this.mapService = mapRepository;
    }

    @GetMapping(value = "/links")
    @Operation(summary = "Loads all links")
    public ResponseEntity<List<Map<String, Object>>> getLinks() {
        List<Map<String, Object>> result = mapService.findAll();
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(result);
    }

    @GetMapping(value = "/links/{" + UUID + ":.+}")
    @Operation(summary = "Loads a link")
    public ResponseEntity<Map<String, Object>> getLink(
            @Parameter(name = "UUID",
                       description = UUID_DESCRIPTION + "\n\n" + UUID_DEFINITION,
                       example = UUID_EXAMPLE,
                       required = true) @PathVariable(value = UUID) String uuid) {
        Map<String, Object> result = mapService.find(uuid);
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(result);
    }

    @PutMapping(value = "/links/{" + UUID + "}")
    @Operation(summary = "Updates a link")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Link data",
                                                          required = true,
                                                          content = @io.swagger.v3.oas.annotations.media.Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                                                                                 schema = @io.swagger.v3.oas.annotations.media.Schema(example = LINK_DATA_EXAMPLE)))
    public ResponseEntity<Object> getLinks(
            @Parameter(name = "UUID",
                       description = UUID_DESCRIPTION + "\n\n" + UUID_DEFINITION,
                       example = UUID_EXAMPLE,
                       required = true) @PathVariable(value = UUID) String uuid,
            @RequestBody Map<String, Object> content) {
        this.mapService.save(uuid, content);
        return ResponseEntity.noContent()
                             .build();
    }

    @PostMapping(value = "/links")
    @Operation(summary = "Creates a link")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Link data",
                                                          required = true,
                                                          content = @io.swagger.v3.oas.annotations.media.Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                                                                                 schema = @io.swagger.v3.oas.annotations.media.Schema(example = LINK_DATA_EXAMPLE)))
    public ResponseEntity<Object> postLink(@RequestBody Map<String, Object> content) {
        String uuid = this.mapService.saveNew(content);
        return ResponseEntity.created(URI.create("/links/" + uuid))
                             .build();
    }

    @GetMapping(value = "/elements/{" + UUID + "}/links")
    @Operation(summary = "Loads links of an elements")
    public ResponseEntity<List<Map<String, Object>>> getLinks(
            @Parameter(name = "UUID",
                       description = "UUID of the element" + "\n\n" + UUID_DEFINITION,
                       example = UUID_EXAMPLE,
                       required = true) @PathVariable(UUID) String uuid) {
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(mapService.findByElement(uuid));
    }

    @DeleteMapping(value = "/links/{" + UUID + "}")
    @Operation(summary = "Deletes a link")
    public ResponseEntity<Object> deleteLink(
            @Parameter(name = "UUID",
                       description = UUID_DESCRIPTION + "\n\n" + UUID_DEFINITION,
                       example = UUID_EXAMPLE,
                       required = true) @PathVariable(UUID) String uuid) {
        mapService.delete(uuid);
        return ResponseEntity.ok()
                             .build();
    }

}
