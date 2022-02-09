/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Finn Westendorf
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.sernet.gendocs


import spock.lang.Specification

class GenDocsSpec extends Specification {

    def "dsgvo doesn't throw exception"() {
        when:
        App.buildModel("../domaintemplates/dsgvo/")
        then:
        noExceptionThrown()
    }

    def "aspects are parsed correctly"() {
        given:
        def parser = new TypeParser("../domaintemplates/dsgvo/types/process/" as File)
        def schema = parser.parseAspect(new File("../domaintemplates/dsgvo/types/process/customAspects/process_accessAuthorization.json"))
        def expected = new CustomAspect()

        when:
        expected.key = "process_accessAuthorization"
        expected.attributes = List.of(new Attribute("process_accessAuthorization_concept","Authorisation concept in place?","Berechtigungskonzept vorhanden?","boolean"),
                new Attribute("process_accessAuthorization_description","Description of the authorization concept","Beschreibung des Berechtigungskonzepts","string"),
                new Attribute("process_accessAuthorization_document","Document","Dokument","string","uri"))

        then:
        expected.attributes[0] == schema.attributes[0]
        expected.attributes[1] == schema.attributes[1]
        expected.attributes[2] == schema.attributes[2]
        expected == schema
    }

    def "links are parsed correctly"() {
        given:
        def parser = new TypeParser("../domaintemplates/dsgvo/types/process/" as File)
        def schema = parser.parseLink(new File("../domaintemplates/dsgvo/types/process/links/process_controller.json"))
        def expected = new CustomLink()

        when:
        expected.key = "process_controller"
        expected.attributes = List.of(new Attribute("process_controller_document","Document","Dokument","string", "uri"))
        expected.targetType = "scope"
        expected.targetSubType = "SCP_Controller"

        then:
        expected.attributes[0] == schema.attributes[0]
        expected == schema
    }

    def "rendering works"() {
        given:
        def model = new TemplateModel()
        def aspects = List.of(new CustomAspect("custom_aspect_key", null, null, List.of(new Attribute("attribute_key", "The Attribute", "Das Attribut", "string"))))
        def links = List.of(new CustomLink("custom_link_key", "Custom Link", "Custom Link", List.of(new Attribute("attribute_key", "The Attribute", "Das Attribut", "string"))))
        model.setTypes(List.of(new VeoType("Asset", aspects, links, List.of("AST_Application", "AST_Datatype", "AST_IT-System"))))
        def baos = new ByteArrayOutputStream()
        def ps = new PrintStream(baos)

        when:
        Renderer.render(model, ps)
        def result = baos.toString()
        then:
        result == '''
= Schemas
verinice.veo CustomAspects und -Links
:toc:

== Asset
Subtypes: AST_Application, AST_Datatype, AST_IT-System

=== Custom Aspects (1)
==== custom_aspect_key (No name found.)

===== The Attribute
Translation: Das Attribut +
Key: attribute_key +
Type: String for example: "text" +


=== Custom Links (1)
==== Custom Link (custom_link_key)
Translation: Custom Link +

===== The Attribute
Translation: Das Attribut +
Key: attribute_key +
Type: String for example: "text" +

'''
    }
}