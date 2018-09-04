package org.veo.rest

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.veo.service.ElementMapService
import org.veo.versioning.HistoryService

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import spock.lang.Specification

@SpringBootTest("veo.basedir=/tmp/veo")
public class ElementsControllerSpec extends Specification {

    private ElementsController elementsController
    private MockMvc mockMvc

    def setup() {
        def mockMapService = new MockMapService();
        HistoryService historyService = Mock()
        this.elementsController = new ElementsController(mockMapService, historyService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(elementsController).build();
    }


    def "getAll"() throws Exception {
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get("/elements").accept("application/json")).andReturn()
        def response = result.response
        then:
        response.status == HttpStatus.OK.value()
        response.contentType == MediaType.APPLICATION_JSON_UTF8_VALUE
        when:
        def parsedResponse =
                new JsonSlurper().parseText(response.contentAsString)
        then:
        parsedResponse instanceof List
    }

    def "get"() throws Exception {
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get("/elements/deadbeef").accept("application/json")).andReturn()
        def response = result.response
        then:
        response.status == HttpStatus.OK.value()
        response.contentType == MediaType.APPLICATION_JSON_UTF8_VALUE
        when:
        def parsedResponse =
                new JsonSlurper().parseText(response.contentAsString)
        then:
        parsedResponse.title == "I'm test data…"
    }

    def "getChildren"() throws Exception {
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get("/elements/abad1dea/children").accept("application/json")).andReturn()
        def response = result.response
        then:
        response.status == HttpStatus.OK.value()
        response.contentType == MediaType.APPLICATION_JSON_UTF8_VALUE
        when:
        def parsedResponse =
                new JsonSlurper().parseText(response.contentAsString)
        then:
        parsedResponse instanceof List
        parsedResponse[0].title == "I'm a child"
        parsedResponse[1].title == "I'm a child, too"
    }

    def "post"() throws Exception {
        given:
        def postBody = JsonOutput.toJson([title:"I am new"])
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.post("/elements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(postBody.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andReturn()
        def response = result.response
        then:
        response.status == HttpStatus.CREATED.value()
        response.getHeader("Location") == "/elements/444444"
    }

    def "put"() throws Exception {


        def postBody = JsonOutput.toJson([
            id: "deadbeef",
            title: "I am modified",
            new: "value"
        ])
        when:

        def result = mockMvc.perform(MockMvcRequestBuilders.put("/elements/deadbeef")
                .contentType(MediaType.APPLICATION_JSON)
                .content(postBody.toString())
                .accept(MediaType.APPLICATION_JSON)).andReturn()
        def response = result.response
        then:
        response.status == HttpStatus.NO_CONTENT.value()
    }

    void "delete"() throws Exception {
        when:
        def result =
                this.mockMvc.perform(MockMvcRequestBuilders.delete("/elements/abad1dea").accept("application/json"))
                .andReturn()
        def response = result.response
        then:
        response.status == HttpStatus.OK.value()
    }

    private class MockMapService implements ElementMapService {

        @Override
        List<Map<String, Object>> findAll()  {
            return [
                [
                    id   : 'deadbeef',
                    title: "I'm test data…"
                ],
                [
                    id   : 'abad1dea',
                    title: "I'm test data, too"
                ],
                [
                    id   : '813ad81',
                    title: "I'm a child",
                    parent: "abad1dea"
                ],
                [
                    id   : '813ad81',
                    title: "I'm a child, too",
                    parent: "abad1dea"
                ],
            ]
        }

        @Override
        Map<String, Object> find(String id)  {
            return [
                id   : 'deadbeef',
                title: "I'm test data…"
            ]
        }

        @Override
        List<Map<String, Object>> findChildren(String parentId) {
            return [
                [
                    id   : '813ad81',
                    title: "I'm a child",
                    parent: "abad1dea"
                ],
                [
                    id   : '813ad81',
                    title: "I'm a child, too",
                    parent: "abad1dea"
                ],
            ]
        }

        @Override
        void save(String id, Map<String, Object> content) {
        }

        @Override
        String saveNew(Map<String, Object> content) {
            return "444444"
        }

        @Override
        void delete(String id) {
        }
    }
}
