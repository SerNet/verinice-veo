/*******************************************************************************
 * Copyright (c) 2017 Daniel Murygin
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
 *     SDaniel Murygin <dm[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package org.veo.client.schema;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.veo.client.AbstractRestClient;
import org.veo.schema.model.ElementDefinition;
import org.veo.schema.model.LinkDefinition;

/**
 * Client for the model schema REST web service.
 * 
 * Set the REST service URL in file: application.properties
 * 
 * rest-server.url=http[s]://<HOSTNAME>[:<PORT>]/
 * 
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
@Service
public class ModelSchemaRestClient extends AbstractRestClient {

    private static final Logger log = LoggerFactory.getLogger(ModelSchemaRestClient.class);

    public static final String SERVER_URL_DEFAULT = "http://localhost:8090/";
    public static final String PATH_DEFAULT = "/service/model-schema";

    private String path;

    public ModelSchemaRestClient() {
        path = PATH_DEFAULT;
    }

    public ModelSchemaRestClient(String username, String password) {
        this(username, password, SERVER_URL_DEFAULT, PATH_DEFAULT);
    }

    public ModelSchemaRestClient(String username, String password, String serverUrl, String path) {
        super(username, password);
        setServerUrl(serverUrl);
        setPath(path);
    }

    public List<ElementDefinition> getElementTypes() {
        StringBuilder sb = new StringBuilder(getBaseUrl());
        sb.append("allElementTypes");
        String url = sb.toString();
        if (log.isInfoEnabled()) {
            log.info("getAllElementTypes, URL: " + url);
        }
        ResponseEntity<List<ElementDefinition>> response = getRestHandler().exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<ElementDefinition>>() {
                });
        return response.getBody();

    }
    
    public ElementDefinition getElementType(String type) {
        StringBuilder sb = new StringBuilder(getBaseUrl());
        sb.append("elementType/");
        sb.append(type);
        String url = sb.toString();
        if (log.isInfoEnabled()) {
            log.info("getElementType, URL: " + url);
        }
        return getRestHandler().getForObject(url, ElementDefinition .class);
    }
    
    public List<LinkDefinition> getLinkDefinitions(String type) {
        StringBuilder sb = new StringBuilder(getBaseUrl());
        sb.append("linkDefinitions/");
        sb.append(type);
        String url = sb.toString();
        if (log.isInfoEnabled()) {
            log.info("getLinkDefinitions, URL: " + url);
        }
        ResponseEntity<List<LinkDefinition>> response = getRestHandler().exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<LinkDefinition>>() {
                });
        return response.getBody();

    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

}
