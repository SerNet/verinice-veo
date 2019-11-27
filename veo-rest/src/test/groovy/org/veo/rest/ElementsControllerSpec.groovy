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
package org.veo.rest

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import org.veo.service.ElementMapService
import org.veo.service.HistoryService
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

    def "getRootElements"() throws Exception {
        when:
        def result = mockMvc.perform(MockMvcRequestBuilders.get("/elements").param("parent","").accept("application/json")).andReturn()
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
        def result = mockMvc.perform(MockMvcRequestBuilders.get("/elements/deadbeef").accept("application/json;charset=UTF-8")).andReturn()
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
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(postBody.toString())
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
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
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(postBody.toString())
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)).andReturn()
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
                    id   : 'b2bde5ab-b496-4466-af24-1ed2c6166313',
                    title: "I'm a root element…"
                ],
                [
                    id   : '6e0b04d1-e0e1-42a2-a965-ffb1c45352f0',
                    title: "I'm a 2nd root element…"
                ],
                [
                    id   : '816ed09c-175a-4527-a2c1-150c93814d18',
                    title: "I'm a child",
                    parent: "6e0b04d1-e0e1-42a2-a965-ffb1c45352f0"
                ],
                [
                    id   : 'ef9c43de-4c5c-47d7-a034-cfab9517ee20',
                    title: "I'm a child, too",
                    parent: "b2bde5ab-b496-4466-af24-1ed2c6166313"
                ],
            ]
        }

        List<Map<String, Object>> findRootElements() {
            return [
                [
                    id   : 'b2bde5ab-b496-4466-af24-1ed2c6166313',
                    title: "I'm a root element…"
                ],
                [
                    id   : '6e0b04d1-e0e1-42a2-a965-ffb1c45352f0',
                    title: "I'm a 2nd root element…"
                ]
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
