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
package org.veo.client.schema.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.veo.client.ClientApplication;
import org.veo.client.schema.ModelSchemaRestClient;
import org.veo.schema.rest.ElementDefinitionResource;
import org.veo.schema.rest.LinkDefinitionResource;

/**
 * @author Sebastian Hagedorn <sh[at]sernet[dot]de>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { ClientApplication.class })
@Ignore
public class ModelSchemaRestClientTest {

    @Autowired
    ModelSchemaRestClient schemaRestClient;

    @Before
    public void init() {
        schemaRestClient.setServerUrl("http://localhost:8090");
    }

    @Test
    public void testGetElementTypes() {
        Collection<ElementDefinitionResource> elementDefinitions = schemaRestClient.getElementTypes();
        assertNotNull(elementDefinitions);
        assertTrue(elementDefinitions.size() > 0);
    }
    
    @Test
    public void testGetElementType() {
        ElementDefinitionResource elementDefinition = schemaRestClient.getElementType("asset");
        assertNotNull(elementDefinition);
        elementDefinition = schemaRestClient.getElementType("control");
        assertNotNull(elementDefinition);
        
    }
    
    @Test
    public void testGetLinkDefinitions() {
        Collection<LinkDefinitionResource> linkDefinitions = schemaRestClient.getLinkDefinitions("asset");
        assertNotNull(linkDefinitions);
        assertTrue(linkDefinitions.size() > 0);
        linkDefinitions = schemaRestClient.getLinkDefinitions("asset");
        assertNotNull(linkDefinitions);
        assertTrue(linkDefinitions.size() > 0);
    }
    
}
