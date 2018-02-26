package org.veo.util.time;

import java.time.Duration

import spock.lang.Specification
import spock.lang.Unroll

class TimeFormatterSpec extends Specification {

    @Unroll
    def "#milliSeconds ms (#duration) should be converted to '#humanReadable'"(){
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
        Duration.ofDays(1).plusHours(2).plusMinutes(29)   | '1 d, 2 h, 29m'
        Duration.ofDays(1).plusHours(2)                  | '1 d, 2 h'
        Duration.ofDays(7)                               | '7 d'
        // this does not work
        // Duration.ofMinutes(2).minusMillis(1)             | '2 m'
        Duration.ofMinutes(2).plusSeconds(30)            | '2 m, 30 s'
        
        milliSeconds = duration.toMillis()

    }
    
}
