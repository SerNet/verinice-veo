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

import org.veo.service.LinkMapService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

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

    @Autowired
    private LinkMapService mapService;

    public LinksController() {
        super();
    }

    public LinksController(LinkMapService mapRepository) {
        this.mapService = mapRepository;
    }

    @GetMapping(value = "/links")
    public ResponseEntity<List<Map<String, Object>>> getLinks() {
        List<Map<String, Object>> result = mapService.findAll();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
    }

    @GetMapping(value = "/links/{" + UUID + ":.+}")
    public ResponseEntity<Map<String, Object>> getLink(@PathVariable(value = UUID) String uuid) {
        Map<String, Object> result = mapService.find(uuid);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
    }

    @PutMapping(value = "/links/{" + UUID + "}")
    public ResponseEntity<Object> getLinks(@PathVariable(value = UUID) String uuid,
            @RequestBody Map<String, Object> content) {
        this.mapService.save(uuid, content);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/links")
    public ResponseEntity<Object> postLink(@RequestBody Map<String, Object> content) {
        String uuid = this.mapService.saveNew(content);
        return ResponseEntity.created(URI.create("/links/" + uuid)).build();
    }

    @GetMapping(value = "/elements/{" + UUID + "}/links")
    public ResponseEntity<List<Map<String, Object>>> getLinks(@PathVariable(UUID) String uuid) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(mapService.findByElement(uuid));
    }

    @DeleteMapping(value = "/links/{" + UUID + "}")
    public ResponseEntity<Object> deleteLink(@PathVariable(UUID) String uuid) {
        mapService.delete(uuid);
        return ResponseEntity.ok().build();
    }

}
