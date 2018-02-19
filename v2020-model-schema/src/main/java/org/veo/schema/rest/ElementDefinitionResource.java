package org.veo.schema.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.ResourceSupport;
import org.veo.schema.model.ElementDefinition;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;


public class ElementDefinitionResource extends ResourceSupport {

    private ElementDefinition elementDefinition;

    @JsonCreator
    public ElementDefinitionResource(@JsonProperty("content") ElementDefinition elementDefinition) {
        this.elementDefinition = elementDefinition;
        final String type = elementDefinition.getElementType();
        add(linkTo(ModelSchemaRestService.class).withRel("people"));
    }

    public ElementDefinition getElementDefinition() {
        return elementDefinition;
    }

    public void setElementDefinition(ElementDefinition elementDefinition) {
        this.elementDefinition = elementDefinition;
    }
}