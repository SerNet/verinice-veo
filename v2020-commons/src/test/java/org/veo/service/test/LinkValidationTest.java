/**
 * 
 */
package org.veo.service.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.veo.json.JsonValidator;
import org.veo.json.ValidationResult;
import org.veo.model.Element;
import org.veo.model.Link;
import org.veo.service.ElementFactory;
import org.veo.service.JsonFactory;
import org.veo.service.LinkFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

/**
 * @author urszeidler
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { TestConfig.class })
public class LinkValidationTest {

	private static final Logger logger = LoggerFactory.getLogger(LinkValidationTest.class.getName());
	
	@Autowired
	JsonFactory jsonFactory;

	@Autowired
	LinkFactory linkFactory;

    @Autowired
    ElementFactory elementFactory;

    private JsonValidator linkSchemaValidator;

    private ObjectMapper mapper;

    @Before
    public void setup() throws IOException, ProcessingException {
        linkSchemaValidator = new JsonValidator(this.getClass().getClassLoader().getResourceAsStream("schemas/asset_control.json"));
        mapper = new ObjectMapper();
    }
    
    @Test
    public void testValidationLinkSuccesful() throws ProcessingException, IOException {
        Link link = createLinkAssetControl();

        Map<String,Object> linkJsonMap = jsonFactory.createJson(link);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNodeAsset = mapper.valueToTree(linkJsonMap);
        ValidationResult validationResult = linkSchemaValidator.validate(jsonNodeAsset);
        
        logger.debug(validationResult.toString());
        assertTrue("Should not be invalid.", validationResult.isSuccessful());
    }
    
    @Test
    public void testValidationLink_No_Source() throws ProcessingException, IOException {
        Link link = createLinkAssetControl();

        Map<String,Object> linkJsonMap = jsonFactory.createJson(link);
        linkJsonMap.remove("source");

        JsonNode jsonNodeAsset = mapper.valueToTree(linkJsonMap);
        ValidationResult validationResult = linkSchemaValidator.validate(jsonNodeAsset);
        
        logger.debug(validationResult.toString());
        assertFalse("Should be invalid as no source is set.", validationResult.isSuccessful());
    }
    
    @Test
    public void testValidationLink_No_Target() throws ProcessingException, IOException {
        Link link = createLinkAssetControl();

        Map<String,Object> linkJsonMap = jsonFactory.createJson(link);
        linkJsonMap.remove("target");
        
        JsonNode jsonNodeAsset = mapper.valueToTree(linkJsonMap);
        ValidationResult validationResult = linkSchemaValidator.validate(jsonNodeAsset);
        
        logger.debug(validationResult.toString());
        assertFalse("Should be invalid as no target is set.", validationResult.isSuccessful());
    }

    @Test
    public void testValidationLink_No_Type() throws ProcessingException, IOException {
        Link link = createLinkAssetControl();

        Map<String,Object> linkJsonMap = jsonFactory.createJson(link);
        linkJsonMap.remove("$veo.type");
        
        JsonNode jsonNodeAsset = mapper.valueToTree(linkJsonMap);
        ValidationResult validationResult = linkSchemaValidator.validate(jsonNodeAsset);
        
        logger.debug(validationResult.toString());
        assertFalse("Should be invalid as no target is set.", validationResult.isSuccessful());
    }

	private Link createLinkAssetControl() {
		Element asset = new Element();
		asset.setTypeId("asset");
        Element control = new Element();
        control.setTypeId("control");
        
        Link link = new Link();
        link.setSource(asset);
        link.setDestination(control);
        
        link.setTypeId("asset_control");
		return link;
	}

}
