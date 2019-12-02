package org.veo.core.entity

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


import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.time.Instant
import spock.lang.Specification

import org.veo.core.entity.EntityLayerSupertype.Lifecycle
import org.veo.core.entity.Key
import org.veo.core.entity.asset.Asset
import org.veo.core.entity.group.EntityGroup
import org.veo.core.entity.process.Process;
import org.veo.core.entity.specification.InvalidUnitException
import org.veo.core.entity.group.EntityGroup

public class EntityGroupSpec extends Specification {

    Unit unit;
    
    def setup() {
        this.unit = Unit.newUnitBelongingToClient(Client.newClient("New Client"), "New Unit");
    }
    
    
    def "A group object can be created" () {
        given: "a timestamp"
            Instant beforeCreation = Instant.now();
            
        when: "a new group object is created"
            EntityGroup<Asset> group = EntityGroup.newGroup(unit, "New group");
            
        then: "the group is initialized as expected"
            group.getKey() != null;
            group.getName().equals("New group");
            group.getState().equals(Lifecycle.CREATING);
            group.getVersion().equals(0L);
            group.getValidFrom()?.isAfter(beforeCreation);
            group.getValidFrom()?.isBefore(Instant.now());
            group.getValidUntil() == null;
    }
    
    def "A group must have unit" () {
        when: "a group is created with no unit"
            EntityGroup group = EntityGroup.existingGroup(Key.newUuid(), null, "no group");
            
       then: "an exception is thrown"
           thrown InvalidUnitException;
    }
    
    def "A group can contain assets" () {
        given: "a set of two assets"
            Set assets = Set.of(
                Asset.newAsset(unit, "New asset 1"),
                Asset.newAsset(unit, "New asset 2")
            );

        when: "a group is created with the assets:"
            EntityGroup<Asset> assetGroup = EntityGroup.existingGroup(Key.newUuid(), unit, "Asset-Group");
            assetGroup.setGroupMembers(assets);

        then: "the group contains the assets"
           assetGroup.getGroupMembers().size() == 2;
    }

/*
    def "A group can contain processes" () {
    }

    
    def "A group can only contain objects of the same type" () {
    }

    
    def "A group can contain subgroups of identical types" () {
    }
    
    def "A group can contain subgroups of different types" () {
    }
    
    def "A group can not contain itself" () {
    }

    
    def "A group must not contain elements from a different client" () {
    }
    
    
    def "A group can return its members" () {
        
    }
    
    
    def "A group can return members that fulfill a specification" () {
        
    }
    
    def "A group can be used just like a single element of the same type" () {
        
    }
*/
    
}
