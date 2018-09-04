package org.veo.versioning

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

import groovy.json.JsonSlurper
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
        history.first().dataId == uuid
        new JsonSlurper().parseText(history.first().data) == element

        when:
        def element2 = element.clone()
        element2.foo = 'baz'
        historyService.save(uuid, element2)
        history = historyService.getHistory(uuid)
        then:
        history.size() == 2
        new JsonSlurper().parseText(history.first().data) == element
        new JsonSlurper().parseText(history[1].data) == element2
    }
}
