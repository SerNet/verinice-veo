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

import net._01001111.text.LoremIpsum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.veo.model.Element;
import org.veo.model.ElementProperty;
import org.veo.model.Link;
import org.veo.model.LinkProperty;
import org.veo.persistence.ElementRepository;
import org.veo.persistence.LinkRepository;
import org.veo.util.time.TimeFormatter;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * By default this test runs with in memory database h2
 * without any configuration.
 * Add this annotation:
 * @AutoConfigureTestDatabase(replace=Replace.NONE)
 * to run this test with a database configured in 
 * src/test/resources/application.properties
 * 
 * See: 
 * https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-testing.html
 * 
 * @author Daniel Murygin
 */
@RunWith(SpringRunner.class)
@DataJpaTest(showSql=false)
// @Transactional(propagation = Propagation.NOT_SUPPORTED)   
public class ElementRepositoryTest {
    
    private final Logger logger = LoggerFactory.getLogger(ElementRepositoryTest.class.getName());
    
    @Autowired
    private ElementRepository elementRepository;
    
    @Autowired
    private LinkRepository linkRepository;
   
    @PersistenceContext
    protected EntityManager entityManager;
    
    private LoremIpsum loremIpsum = new LoremIpsum();
    
    private Random random = new Random();

    @Before
    public void init() {
        // empty         
    }
    
    @Test
    public void testSaveAndFindOne() { 
        Element element = createElement("org");
        elementRepository.save(element);
        Element elementResult = elementRepository.findByUuid(element.getUuid());
        assertNotNull(elementResult);
        assertEquals(4,elementResult.getProperties().size());
    }
    
    @Test
    public void testSaveAndFindWithChildren() { 
        Element element = createElement("org");
        Element child = createElement("asset_group");
        child.setParent(element);
        element.addChild(child);
        element = elementRepository.save(element);
        
        Element elementResult = elementRepository.findOneWithChildren(element.getUuid());
        assertNotNull(elementResult);
        assertEquals(4, elementResult.getProperties().size());
        assertEquals(1, elementResult.getChildren().size());
    }
    
    @Test
    public void testSaveAndFindLinkWithNewElement() {
        Element element = createElement("org");
        Element linkedElement = createElement("person"); 
        Link link = new Link();
        link.setSource(element);
        link.setDestination(linkedElement);
        link.setTypeId("");
        element.addLinkOutgoing(link);
        elementRepository.save(element);
        
        Element elementResult = elementRepository.findOneWithLinks(element.getUuid());
        assertNotNull(elementResult);
        assertEquals(4, elementResult.getProperties().size());
        assertEquals(1, elementResult.getLinksOutgoing().size());  
        
        assertEquals(1, elementResult.getLinksOutgoing().size());  
        assertEquals(1, elementResult.getLinkedDestinations().size());
    }
    
    @Test
    public void testFindOneWithLinks() { 
        Element element = createElement("org");
        element = elementRepository.save(element);
        
        Element linkedElement = createElement("person");
        linkedElement = elementRepository.save(linkedElement);
        
        Link link = new Link();
        link.setSource(element);
        link.setDestination(linkedElement);
        link.setTypeId("");
        LinkProperty number = new LinkProperty();
        number.setKey(UUID.randomUUID().toString());
        number.setValue(String.valueOf(23));
        link.addProperty(number);
        link = linkRepository.save(link);
        
        simulateNewTransaction();
        
        Element elementResult = elementRepository.findOneWithLinks(element.getUuid());
        assertNotNull(elementResult);
        assertEquals(4, elementResult.getProperties().size());
        assertEquals(1, elementResult.getLinksOutgoing().size());  
        
        assertEquals(1, elementResult.getLinksOutgoing().iterator().next().getProperties().size());  
        assertEquals(1, elementResult.getLinkedDestinations().size());
    }
    
    @Test
    public void testSaveAndFindOneWithAll() { 
        Element element = createElement("org");
        elementRepository.save(element);
        Element elementResult = elementRepository.findOneWithAll(element.getUuid());
        assertNotNull(elementResult);
        assertEquals(4,elementResult.getProperties().size());
    }
    
    @Test
    public void testFindByTypeId() {
        for (int i = 0; i < 10; i++) {
            Element element = createElement("testFindByTypeId");
            element = elementRepository.save(element);
        }
        List<Element> elementList = elementRepository.findByTypeId("testFindByTypeId");
        assertNotNull(elementList);
        assertEquals(10, elementList.size());
    }
    
    @Test
    public void testChangeParent() {
        Element parent = createElement("org");
        Element child = createElement("asset_group");
        child.setParent(parent);
        parent.addChild(child);
        elementRepository.save(parent);
        
        Element elementResult = elementRepository.findOneWithLinks(child.getUuid());
        assertNotNull(elementResult);
        assertTrue(parent.getUuid().equals(elementResult.getParent().getUuid()));
        
        Element newParent = createElement("org");
        newParent = elementRepository.save(newParent);
        elementResult.setParent(newParent);
        elementRepository.save(elementResult);
        
        elementResult = elementRepository.findOneWithLinks(child.getUuid());
        assertNotNull(elementResult);
        assertTrue(newParent.getUuid().equals(elementResult.getParent().getUuid()));
    }
    
    @Test
    public void testBigTree() {
        Element element = createElement("org");
        elementRepository.save(element);
        int maxDepth = random.nextInt(2)+1;
        logger.debug("Creating tree, depth is: " + maxDepth + "...");
        createChildren(element, maxDepth, 0);
    }
    
    @Test
    public void testFindAll() {
        long start = System.currentTimeMillis();
        Iterable<Element> result = elementRepository.findAll();
        String time = TimeFormatter.getHumanRedableTime(System.currentTimeMillis() - start);
        logger.debug("FindAll runtime: " + time + " (" + size(result) + ")");
    }
    
    @Test
    public void testFindAllWithProperties() {
        long start = System.currentTimeMillis();
        Iterable<Element> result = elementRepository.findAllWithProperties();
        String time = TimeFormatter.getHumanRedableTime(System.currentTimeMillis() - start);
        logger.debug("FindAllWithProperties runtime: " + time + " (" + size(result) + ")");
    }

    private long size(Iterable<Element> result) {
        Iterator<Element> it = result.iterator();
        long n = 0;
        while (it.hasNext()) {
          it.next();
          n++;
        }
        return n;
    }
    
    private Element createElement(String typeId) {
        Element element = new Element();
        element.setTypeId(typeId);
        element.setTitle("ElementRepositoryTest");
        
        ElementProperty date = new ElementProperty();
        date.setKey("date");
        date.setValue(Calendar.getInstance().getTime().toString());
        element.addProperty(date);
        
        ElementProperty label = new ElementProperty();
        label.setKey("label");
        label.setValue(loremIpsum.words(random.nextInt(4)+1));
        element.addProperty(label);
        
        ElementProperty text = new ElementProperty();
        text.setKey("text");
        text.setValue(loremIpsum.paragraphs(random.nextInt(4)+1));
        element.addProperty(text);
        
        ElementProperty number = new ElementProperty();
        number.setKey("number");
        number.setValue(String.valueOf(random.nextInt(10000)+1));
        element.addProperty(number);
        
        return element;
    }
    
    private void createChildren(Element parent, int maxDepth, int depth) {
        if(depth>maxDepth) {
            return;
        }
        int number = random.nextInt(10)+1;
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
    
    /**
     * Simulates new transaction (empties Entity Manager cache).
     */
    public void simulateNewTransaction() {
        if(entityManager.isJoinedToTransaction()) {
            entityManager.flush();
            entityManager.clear();
        }
    }
}
