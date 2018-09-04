package org.veo.versioning

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

import spock.lang.Specification

@ContextConfiguration
@DataJpaTest(showSql=false)
@ActiveProfiles("test")
class HistoryServiceSpec extends Specification {

    @Autowired
    private HistoryService historyService;

    def "create entry for a new element"(){
        given:
        def uuid = UUID.randomUUID().toString()
        def element = [
            title: 'Asset 1',
            foo: 'bar'
        ]
        when:
        historyService.save(uuid, element)
        def history = historyService.getHistory(uuid)
        then:
        history.size() == 1
    }
}
