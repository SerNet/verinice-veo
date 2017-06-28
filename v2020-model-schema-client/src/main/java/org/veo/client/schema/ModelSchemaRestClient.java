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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.veo.client.AbstractRestClient;
import org.veo.schema.model.ElementDefinition;

/**
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
@Service
public class ModelSchemaRestClient extends AbstractRestClient {

    private static final Logger LOG = LoggerFactory.getLogger(ModelSchemaRestClient.class);

    public static final String SERVER_URL_DEFAULT = "http://localhost:8090/";
    public static final String PATH_DEFAULT = "/service/model-schema";

    private static final String NOT_IMPLEMENTED_MSG = "Method not implemented by this client. Use trackGameResult(GoalsOfAGameCollection goals) instead";

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

    public ElementDefinition[] getAllElementTypes() {
        StringBuilder sb = new StringBuilder(getBaseUrl());
        sb.append("allElementTypes");
        String url = sb.toString();
        if (LOG.isInfoEnabled()) {
            LOG.info("getAllElementTypes, URL: " + url);
        }
        ResponseEntity<ElementDefinition[]> responseEntity = getRestHandler().getForEntity(url,
                ElementDefinition[].class);
        return responseEntity.getBody();

    }

   

    /* (non-Javadoc)
     * @see de.sernet.fluke.client.rest.AbstractSecureRestClient#getPath()
     */
    @Override
    public String getPath() {
        return this.path;
    }

    /* (non-Javadoc)
     * @see de.sernet.fluke.client.rest.AbstractSecureRestClient#setPath(java.lang.String)
     */
    @Override
    public void setPath(String path) {
        this.path = path;
    }

   

}
