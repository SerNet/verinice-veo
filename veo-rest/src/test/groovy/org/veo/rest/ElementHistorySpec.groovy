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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import spock.lang.Specification

@SpringBootTest("veo.basedir=/tmp/veo")
public class ElementHistorySpec extends Specification {

    @Autowired
    private ElementsController elementsController
    private MockMvc mockMvc

    def setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(elementsController).build();
    }

    void "CRUD operations create history entries"(){
        given:
        def mockAuthentication = Stub(Authentication){ getPrincipal() >> 'versioned-user' }
        def mockSecurityContext = Mock(SecurityContext){ getAuthentication() >> mockAuthentication }
        SecurityContextHolder.setContext(mockSecurityContext)

        def data = ['$veo.title':'Asset 1', '$veo.type': 'asset']

        when:
        def uuid = createElement(data)
        def history = getHistory(uuid)
        then:
        history.size() ==1
        when:
        def firstHistoryEntry = history.first()
        then:
        firstHistoryEntry.author == 'versioned-user'
        when:
        def savedData = parseJSON(firstHistoryEntry.data)
        then:
        savedData.'$veo.title' == 'Asset 1'
        when:
        def newData = ['$veo.title':'Asset 1', '$veo.type': 'asset', 'description': 'Example asset']
        saveElement(uuid, newData)
        history = getHistory(uuid)
        then:
        history.size() == 2
        parseJSON(history[0].data).description == null
        parseJSON(history[1].data).description == 'Example asset'
        when:
        deleteElement(uuid)
        history = getHistory(uuid)
        then:
        history.size() == 3
        history[2].data == null
    }


    /**
     * Create an element with the given data and return its uuid
     */
    String createElement(Map data) {
        def postBody = JsonOutput.toJson(data)
        def response = mockMvc.perform(MockMvcRequestBuilders.post("/elements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(postBody.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andReturn().response
        def locationHeader = response.getHeader("Location")
        locationHeader.replaceFirst("/elements/", "")
    }

    /**
     * Create an element with the given data and return its uuid
     */
    void saveElement(String uuid, Map data) {
        def postBody = JsonOutput.toJson(data)

        def result = mockMvc.perform(MockMvcRequestBuilders.put("/elements/$uuid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(postBody.toString())
                .accept(MediaType.APPLICATION_JSON)).andReturn()
        def response = result.response
        assert response.status == HttpStatus.NO_CONTENT.value()
    }

    /**
     * Delete the element with the given uuid
     */
    void deleteElement(String uuid) {

        def result =
                this.mockMvc.perform(MockMvcRequestBuilders.delete("/elements/$uuid").accept("application/json"))
                .andReturn()
        def response = result.response
        assert response.status == HttpStatus.OK.value()
    }


    /**
     * Return the history for a given uuid
     */
    List getHistory(String uuid) {
        def response = mockMvc.perform(MockMvcRequestBuilders.get("/elements/$uuid/history")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andReturn().response
        def history = parseJSON(response.contentAsString)
        assert history instanceof List
        history
    }

    static def parseJSON(string) {
        new JsonSlurper().parseText(string)
    }
}
