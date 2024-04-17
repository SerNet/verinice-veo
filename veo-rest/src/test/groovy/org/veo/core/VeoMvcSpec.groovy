/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core

import static groovy.json.JsonOutput.toJson
import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

import org.veo.rest.configuration.WebMvcSecurityConfiguration

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

/**
 * Base class for veo specifications that use MockMvc
 */
@AutoConfigureMockMvc
@SpringBootTest(
webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
classes = [WebMvcSecurityConfiguration]
)
@EnableAsync
abstract class VeoMvcSpec extends VeoSpringSpec {

    // TODO VEO-663 make private again
    @Autowired
    protected MockMvc mvc

    ResultActions postUnauthorized(String url, Map content) {
        ResultActions asyncActions = mvc
                .perform(
                MockMvcRequestBuilders.post(url)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON))
        return asyncActions.andExpect(status().is4xxClientError())
    }

    ResultActions putUnauthorized(String url, Map content) {
        ResultActions asyncActions = mvc
                .perform(
                MockMvcRequestBuilders.put(url)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON))
        return asyncActions.andExpect(status().is4xxClientError())
    }

    ResultActions multipart(String url, Map content, int expectedStatusCode = 201) {
        doRequest(MockMvcRequestBuilders.multipart(url)
                .file(toFile(content)),
                expectedStatusCode)
    }

    ResultActions post(String url, Map content, int expectedStatusCode = 201) {
        doRequest(MockMvcRequestBuilders.post(url)
                .contentType(APPLICATION_JSON)
                .content(toJson(content))
                .accept(APPLICATION_JSON),
                expectedStatusCode)
    }

    ResultActions get(URI uri, int expectedStatusCode = 200) {
        doRequest(MockMvcRequestBuilders.get(uri)
                .accept(APPLICATION_JSON), expectedStatusCode)
    }

    ResultActions get(String url, int expectedStatusCode = 200, MediaType mediaType = APPLICATION_JSON) {
        doRequest(MockMvcRequestBuilders.get(url)
                .accept(mediaType), expectedStatusCode)
    }

    ResultActions put(String url, Map content, Map headers, int expectedStatusCode = 200) {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put(url)
                .contentType(APPLICATION_JSON)
                .content(toJson(content))
                .accept(APPLICATION_JSON)
        for (key in headers.keySet()) {
            requestBuilder.header(key, headers.get(key))
        }
        doRequest(requestBuilder,
                expectedStatusCode)
    }

    ResultActions put(String url, Map content, int expectedStatusCode = 200) {
        doRequest(MockMvcRequestBuilders.put(url)
                .contentType(APPLICATION_JSON)
                .content(toJson(content))
                .accept(APPLICATION_JSON),
                expectedStatusCode)
    }

    ResultActions delete(String url, int expectedStatusCode = 204) {
        doRequest(MockMvcRequestBuilders.delete(url)
                .accept(APPLICATION_JSON), expectedStatusCode)
    }

    ResultActions doRequest(MockHttpServletRequestBuilder requestBuilder, int expectedStatusCode) throws Exception {
        boolean expectSuccessfulRequest = HttpStatus.resolve(expectedStatusCode).is2xxSuccessful()
        def asyncActions = mvc.perform(requestBuilder)
        def asyncResult = asyncActions.andReturn()
        if (asyncResult.resolvedException != null) {
            asyncActions.andExpect(status().is(expectedStatusCode))
            throw asyncResult.resolvedException
        }
        if (expectSuccessfulRequest) {
            asyncResult.request.with {
                asyncActions.andExpect(status().is2xxSuccessful())
                assert asyncStarted
            }
        } else if (!asyncResult.request.asyncStarted) {
            // The async request may fail to start if the request is invalid (e.g. has invalid HTTP header or params).
            asyncActions.andExpect(status().is(expectedStatusCode))
            return asyncActions
        }
        ResultActions actions = mvc
                .perform(MockMvcRequestBuilders.asyncDispatch(asyncResult))
        MvcResult result = actions.andReturn()

        if (expectSuccessfulRequest) {
            assert result.resolvedException == null
            actions.andExpect(status().is(expectedStatusCode))
        } else {
            actions.andExpect(status().is(expectedStatusCode))
            if(result.resolvedException != null) {
                throw result.resolvedException
            }
        }
        return actions
    }

    def parseJson(ResultActions resultActions) {
        parseJson(resultActions.andReturn().response.getContentAsString(StandardCharsets.UTF_8))
    }

    def parseJson(String json) {
        new JsonSlurper().parseText(json)
    }

    String getETag(ResultActions resultActions) {
        resultActions.andReturn().response.getHeader("ETag")
    }

    MockMultipartFile toFile(Map json) {
        def jsonString = JsonOutput.prettyPrint(JsonOutput.toJson(json))
        return new MockMultipartFile("file", "", "application/json", jsonString.getBytes(StandardCharsets.UTF_8));
    }
}
