/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.message

import java.time.Instant

import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper

import org.veo.core.VeoSpringSpec

class MessageConverterSpec extends VeoSpringSpec {
    @Autowired
    private MessageConverter messageConverter
    private ObjectMapper objectMapper = new ObjectMapper()

    def "serializes timestamps as strings"() {
        given:
        def datetime = "2007-12-03T10:15:30Z"
        def input = new EventMessage("someKey", "be content", 42l, Instant.parse(datetime))

        when:
        def message = messageConverter.toMessage(input, new MessageProperties())
        def timestamp = objectMapper.readTree(message.body).get("timestamp").asText()

        then:
        timestamp == datetime
    }
}
