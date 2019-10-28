/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman
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
package org.veo.core.entity

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import spock.lang.Specification

import org.veo.core.entity.Key

public class KeySpec extends Specification {

    def "Create an undefined key" () {
        given: "a simple key based on a String value"
        Key other = new Key("Key")
    
        when: "an undefined key is created"
        Key key = Key.undefined()
    
        then: "the key compares correctly to other keys"
        key.equals(other) == false
        other.equals(key) == false
        key.isUndefined() == true
    }
    
    def "Create a simple value key" () {
        given: "some keys to compare against"
        Key simpleKey = new Key("NotTheValue")
        Key compoundKeyMultiType = new Key(["One", 2, true])
        Key compoundKeySameType = new Key(["One", "Two", "Three"])
        Key sameKey = new Key("TheValue")
    
        when: "a Key is based on a simple value"
        Key key = new Key("TheValue");
        
        then: "the key compares correctly to other keys"
        !key.undefined();
        !key.equals(simpleKey)
        !key.equals(compoundKeyMultiType)
        !key.equals(compoundKeySameType)
        key.equals(sameKey)
        
        when: "we read the simple value from the key"
        String value = key.value() 
        
        then: "we receive the value the key was created from"
        value == "TheValue"
        
    
    }
    
    def "Create a compound value key" () {
        given: "some keys to compare against"
        Key otherSimpleKey = new Key("NotTheValue")
        Key otherCompoundKeyMultiType = new Key(["TheValue", 2, true])
        Key otherCompoundKeySameType = new Key(["One", "Two", "Three"])
        Key sameKey = new Key("TheValue", 2, false);
    
        when: "a Key is based on a compound value"
        Key key = new Key("TheValue", 2, false);
        
        then: "the key compares correctly to other keys"
        !key.undefined();
        !key.equals(otherSimpleKey)
        !key.equals(otherCompoundKeyMultiType)
        !key.equals(otherCompoundKeySameType)
        
        when: "we try to read a compound key as a simple value"
        key.equals(sameKey)
        
        then: "an exception is thrown"
        key.value() thrown
    }

    def "Create a UUID based key" () {
        given: "two keys to compare against"
        Key otherSimpleKey = new Key("NotTheValue")
        Key otherCompoundKeyMultiType = new Key(["TheValue", 2, true])
        Key otherCompoundKeySameType = new Key(["One", "Two", "Three"])
        
        when: "a UUID-based key is created"
        Key key = Key.newUuid();
        
        then: "the key compares correctly to other keys"
        !key.isUndefined()
        !key.equals(otherSimpleKey)
        !key.equals(otherCompoundKeyMultiType)
        !key.equals(otherCompoundKeySameType)
        
        and: "the UUID can be read"
        String uuid = key.uuidValue();
        
        and: "the key equals one created with the same UUID"
        Key sameKey = new Key();
    }

}
