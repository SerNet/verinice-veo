/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Alexander Koderman
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

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

public class TestContainersUtil {

    public static GenericContainer startRabbitMqContainer() {
        def rabbit = null
        if (!System.env.containsKey('SPRING_RABBITMQ_HOST')) {
            println("Test will start RabbitMQ container...")

            rabbit = new GenericContainer("rabbitmq:3-management")
                    .withExposedPorts(5672, 15672)
                    .waitingFor(Wait.forListeningPort())
                    .tap {
                        it.start()
                    }

            System.properties.putAll([
                "spring.rabbitmq.host": rabbit.getHost(),
                "spring.rabbitmq.port": rabbit.getMappedPort(5672),
            ])
        }
        return rabbit
    }
}
