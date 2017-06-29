package org.veo.modelschema.test;
/*******************************************************************************
 * Copyright (c) 2017 Sebastian Hagedorn.
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
 *     Sebastian Hagedorn sh (at) sernet.de - initial API and implementation
 ******************************************************************************/

import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.veo.schema.model.ElementDefinition;
import org.veo.schema.model.LinkDefinition;
import org.veo.service.ElementDefinitionFactory;

/**
 * @author sh
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ElementDefinitionFactory.class})
public class ElementFactoryTest {
    
    private final Logger logger = LoggerFactory.getLogger(ElementFactoryTest.class);
    public static final String TEST_ELEMENT_TYPE = "asset";
    
    @Test
    public void testParseElemenetDefiniton(){



        ElementDefinition asset = ElementDefinitionFactory.getInstance().getElementDefinition(TEST_ELEMENT_TYPE);
        assertTrue(asset instanceof ElementDefinition);
        assertTrue(asset.getElementType().equals(TEST_ELEMENT_TYPE));
        assertTrue(asset.getProperties().size() > 0);
    }
    
    @Test
    public void testInitLinkDefinitionMap(){
        assertTrue(ElementDefinitionFactory.getInstance().getLinkDefinitionsByElementType(TEST_ELEMENT_TYPE).size() == 1);
    }    
    
    @Test
    public void testLinkModel(){
        for (LinkDefinition linkDefinition : ElementDefinitionFactory.getInstance().getLinkDefinitionsByElementType(TEST_ELEMENT_TYPE)){
            assertTrue(linkDefinition.getProperties().size() == 3);
            assertTrue(linkDefinition.getDestinationType().equals("control"));
        }
    }
    
    @Test
    public void testInitElementDefinitionMap(){
        Map<String, ElementDefinition> map = ElementDefinitionFactory.getInstance().getElementDefinitions(); 
        assertTrue(map.size() == 2);
    }
    
    @Test
    public void testGetElementDefinition(){
        Map<String, ElementDefinition> map = ElementDefinitionFactory.getInstance().getElementDefinitions();
        assertTrue(map.containsKey(TEST_ELEMENT_TYPE));
    }
}
