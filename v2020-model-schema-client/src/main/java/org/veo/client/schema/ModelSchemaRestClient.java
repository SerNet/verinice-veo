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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.veo.client.AbstractRestClient;
import org.veo.schema.model.ElementDefinition;
import org.veo.schema.model.LinkDefinition;
import org.veo.schema.rest.ModelSchemaRestService;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Client for the model schema REST web service.
 * 
 * Set the REST service URL in file: application.properties
 * rest-server.url=http[s]://<HOSTNAME>[:<PORT>]/
 * 
 * @author Daniel Murygin
 */
@Service
public class ModelSchemaRestClient extends AbstractRestClient {

    private static final Logger log = LoggerFactory.getLogger(ModelSchemaRestClient.class);

    @Value("${model-schema.rest.url:'http://localhost:8090'}")
    private String modelSchemaServerUrl;
    
    @Value("${model-schema.rest.path:/service/model-schema}")
    private String path;
    
    public ModelSchemaRestClient() {
    }

    public ModelSchemaRestClient(String username, String password) {
        super(username, password);
    }
    
    @PostConstruct
    public void init() {
        setServerUrl(modelSchemaServerUrl);
        setPath(path);
    }

    public List<ElementDefinition> getElementTypes() {
        StringBuilder sb = new StringBuilder(getBaseUrl());
        sb.append("allElementTypes");
        String url = sb.toString();
        log.info("getAllElementTypes, URL: {}", url);
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
        log.info("getElementType, URL: {}", url);
        return getRestHandler().getForObject(url, ElementDefinition .class);
    }
    
    public List<LinkDefinition> getLinkDefinitions(String type) {
        StringBuilder sb = new StringBuilder(getBaseUrl());
        sb.append("linkDefinitions/");
        sb.append(type);
        String url = sb.toString();
        log.info("getLinkDefinitions, URL: {}", url);
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
