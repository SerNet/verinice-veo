package org.veo.client.schema.test;
/*
 * Copyright 2016 SerNet Service Network GmbH.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.veo.client.ClientApplication;
import org.veo.client.schema.ModelSchemaRestClient;
import org.veo.schema.model.ElementDefinition;

/**
 * @author Sebastian Hagedorn <sh[at]sernet[dot]de>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { ClientApplication.class })
public class ModelSchemaRestClientTest {
    
    @Autowired 
    ModelSchemaRestClient schemaRestClient;
  
    @Before
    public void init() {
        schemaRestClient.setServerUrl("http://localhost:8090");
    }
    
    @Test
    public void test() {
        ElementDefinition[] elementDefinitions = schemaRestClient.getAllElementTypes();
        assertNotNull(elementDefinitions);
        assertTrue(elementDefinitions.length>0);
    }

}
