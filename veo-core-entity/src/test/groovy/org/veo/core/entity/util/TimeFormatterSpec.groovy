/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2018  Jochen Kemnade.
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
package org.veo.core.entity.util

import java.time.Duration

import spock.lang.Specification

class TimeFormatterSpec extends Specification {

    def "#milliSeconds ms (#duration) should be converted to '#humanReadable'"() {
        expect:

        TimeFormatter.getHumanRedableTime(milliSeconds) == humanReadable
        where:
        duration                                         | humanReadable
        Duration.ofMillis(1)                             | '1 ms'
        Duration.ofMillis(1024)                          | '1 s'
        Duration.ofMillis(1624)                          | '2 s'
        Duration.ofHours(3)                              | '3 h'
        Duration.ofHours(2).plusMinutes(30)              | '2 h, 30 m'
        Duration.ofDays(1).plusHours(2).plusMinutes(3)   | '1 d, 2 h'
        Duration.ofDays(1).plusHours(2).plusMinutes(45)  | '1 d, 3 h'
        Duration.ofDays(1).plusHours(2)                  | '1 d, 2 h'
        Duration.ofDays(7)                               | '7 d'
        Duration.ofMinutes(2).minusMillis(1)             | '2 m'
        Duration.ofMinutes(2).plusSeconds(30)            | '2 m, 30 s'

        milliSeconds = duration.toMillis()
    }
}
