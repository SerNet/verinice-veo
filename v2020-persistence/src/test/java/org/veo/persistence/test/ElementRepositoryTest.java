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
package org.veo.persistence.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.veo.model.Element;
import org.veo.model.ElementProperty;
import org.veo.model.Link;
import org.veo.model.LinkProperty;
import org.veo.persistence.ElementRepository;
import org.veo.persistence.LinkRepository;

import net._01001111.text.LoremIpsum;

/**
 *
 * By default this test runs with in memory database h2
 * without any configuration.
 * Add this annotation:
 * @AutoConfigureTestDatabase(replace=Replace.NONE)
 * to run this test with a database configured in 
 * src/test/resources/application.properties
 * 
 * See: 
 * https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-testing.html
 * @author Daniel Murygin
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ElementRepositoryTest {
    
    private final Logger logger = LoggerFactory.getLogger(ElementRepositoryTest.class.getName());
    
    @Autowired
    private ElementRepository elementRepository;
    
    private Element parent;
    
    private LoremIpsum loremIpsum = new LoremIpsum();
    
    @Before
    public void init() {
        parent = createElement("org");
        Element child = createElement("asset_group");
        parent.addChild(child);      
        Element linkedElement = createElement("person");
        Link link = new Link();
        link.setSource(parent);
        link.setDestination(linkedElement);
        LinkProperty number = new LinkProperty();
        number.setTypeId(UUID.randomUUID().toString());
        number.setNumber((long) 23);
        link.addProperty(number);
        parent.addLinkOutgoing(link);
        elementRepository.save(parent);
    }

    private Element createElement(String typeId) {
        Element element = new Element();
        element.setTypeId(typeId);
        element.setTitle("ElementRepositoryTest");
        
        ElementProperty date = new ElementProperty();
        date.setTypeId("date");
        date.setDate(Calendar.getInstance().getTime());
        element.addProperty(date);
        
        ElementProperty label = new ElementProperty();
        label.setTypeId("label");
        label.setLabel(loremIpsum.words(RandomUtils.nextInt(4)+1));
        element.addProperty(label);
        
        ElementProperty text = new ElementProperty();
        text.setTypeId("text");
        text.setText(loremIpsum.paragraphs(RandomUtils.nextInt(4)+1));
        element.addProperty(text);
        
        ElementProperty number = new ElementProperty();
        number.setTypeId("number");
        number.setNumber((long) (RandomUtils.nextInt(10000)+1));
        element.addProperty(number);
        
        return element;
    }
    
    @Test
    public void testFindOne() { 
        Element elementResult = elementRepository.findOne(parent.getUuid());
        assertNotNull(elementResult);
        assertEquals(4,elementResult.getProperties().size());
    }
    
    @Test
    public void testFindOneWithChildren() { 
        Element elementResult = elementRepository.findOneWithChildren(parent.getUuid());
        assertNotNull(elementResult);
        assertEquals(4, elementResult.getProperties().size());
        assertEquals(1, elementResult.getChildren().size());
    }
    
    @Test
    public void testFindOneWithLinks() { 
        Element elementResult = elementRepository.findOneWithLinks(parent.getUuid());
        assertNotNull(elementResult);
        assertEquals(4, elementResult.getProperties().size());
        assertEquals(1, elementResult.getLinksOutgoing().size());  
        
        assertEquals(1, elementResult.getLinksOutgoing().iterator().next().getProperties().size());  
        assertEquals(1, elementResult.getLinkedDestinations().size());
    }
    
    @Test
    public void testBigTree() {
        int maxDepth = RandomUtils.nextInt(2)+1;
        logger.debug("Creating tree, depth is: " + maxDepth + "...");
        createChildren(parent, maxDepth, 0);
    }
    
    private void createChildren(Element parent, int maxDepth, int depth) {
        if(depth>maxDepth) {
            return;
        }
        int number = RandomUtils.nextInt(10)+1;
        logger.debug("Depth: " + depth + ", creating " + (number + 1) + " childs...");
        for (int i = 0; i < number; i++) {
            Element child = createElement(loremIpsum.randomWord());
            parent.addChild(child);   
        }
        elementRepository.save(parent);
        depth++;
        for (Element child : parent.getChildren()) {
            createChildren(child, maxDepth, depth);
        }
    }
}
