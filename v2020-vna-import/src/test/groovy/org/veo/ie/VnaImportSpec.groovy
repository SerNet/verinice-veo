package org.veo.ie

import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.veo.persistence.PersistenceApplication
import org.veo.service.ElementService
import org.veo.service.ie.VnaImport

import spock.lang.Specification

@ContextConfiguration
@DataJpaTest(showSql=false)
@ActiveProfiles("test")
class VnaImportSpec extends Specification {

    @Autowired
    VnaImport vnaImport;

    @Autowired
    ElementService elementService;

    def "import RECPLAST VNA"(){
        setup:
        def is = VnaImportSpec.class.getResourceAsStream('RECPLAST.vna')
        def bytes = is.bytes
        when:
        def vna = vnaImport.importVna(bytes)
        then:
        elementService.findAll().size() == 2272
    }
}
