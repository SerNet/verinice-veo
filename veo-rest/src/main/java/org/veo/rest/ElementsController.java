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

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.veo.model.HistoryEntry;
import org.veo.service.ElementMapService;
import org.veo.service.HistoryService;

/**
 * REST service which provides methods to manage elements.
 *
 * An element is a generic Map<String, Object> with just a few requirements, see
 * org.veo.service.ElementMapService.
 */
@RestController
@SecurityRequirement(name = RestApplication.SECURITY_SCHEME_BEARER_AUTH)
public class ElementsController {

    public static final String UUID_PARAM = "uuid";
    public static final String PARENT_PARAM = "parent";

    private static final String UUID_DESCRIPTION = "UUID of the element";
    private static final String UUID_DEFINITION = "This is the normalized UUID representation:\n"
            + "* a block of 8 HEX chars followed by\n* 3 blocks of 4 HEX chars followed by\n"
            + "* a block of 12 HEX chars.";
    private static final String UUID_EXAMPLE = "f35b982c-8ad4-4515-96ee-df5fdd4247b9";
    // @formatter:off
    private static final String ELEMENT_DATA_EXAMPLE = "{\n"
            + "    \"id\": \"f35b982c-8ad4-4515-96ee-df5fdd4247b9\",\n"
            + "    \"type\": \"asset\",\n"
            + "    \"title\": \"Asset\",\n"
            + "    \"parent\": \"f35b982c-96ee-4515-8ad4-a15fdd4245a4\",\n"
            + "    \"description\": \"This is an asset.\",\n"
            + "    \"role\": [\"role-a\",\"role-b\"],\n"
            + "    \"business-value\": [1,3,5],\n"
            + "    \"asset-type\": \"physical\"\n"
            + "}";
    // @formatter:on

    @Autowired
    private ElementMapService mapService;

    @Autowired
    private HistoryService historyService;

    public ElementsController() {
    }

    public ElementsController(ElementMapService mapService, HistoryService historyService) {
        this.mapService = mapService;
        this.historyService = historyService;
    }

    @GetMapping(value = "/elements")
    @Operation(summary = "Loads all elements")
    public ResponseEntity<List<Map<String, Object>>> getElements(
            @Parameter(name = "parentUUID",
                       description = "UUID of the parent element\n\n" + UUID_DEFINITION,
                       example = UUID_EXAMPLE,
                       required = false) @RequestParam(value = PARENT_PARAM,
                                                       required = false) String parentUuid) {
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON_UTF8)
                             .body(getElementList(parentUuid));
    }

    @PostMapping(value = "/elements")
    @Operation(summary = "Creates an element")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Element data",
                                                          required = true,
                                                          content = @io.swagger.v3.oas.annotations.media.Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                                                                                 schema = @io.swagger.v3.oas.annotations.media.Schema(example = ELEMENT_DATA_EXAMPLE)))
    public ResponseEntity<Resource> createElements(@RequestBody Map<String, Object> content) {
        String uuid = this.mapService.saveNew(content);
        return ResponseEntity.created(URI.create("/elements/" + uuid))
                             .build();
    }

    @GetMapping(value = "/elements/{" + UUID_PARAM + ":.+}")
    @Operation(summary = "Loads an element")
    public ResponseEntity<Map<String, Object>> getElement(
            @Parameter(name = "UUID",
                       description = UUID_DESCRIPTION + "\n\n" + UUID_DEFINITION,
                       example = UUID_EXAMPLE,
                       required = true) @PathVariable(UUID_PARAM) String uuid) {
        Map<String, Object> map = mapService.find(uuid);
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON_UTF8)
                             .body(map);
    }

    @GetMapping(value = "/elements/{" + UUID_PARAM + ":.+}/children")
    @Operation(summary = "Loads the children of an element")
    public ResponseEntity<List<Map<String, Object>>> getChildren(
            @Parameter(name = "UUID",
                       description = UUID_DESCRIPTION + "\n\n" + UUID_DEFINITION,
                       example = UUID_EXAMPLE,
                       required = true) @PathVariable(UUID_PARAM) String uuid) {
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON_UTF8)
                             .body(mapService.findChildren(uuid));
    }

    @GetMapping(value = "/elements/{" + UUID_PARAM + ":.+}/history")
    @Operation(summary = "Loads the history of an element")
    public ResponseEntity<List<HistoryEntry>> getElementHistory(
            @Parameter(name = "UUID",
                       description = UUID_DESCRIPTION + "\n\n" + UUID_DEFINITION,
                       example = UUID_EXAMPLE,
                       required = true) @PathVariable(UUID_PARAM) String uuid) {
        List<HistoryEntry> history = historyService.getHistory(uuid);
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON_UTF8)
                             .body(history);
    }

    @PutMapping(value = "/elements/{" + UUID_PARAM + "}")
    @Operation(summary = "Updates an element")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Element data",
                                                          required = true,
                                                          content = @io.swagger.v3.oas.annotations.media.Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                                                                                 schema = @io.swagger.v3.oas.annotations.media.Schema(example = ELEMENT_DATA_EXAMPLE)))
    public ResponseEntity<Resource> updateElement(
            @Parameter(name = "UUID",
                       description = UUID_DESCRIPTION + "\n\n" + UUID_DEFINITION,
                       example = UUID_EXAMPLE,
                       required = true) @PathVariable(UUID_PARAM) String uuid,
            @RequestBody Map<String, Object> content) {
        mapService.save(uuid, content);
        return ResponseEntity.noContent()
                             .build();
    }

    @DeleteMapping(value = "/elements/{" + UUID_PARAM + "}")
    @Operation(summary = "Deletes an element")
    public ResponseEntity<Resource> deleteElement(
            @Parameter(name = "UUID",
                       description = UUID_DESCRIPTION + "\n\n" + UUID_DEFINITION,
                       example = UUID_EXAMPLE,
                       required = true) @PathVariable(UUID_PARAM) String uuid) {
        mapService.delete(uuid);
        return ResponseEntity.ok()
                             .build();
    }

    private List<Map<String, Object>> getElementList(String parentUuid) {
        if (parentUuid == null) {
            return mapService.findAll();
        }
        if (parentUuid.isEmpty()) {
            return mapService.findRootElements();
        }
        return mapService.findChildren(parentUuid);
    }

}
