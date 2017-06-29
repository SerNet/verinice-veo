/*******************************************************************************
 * Copyright (c) 2017 Urs Zeidler.
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
 *     Urs Zeidler uz<at>sernet.de - initial API and implementation
 ******************************************************************************/
package org.veo.web.bean;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.veo.client.schema.ModelSchemaRestClient;
import org.veo.persistence.ElementRepository;

/**
 * References the services for the jsf.
 * 
 * @author urszeidler
 *
 */
@Component()
@ManagedBean()
@ApplicationScoped
public class ApplicationBean {

    @Autowired
    private ElementRepository elementRepository;

    @Autowired
    private ModelSchemaRestClient schemaService;
    @Autowired
    private CacheService cacheService;

    public ModelSchemaRestClient getSchemaService() {
        return schemaService;
    }

    public CacheService getCacheService() {
        return cacheService;
    }

    public ElementRepository getElementRepository() {
        return elementRepository;
    }

}
