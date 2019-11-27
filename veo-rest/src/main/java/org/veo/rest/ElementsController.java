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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

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

import org.veo.model.HistoryEntry;
import org.veo.service.ElementMapService;
import org.veo.service.HistoryService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
/**
 * REST service which provides methods to manage elements.
 *
 * An element is a generic Map<String, Object> with just a few requirements, see
 * org.veo.service.ElementMapService.
 */
@RestController
@SecurityRequirement(name = RestApplication.SECURITY_SCHEME_BEARER_AUTH)
public class ElementsController {

    public static final String PARENT_PARAM = "parent";

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
    public ResponseEntity<List<Map<String, Object>>> getElements(
            @RequestParam(value = PARENT_PARAM, required = false) String parentUuid) {
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON_UTF8)
                             .body(getElementList(parentUuid));
    }

    @PostMapping(value = "/elements")
    public ResponseEntity<Resource> createElements(@RequestBody Map<String, Object> content) {
        String uuid = this.mapService.saveNew(content);
        return ResponseEntity.created(URI.create("/elements/" + uuid))
                             .build();
    }

    @GetMapping(value = "/elements/{uuid:.+}")
    public ResponseEntity<Map<String, Object>> getElement(
            @PathVariable("uuid") @NotBlank @Size(min = 1, max = 50) String uuid) {
        Map<String, Object> map = mapService.find(uuid);
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON_UTF8)
                             .body(map);
    }

    @GetMapping(value = "/elements/{uuid:.+}/children")
    public ResponseEntity<List<Map<String, Object>>> getChildren(
            @PathVariable("uuid") String uuid) {
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON_UTF8)
                             .body(mapService.findChildren(uuid));
    }

    @GetMapping(value = "/elements/{uuid:.+}/history")
    public ResponseEntity<List<HistoryEntry>> getElementHistory(@PathVariable("uuid") String uuid) {
        List<HistoryEntry> history = historyService.getHistory(uuid);
        return ResponseEntity.ok()
                             .contentType(MediaType.APPLICATION_JSON_UTF8)
                             .body(history);
    }

    @PutMapping(value = "/elements/{uuid}")
    public ResponseEntity<Resource> updateElement(@PathVariable("uuid") String uuid,
            @RequestBody Map<String, Object> content) {
        mapService.save(uuid, content);
        return ResponseEntity.noContent()
                             .build();
    }

    @DeleteMapping(value = "/elements/{uuid}")
    public ResponseEntity<Resource> deleteElement(@PathVariable("uuid") String uuid) {
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
