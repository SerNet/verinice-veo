/*******************************************************************************
 * Copyright (c) 2015 Daniel Murygin.
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
 *     Daniel Murygin <dm[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package org.veo.util.io;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

/**
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
public final class XmlIO {

    private XmlIO() {
        // do not instantiate this class
    }

    public static <T> T read(String xmlFileName, Class<T> clazz) {
        return javax.xml.bind.JAXB.unmarshal(new File(xmlFileName), clazz);
    }

    public static <T> T read(String xsdSchemaFileName, String xmlFileName, Class<T> clazz) throws JAXBException, SAXException {
        // Schema und JAXBContext are thread safe
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = (xsdSchemaFileName == null || xsdSchemaFileName.trim().length() == 0) ? null : schemaFactory.newSchema(new File(xsdSchemaFileName));
        JAXBContext jaxbContext = JAXBContext.newInstance(clazz.getPackage().getName());
        return read(jaxbContext, schema, xmlFileName, clazz);
    }

    public static <T> T read(JAXBContext jaxbContext, Schema schema, String xmlFileName, Class<T> clazz) throws JAXBException {
        // Unmarshaller ist not thread safe
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setSchema(schema);
        return clazz.cast(unmarshaller.unmarshal(new File(xmlFileName)));
    }

    public static void write(String xsdSchemaFileName, String xmlFileName, Object jaxbElement) throws JAXBException, SAXException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = (xsdSchemaFileName == null || xsdSchemaFileName.trim().length() == 0) ? null : schemaFactory.newSchema(new File(xsdSchemaFileName));
        JAXBContext jaxbContext = JAXBContext.newInstance(jaxbElement.getClass().getPackage().getName());
        write(jaxbContext, schema, xmlFileName, jaxbElement);
    }

    public static void write(JAXBContext jaxbContext, Schema schema, String xmlFileName, Object jaxbElement) throws JAXBException {
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setSchema(schema);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(jaxbElement, new File(xmlFileName));
    }
}
