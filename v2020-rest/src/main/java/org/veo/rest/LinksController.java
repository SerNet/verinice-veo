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
 *
 * Contributors:
 *     Jochen Kemnade <jk@sernet.de> - initial API and implementation
 *     Alexander Ben Nasrallah
 *     Urs Zeidler
 ******************************************************************************/
package org.veo.rest;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.veo.service.LinkMapService;

/**
 * REST service which provides methods to manage links.
 *
 * An link is a generic Map<String, Object> with just a few requirements,
 * see org.veo.service.LinkMapService.
 */
@RestController
public class LinksController {

    @Autowired
    private LinkMapService mapService;

    public LinksController() {
        super();
    }

    public LinksController(LinkMapService mapRepository) {
        this.mapService = mapRepository;
    }

    @RequestMapping(path = "/links", method = RequestMethod.GET)
    public ResponseEntity<List<Map<String, Object>>> getLinks() throws IOException {
        List<Map<String, Object>> result = mapService.findAll();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(result);
    }

    @RequestMapping(path = "/links/{uuid:.+}" /* at least on char to distinguish form get all */, method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getLink(@PathVariable(value = "uuid") String uuid)
            throws IOException {
        Map<String, Object> result = mapService.find(uuid);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(result);
    }

    @RequestMapping(path = "/links/{uuid}", method = RequestMethod.PUT)
    public ResponseEntity<Resource> getLinks(@PathVariable(value = "uuid") String uuid,
            @RequestBody Map<String, Object> content) throws IOException {
        this.mapService.save(uuid, content);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(path = "/links", method = RequestMethod.POST)
    public ResponseEntity<Resource> createLink(@RequestBody Map<String, Object> content)
            throws IOException {
        String uuid = this.mapService.saveNew(content);
        return ResponseEntity.created(URI.create("/links/" + uuid)).build();
    }

    @RequestMapping(value = "/elements/{uuid}/links", method = RequestMethod.GET)
    public ResponseEntity<List<Map<String, Object>>> getLinks(@PathVariable("uuid") String uuid)
            throws IOException {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(mapService.findByElement(uuid));
    }

    @RequestMapping(value = "/links/{uuid}", method = RequestMethod.DELETE)
    public ResponseEntity<Resource> deleteLink(@PathVariable("uuid") String uuid)
            throws IOException {
        mapService.delete(uuid);
        return ResponseEntity.ok().build();
    }

}
