/*******************************************************************************
 * Copyright (c) 2018 Jochen Kemnade.
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
 ******************************************************************************/
package org.veo.util.io

import java.nio.file.Files

import javax.xml.bind.JAXBContext

import spock.lang.Specification

class XmlIOSpec extends Specification {

    def "read a Person from an XML file"(){
        setup:
        File file = Files.createTempFile('xmliospec', '.xml').toFile();
        file.text = '''<person><age>42</age><name>John Doe</name></person>'''

        when:
        def person = XmlIO.read(file.absolutePath, Person)

        then:
        person.name == 'John Doe'
        person.age == 42

        cleanup:
        file?.delete()
    }

    def "serialize a Person to an XML string"(){
        setup:
        def person = new Person()
        person.name = 'Guybrush Threepwood'
        person.age = 23
        File file = Files.createTempFile('xmliospec', '.xml').toFile();

        when:
        JAXBContext jaxbContext = JAXBContext.newInstance(Person)
        XmlIO.write(jaxbContext, null, file.absolutePath, person)

        then:
        file.text ==
                '''\
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<person>
    <age>23</age>
    <name>Guybrush Threepwood</name>
</person>
'''
        cleanup:
        file?.delete()
    }
}
