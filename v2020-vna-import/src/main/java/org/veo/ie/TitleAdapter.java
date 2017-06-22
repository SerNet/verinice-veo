/*******************************************************************************
 * Copyright (c) 2015 Daniel Murygin.
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
 * 
 * Contributors:
 *     Daniel Murygin <dm[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package org.veo.ie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.sernet.sync.data.SyncAttribute;
import de.sernet.sync.data.SyncObject;
import de.sernet.sync.mapping.SyncMapping.MapObjectType;
import de.sernet.sync.mapping.SyncMapping.MapObjectType.MapAttributeType;

/**
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
public final class TitleAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(TitleAdapter.class);

    public static final String ASSET_GROUP_TITLE = "assetgroup_name";
    public static final String ASSET_TITLE = "asset_name";
    public static final String AUDIT_GROUP_TITLE = "auditgroup_name";
    public static final String AUDIT_TITLE = "audit_name";
    public static final String CONTROL_GROUP_TITLE = "controlgroup_name";
    public static final String CONTROL_TITLE = "control_name";
    public static final String DOCUMENT_GROUP_TITLE = "document_group_name";
    public static final String DOCUMENT_TITLE = "document_name";
    public static final String EVIDENCE_GROUP_TITLE = "evidence_group_name";
    public static final String EVIDENCE_TITLE = "evidence_name";
    public static final String EXCEPTION_GROUP_TITLE = "exceptiongroup_name";
    public static final String EXCEPTION_TITLE = "exception_name";
    public static final String FINDING_GROUP_TITLE = "finding_group_name";
    public static final String FINDING_TITLE = "finding_name";
    public static final String INCIDENT_GROUP_TITLE = "incident_group_name";
    public static final String INCIDENT_TITLE = "incident_name";
    public static final String INTERVIEW_GROUP_TITLE = "interview_group_name";
    public static final String INTERVIEW_TITLE = "interview_name";
    public static final String ORGANIZATION_TITLE = "org_name";
    public static final String PERSON_GROUP_TITLE = "persongroup_name";
    public static final String PERSON_TITLE = "person_name";
    public static final String PROCESS_GROUP_TITLE = "process_group_name";
    public static final String PROCESS_TITLE = "process_name";
    public static final String RECORD_GROUP_TITLE = "record_group_name";
    public static final String RECORD_TITLE = "record_name";
    public static final String REQUIREMENT_GROUP_TITLE = "requirementgroup_name";
    public static final String REQUIREMENT_TITLE = "requirement_name";
    public static final String RESPONSE_GROUP_TITLE = "response_group_name";
    public static final String RESPONSE_TITLE = "response_name";
    public static final String SAMT_TOPIC_TITLE = "samt_topic_name";
    public static final String SCENARIO_GROUP_TITLE = "incident_scenario_group_name";
    public static final String SCENARIO_TITLE = "incident_scenario_name";
    public static final String THREAT_GROUP_TITLE = "threat_group_name";
    public static final String THREAT_TITLE = "threat_name";
    public static final String VULNERABILITY_GROUP_TITLE = "vulnerability_group_name";
    public static final String VULNERABILITY_TITLE = "vulnerability_name";

    protected static final Map<String, String> TITLE_KEY_MAP;

    static {
        TITLE_KEY_MAP = new HashMap<String, String>();
        TITLE_KEY_MAP.put(NodeFactory.ASSET_GROUP_TYPE, ASSET_GROUP_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.ASSET_TYPE, ASSET_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.AUDIT_GROUP_TYPE, AUDIT_GROUP_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.AUDIT_TYPE, AUDIT_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.CONTROL_GROUP_TYPE, CONTROL_GROUP_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.CONTROL_TYPE, CONTROL_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.DOCUMENT_GROUP_TYPE, DOCUMENT_GROUP_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.DOCUMENT_TYPE, DOCUMENT_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.EVIDENCE_GROUP_TYPE, EVIDENCE_GROUP_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.EVIDENCE_TYPE, EVIDENCE_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.EXCEPTION_GROUP_TYPE, EXCEPTION_GROUP_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.EXCEPTION_TYPE, EXCEPTION_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.FINDING_GROUP_TYPE, FINDING_GROUP_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.FINDING_TYPE, FINDING_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.INCIDENT_GROUP_TYPE, INCIDENT_GROUP_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.INCIDENT_TYPE, INCIDENT_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.INTERVIEW_GROUP_TYPE, INTERVIEW_GROUP_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.INTERVIEW_TYPE, INTERVIEW_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.ORGANIZATION_TYPE, ORGANIZATION_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.PERSON_GROUP_TYPE, PERSON_GROUP_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.PERSON_TYPE, PERSON_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.PROCESS_GROUP_TYPE, PROCESS_GROUP_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.PROCESS_TYPE, PROCESS_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.RECORD_GROUP_TYPE, RECORD_GROUP_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.RECORD_TYPE, RECORD_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.REQUIREMENT_GROUP_TYPE, REQUIREMENT_GROUP_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.REQUIREMENT_TYPE, REQUIREMENT_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.RESPONSE_GROUP_TYPE, RESPONSE_GROUP_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.RESPONSE_TYPE, RESPONSE_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.SAMT_TOPIC_TYPE, SAMT_TOPIC_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.SCENARIO_GROUP_TYPE, SCENARIO_GROUP_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.SCENARIO_TYPE, SCENARIO_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.THREAT_GROUP_TYPE, THREAT_GROUP_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.THREAT_TYPE, THREAT_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.VULNERABILITY_GROUP_TYPE, VULNERABILITY_GROUP_TITLE);
        TITLE_KEY_MAP.put(NodeFactory.VULNERABILITY_TYPE, VULNERABILITY_TITLE);
    }

    public static String getTitle(SyncObject syncObject, MapObjectType mapObject) {
        String intKey = getTitleKey(mapObject.getIntId());
        MapAttributeType mapAttribute = getMapAttribute(mapObject, intKey);
        return (mapAttribute == null) ? null : getAttribute(syncObject.getSyncAttribute(), mapAttribute.getExtId());

    }

    public static String getTitleKey(String extObjectType) {
        String key = TITLE_KEY_MAP.get(extObjectType);
        if (key == null) {
            LOG.warn("No title key found for type: " + extObjectType);
        }
        return key;
    }

    public static String getAttribute(List<SyncAttribute> syncAttributeList, String key) {
        for (SyncAttribute attribute : syncAttributeList) {
            if (key.equals(attribute.getName())) {
                return attribute.getValue().get(0);
            }
        }
        LOG.warn("No title found for key: " + key);
        return null;
    }

    private static MapAttributeType getMapAttribute(MapObjectType mapObject, String intId) {
        for (MapAttributeType mapAttribute : mapObject.getMapAttributeType()) {
            if (intId.equals(mapAttribute.getIntId())) {
                return mapAttribute;
            }
        }
        return null;
    }
}
