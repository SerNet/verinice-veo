package org.veo.versioning

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.veo.commons.VeoException
import org.veo.service.HistoryService

import groovy.json.JsonSlurper
import spock.lang.Specification

@DataJpaTest(showSql=false)
@ActiveProfiles("test")
class HistoryServiceSpec extends Specification {

    @Autowired
    private HistoryService historyService

    def "create entry for a new element"(){
        given:
        SecurityContext mockSecurityContext = Mock()
        Authentication mockAuthentication = Mock()

        SecurityContextHolder.setContext(mockSecurityContext)
        mockSecurityContext.getAuthentication() >> mockAuthentication
        mockAuthentication.getPrincipal() >> 'versioned-user'

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
        history.first().author == 'versioned-user'
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

    def "unauthorized create"(){
        given:
        SecurityContext mockSecurityContext = Mock()

        SecurityContextHolder.setContext(mockSecurityContext)
        mockSecurityContext.getAuthentication() >> null

        def uuid = UUID.randomUUID().toString()
        def element = [
            title: 'Asset 1',
            foo: 'bar'
        ]
        when:
        historyService.save(uuid, element)

        then:
        VeoException veoException = thrown()
        veoException.error == VeoException.Error.AUTHENTICATION_REQUIRED
    }

    @SpringBootConfiguration
    @ComponentScan("org.veo")
    static class TextConfiguration {
    }
}
