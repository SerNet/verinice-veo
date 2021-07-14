/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman
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
package org.veo.rest.test

import static groovy.json.JsonOutput.toJson
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

import java.util.regex.Matcher
import java.util.regex.Pattern

import org.apache.http.HttpHost
import org.apache.http.impl.client.HttpClientBuilder
import org.keycloak.authorization.client.AuthzClient
import org.keycloak.authorization.client.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles

import com.fasterxml.jackson.databind.node.ObjectNode

import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.rest.RestApplication
import org.veo.rest.security.WebSecurity

import groovy.json.JsonSlurper
import spock.lang.Shared

@SpringBootTest(classes = [RestApplication.class, WebSecurity.class],
webEnvironment = RANDOM_PORT)
@ActiveProfiles(["test", "resttest", "local"])
class VeoRestTest extends spock.lang.Specification {

    @Autowired
    TestRestTemplate restTemplate

    @Shared
    JsonSlurper jsonSlurper

    @Shared
    EntityFactory factory = new EntityDataFactory()

    @Value('${veo.resttest.user}')
    String user

    @Value('${veo.resttest.baseUrl}')
    String baseUrl

    @TestConfiguration
    @Profile("resttest")
    static class TestRestTemplateAuthenticationConfiguration {

        @Value('${veo.resttest.oidcUrl}')
        def oidcUrl = 'https://keycloak.staging.verinice.com'

        @Value('${veo.resttest.realm}')
        def realm = 'verinice-veo'

        @Value('${veo.resttest.clientId}')
        def clientId = 'veo-development-client'

        @Value('${veo.resttest.user}')
        private String user

        @Value('${veo.resttest.pass}')
        private String pass

        @Value('${veo.resttest.proxyHost}')
        private String proxyHost

        @Value('${veo.resttest.proxyPort}')
        private int proxyPort

        @Bean
        public RestTemplateBuilder restTemplateBuilder() {
            HttpHost proxy = new HttpHost(proxyHost, proxyPort)
            def user = this.user
            def pass = this.pass

            def accessToken = HttpClientBuilder.create().with {
                it.proxy = proxy
                build()
            }.withCloseable {
                Configuration configuration = new Configuration("$oidcUrl/auth", realm, clientId, ['secret':''], it)
                AuthzClient authzClient = AuthzClient.create(configuration)
                def accessTokenResponse = authzClient.obtainAccessToken(user, pass)
                accessTokenResponse.token
            }
            return new RestTemplateBuilder().defaultHeader('Authorization', 'Bearer ' + accessToken)
        }
    }

    class Response{
        Map headers
        Object body
    }

    def setupSpec() {
        jsonSlurper = new JsonSlurper()
    }

    def String getETag(String text) {
        Pattern p = Pattern.compile("\"([^\"]*)\"")
        Matcher m = p.matcher(text)
        if (m.find()) {
            return m.group(1)
        } else {
            return text
        }
    }



    Response get(String relativeUri, int assertStatusCode = 200) {
        def absoluteUrl = baseUrl + relativeUri
        def resp = restTemplate.exchange(absoluteUrl, HttpMethod.GET, null, ObjectNode.class)
        assert resp.statusCodeValue == assertStatusCode
        new Response(
                headers: resp.headers,
                body: jsonSlurper.parseText(resp.body.toString()))
    }

    Response post(String relativeUri, Object requestBody, int assertStatusCode = 201) {
        def absoluteUrl = baseUrl + relativeUri
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity<String> request = new HttpEntity<>(toJson(requestBody), headers)

        def resp = restTemplate.exchange(absoluteUrl, HttpMethod.POST, request, ObjectNode.class)
        assert resp.statusCodeValue == assertStatusCode
        new Response(headers: resp.headers,
        body: jsonSlurper.parseText(resp.body.toString()))
    }

    void put(String relativeUri, Object requestBody, String etagHeader, int assertStatusCode = 200) {
        def absoluteUrl = baseUrl + relativeUri
        HttpHeaders headers = new HttpHeaders()
        headers.setIfMatch(getETag(etagHeader))
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity putEntity = new HttpEntity(toJson(requestBody), headers)

        def resp = restTemplate.exchange(absoluteUrl, HttpMethod.PUT, putEntity, ObjectNode.class)
        assert resp.statusCodeValue == assertStatusCode
    }

    void delete(String relativeUri, int assertStatusCode = 204) {
        def absoluteUrl = baseUrl + relativeUri
        def resp = restTemplate.exchange(absoluteUrl, HttpMethod.DELETE, null, ObjectNode.class)
        assert resp.statusCodeValue == assertStatusCode
    }
}
