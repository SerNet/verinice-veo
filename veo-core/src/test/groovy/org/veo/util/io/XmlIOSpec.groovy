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
