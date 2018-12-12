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

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
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
public class ElementsController {
    private static final Logger logger = LoggerFactory.getLogger(ElementsController.class);

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

    @RequestMapping(value = "/elements", method = RequestMethod.GET)
    public ResponseEntity<List<Map<String, Object>>> getElements() throws IOException {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(mapService.findAll());
    }

    @RequestMapping(value = "/elements", method = RequestMethod.POST)
    public ResponseEntity<Resource> createElements(@RequestBody Map<String, Object> content)
            throws IOException {
        String uuid = this.mapService.saveNew(content);
        return ResponseEntity.created(URI.create("/elements/" + uuid)).build();
    }

    @RequestMapping(value = "/elements/{uuid:.+}" /*
                                                   * at least on char to
                                                   * distinguish from get all
                                                   */, method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getElement(@PathVariable("uuid") String uuid) {
        Map<String, Object> map = mapService.find(uuid);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(map);
    }

    @RequestMapping(value = "/elements/{uuid:.+}/children" /*
                                                            * at least on char
                                                            * to distinguish
                                                            * from get all
                                                            */, method = RequestMethod.GET)
    public ResponseEntity<List<Map<String, Object>>> getChildren(
            @PathVariable("uuid") String uuid) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(mapService.findChildren(uuid));
    }

    @RequestMapping(value = "/elements/{uuid:.+}/history", method = RequestMethod.GET)
    public ResponseEntity<List<HistoryEntry>> getElementHistory(@PathVariable("uuid") String uuid) {
        List<HistoryEntry> history = historyService.getHistory(uuid);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON_UTF8).body(history);
    }

    @RequestMapping(value = "/elements/{uuid}", method = RequestMethod.PUT)
    public ResponseEntity<Resource> updateElement(@PathVariable("uuid") String uuid,
            @RequestBody Map<String, Object> content) {
        mapService.save(uuid, content);
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(value = "/elements/{uuid}", method = RequestMethod.DELETE)
    public ResponseEntity<Resource> deleteElement(@PathVariable("uuid") String uuid) {
        mapService.delete(uuid);
        return ResponseEntity.ok().build();
    }

}
