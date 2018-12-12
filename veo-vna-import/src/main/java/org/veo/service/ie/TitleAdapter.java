/*******************************************************************************
 * Copyright (c) 2017 Daniel Murygin.
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
package org.veo.service.ie;

import java.util.Collections;
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
 * Returns a title of a SyncObject from verinice archive VNAs.
 * 
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
public final class TitleAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(TitleAdapter.class);

    // Title properties for ISO27000 elements
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
    public static final String PERSON_TITLE = "person-iso_surname";
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

    // Title properties for ITBP elements
    public static final String RAUM_TITLE = "raum_name";
    public static final String SERVER_TITLE = "server_name";
    public static final String TKKOMPONENTE_TITLE = "tkkomponente_name";
    public static final String NETZKOMPONENTE_TITLE = "netzkomponente_name";
    public static final String ITVERBUND_TITLE = "itverbund_name";
    public static final String CLIENT_TITLE = "client_name";
    public static final String SONSTIT_TITLE = "sonstit_name";
    public static final String PERSON_ISO_TITLE = "nachname";
    public static final String GEBAEUDE_TITLE = "gebaeude_name";
    public static final String BSTUMSETZUNG_TITLE = "bstumsetzung_name";
    public static final String GEFAEHRDUNGSUMSETZUNG_TITLE = "gefaehrdungsumsetzung_titel";
    public static final String MNUMS_TITLE = "mnums_id";
    public static final String ANWENDUNG_TITLE = "anwendung_name";

    protected static final Map<String, String> TITLE_KEY_MAP;
    protected static final Map<String, String> TITLE_STATIC_MAP;

    static {
        Map<String, String> titleKeyMap = new HashMap<>();
        // ISO27000 elements
        titleKeyMap.put(ElementFactory.ASSET_GROUP_TYPE, ASSET_GROUP_TITLE);
        titleKeyMap.put(ElementFactory.ASSET_TYPE, ASSET_TITLE);
        titleKeyMap.put(ElementFactory.AUDIT_GROUP_TYPE, AUDIT_GROUP_TITLE);
        titleKeyMap.put(ElementFactory.AUDIT_TYPE, AUDIT_TITLE);
        titleKeyMap.put(ElementFactory.CONTROL_GROUP_TYPE, CONTROL_GROUP_TITLE);
        titleKeyMap.put(ElementFactory.CONTROL_TYPE, CONTROL_TITLE);
        titleKeyMap.put(ElementFactory.DOCUMENT_GROUP_TYPE, DOCUMENT_GROUP_TITLE);
        titleKeyMap.put(ElementFactory.DOCUMENT_TYPE, DOCUMENT_TITLE);
        titleKeyMap.put(ElementFactory.EVIDENCE_GROUP_TYPE, EVIDENCE_GROUP_TITLE);
        titleKeyMap.put(ElementFactory.EVIDENCE_TYPE, EVIDENCE_TITLE);
        titleKeyMap.put(ElementFactory.EXCEPTION_GROUP_TYPE, EXCEPTION_GROUP_TITLE);
        titleKeyMap.put(ElementFactory.EXCEPTION_TYPE, EXCEPTION_TITLE);
        titleKeyMap.put(ElementFactory.FINDING_GROUP_TYPE, FINDING_GROUP_TITLE);
        titleKeyMap.put(ElementFactory.FINDING_TYPE, FINDING_TITLE);
        titleKeyMap.put(ElementFactory.INCIDENT_GROUP_TYPE, INCIDENT_GROUP_TITLE);
        titleKeyMap.put(ElementFactory.INCIDENT_TYPE, INCIDENT_TITLE);
        titleKeyMap.put(ElementFactory.INTERVIEW_GROUP_TYPE, INTERVIEW_GROUP_TITLE);
        titleKeyMap.put(ElementFactory.INTERVIEW_TYPE, INTERVIEW_TITLE);
        titleKeyMap.put(ElementFactory.ORGANIZATION_TYPE, ORGANIZATION_TITLE);
        titleKeyMap.put(ElementFactory.PERSON_GROUP_TYPE, PERSON_GROUP_TITLE);
        titleKeyMap.put(ElementFactory.PERSON_ISO_TYPE, PERSON_TITLE);
        titleKeyMap.put(ElementFactory.PROCESS_GROUP_TYPE, PROCESS_GROUP_TITLE);
        titleKeyMap.put(ElementFactory.PROCESS_TYPE, PROCESS_TITLE);
        titleKeyMap.put(ElementFactory.RECORD_GROUP_TYPE, RECORD_GROUP_TITLE);
        titleKeyMap.put(ElementFactory.RECORD_TYPE, RECORD_TITLE);
        titleKeyMap.put(ElementFactory.REQUIREMENT_GROUP_TYPE, REQUIREMENT_GROUP_TITLE);
        titleKeyMap.put(ElementFactory.REQUIREMENT_TYPE, REQUIREMENT_TITLE);
        titleKeyMap.put(ElementFactory.RESPONSE_GROUP_TYPE, RESPONSE_GROUP_TITLE);
        titleKeyMap.put(ElementFactory.RESPONSE_TYPE, RESPONSE_TITLE);
        titleKeyMap.put(ElementFactory.SAMT_TOPIC_TYPE, SAMT_TOPIC_TITLE);
        titleKeyMap.put(ElementFactory.SCENARIO_GROUP_TYPE, SCENARIO_GROUP_TITLE);
        titleKeyMap.put(ElementFactory.SCENARIO_TYPE, SCENARIO_TITLE);
        titleKeyMap.put(ElementFactory.THREAT_GROUP_TYPE, THREAT_GROUP_TITLE);
        titleKeyMap.put(ElementFactory.THREAT_TYPE, THREAT_TITLE);
        titleKeyMap.put(ElementFactory.VULNERABILITY_GROUP_TYPE, VULNERABILITY_GROUP_TITLE);
        titleKeyMap.put(ElementFactory.VULNERABILITY_TYPE, VULNERABILITY_TITLE);
        // ITBP elements
        titleKeyMap.put(ElementFactory.ANWENDUNG_TYPE, ANWENDUNG_TITLE);
        titleKeyMap.put(ElementFactory.BSTUMSETZUNG_TYPE, BSTUMSETZUNG_TITLE);
        titleKeyMap.put(ElementFactory.CLIENT_TYPE, CLIENT_TITLE);
        titleKeyMap.put(ElementFactory.GEBAEUDE_TYPE, GEBAEUDE_TITLE);
        titleKeyMap.put(ElementFactory.GEFAEHRDUNGSUMSETZUNG_TYPE, GEFAEHRDUNGSUMSETZUNG_TITLE);
        titleKeyMap.put(ElementFactory.ITVERBUND_TYPE, ITVERBUND_TITLE);
        titleKeyMap.put(ElementFactory.MNUMS_TYPE, MNUMS_TITLE);
        titleKeyMap.put(ElementFactory.NETZKOMPONENTE_TYPE, NETZKOMPONENTE_TITLE);
        titleKeyMap.put(ElementFactory.PERSON_ITBP_TYPE, PERSON_ISO_TITLE);
        titleKeyMap.put(ElementFactory.RAUM_TYPE, RAUM_TITLE);
        titleKeyMap.put(ElementFactory.SERVER_TYPE, SERVER_TITLE);
        titleKeyMap.put(ElementFactory.SONSTIT_TYPE, SONSTIT_TITLE);
        titleKeyMap.put(ElementFactory.TKKOMPONENTE_TYPE, TKKOMPONENTE_TITLE);
        TITLE_KEY_MAP = Collections.unmodifiableMap(titleKeyMap);

        // Titles which are the same for all elements of a specific type
        Map<String, String> titleStaticMap = new HashMap<>();
        titleStaticMap.put(ElementFactory.GEBAEUDEKATEGORIE_TYPE, "Gebäude");
        titleStaticMap.put(ElementFactory.SONSTITKATEGORIE_TYPE, "IT-Systeme: Netzkomponenten");
        titleStaticMap.put(ElementFactory.SERVERKATEGORIE_TYPE, "IT-Systeme: Server");
        titleStaticMap.put(ElementFactory.NETZKATEGORIE_TYPE, "Netzwerkverbindungen");
        titleStaticMap.put(ElementFactory.RAEUMEKATEGORIE_TYPE, "Räume");
        titleStaticMap.put(ElementFactory.TKKATEGORIE_TYPE, "IT-Systeme: TK-Komponenten");
        titleStaticMap.put(ElementFactory.PERSONKATEGORIE_TYPE, "Mitarbeiter");
        titleStaticMap.put(ElementFactory.ANWENDUNGENKATEGORIE_TYPE, "Anwendungen");
        titleStaticMap.put(ElementFactory.CLIENTSKATEGORIE_TYPE, "IT-Systeme: Clients");
        titleStaticMap.put(ElementFactory.RISKANALYSIS_TYPE, "Risikoanalyse");
        titleStaticMap.put(ElementFactory.STELLUNGNAHMEDSB_TYPE, "Stelungname DSB");
        titleStaticMap.put(ElementFactory.VERANTWORTLICHESTELLE_TYPE, "Verantwortlichestelle");
        titleStaticMap.put(ElementFactory.VERARBEITUNGSANGABEN_TYPE, "Verarbeitungsangaben");
        titleStaticMap.put(ElementFactory.DATENVERARBEITUNG_TYPE, "Datenverarbeitung");
        titleStaticMap.put(ElementFactory.PERSONENGRUPPEN_TYPE, "Mitarbeiter");
        TITLE_STATIC_MAP = Collections.unmodifiableMap(titleStaticMap);

    }

    private TitleAdapter() {
        // do not instantiate this class, use public final methods
    }

    /**
     * @param syncObject
     *            An object from a VNA
     * @param mapObject
     *            A map object element from a VNA
     * @return A title from a given SyncObject and a MapObjectType.
     */
    public static String getTitle(SyncObject syncObject, MapObjectType mapObject) {
        String id = mapObject.getIntId();
        String titleKey = getTitleKey(id);
        if (titleKey == null) {
            return getStaticTitle(id);
        }
        MapAttributeType mapAttribute = getMapAttribute(mapObject, titleKey);
        return (mapAttribute == null) ? null
                : getAttribute(syncObject.getSyncAttribute(), mapAttribute.getExtId());

    }

    private static String getStaticTitle(String id) {
        String staticTitle = TITLE_STATIC_MAP.get(id);
        if (staticTitle == null) {
            LOG.warn("No title property found for id {}. Using id as title.", id);
            return id;
        }
        return staticTitle;
    }

    /**
     * @param typeId
     *            An object type id
     * @return The property type of the title for a given object type.
     */
    public static String getTitleKey(String typeId) {
        String key = TITLE_KEY_MAP.get(typeId);
        if (key == null) {
            LOG.info("No title key found for type: {}", typeId);
        }
        return key;
    }

    /**
     * @param syncAttributeList
     *            A list of sync attributes from a VNA
     * @param propertyId
     * @return The value of the property with the given id in the list of
     *         attributes
     */
    public static String getAttribute(List<SyncAttribute> syncAttributeList, String propertyId) {
        for (SyncAttribute attribute : syncAttributeList) {
            if (propertyId.equals(attribute.getName())) {
                return attribute.getValue().get(0);
            }
        }
        LOG.warn("No property found for property type id: {}", propertyId);
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
