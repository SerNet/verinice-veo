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
package org.veo.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestOperations;

/**
 * Abstract base class for REST clients.
 *
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
public abstract class AbstractRestClient {

    @Value("${rest-server.url:http://localhost:8080}")
    private String serverUrl;

    private RestOperations restOperations;

    public AbstractRestClient() {
        super();
        restOperations = new RestClient();
    }

    public AbstractRestClient(String username, String password) {
        super();
        restOperations = new RestClient(username, password);
    }

    /**
     * Init the rest client with user credentials.
     *
     * If the server is secured, this is the only way for clients to talk with
     * the rest backend.
     *
     * @param username The login name of the user.
     * @param password The password of the user.
     *
     */
    public void initRestOperations(String username, String password) {
        restOperations = new RestClient(username, password);
    }

    protected String getBaseUrl() {
        StringBuilder sb = new StringBuilder(getServerUrl());
        if (!getServerUrl().endsWith("/") && !getPath().startsWith("/")) {
            sb.append("/");
        }
        sb.append(getPath());
        if (!getPath().endsWith("/")) {
            sb.append("/");
        }
        return sb.toString();
    }

    protected String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
    
    protected RestOperations getRestHandler() {
        return restOperations;
    }
    
    public void setRestHandler(RestOperations restHandler) {
        this.restOperations = restHandler;
    }

    public abstract String getPath();

    public abstract void setPath(String path);
    
}
