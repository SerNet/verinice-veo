/*******************************************************************************
 * Copyright (c) 2017 Daniel Murygin.
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
 *     Daniel Murygin dm[at]sernet[dot]de - initial API and implementation
 ******************************************************************************/
package org.veo.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.veo.model.Element;
import org.veo.service.ElementService;

/**
 * REST service which provides methods to manage elements. Use Spring Data REST
 * service for simple CRUD operations.
 *
 * @author Daniel Murygin dm[at]sernet[dot]de
 */
@RestController
@RequestMapping("/element-service")
public class ElementServiceController {

    @Autowired
    ElementService elementService;

    @RequestMapping("/elements")
    public List<Element> loadAll(@RequestParam(value = "type") String typeId) {
        return elementService.loadAll(typeId);
    }
}
