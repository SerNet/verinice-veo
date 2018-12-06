/*******************************************************************************
 * Copyright (c) 2018 Daniel Murygin.
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
 ******************************************************************************/
package org.veo.service.ie;

import org.veo.model.Element;

/**
 * @author Daniel Murygin
 */
public class ElementFactory {

    // Type ids of ISO27000 elements from SNCA.xml
    public static final String ASSET_GROUP_TYPE = "assetgroup";
    public static final String ASSET_TYPE = "asset";
    public static final String AUDIT_GROUP_TYPE = "auditgroup";
    public static final String AUDIT_TYPE = "audit";
    public static final String CONTROL_GROUP_TYPE = "controlgroup";
    public static final String CONTROL_TYPE = "control";
    public static final String DOCUMENT_GROUP_TYPE = "document_group";
    public static final String DOCUMENT_TYPE = "document";
    public static final String EVIDENCE_GROUP_TYPE = "evidence_group";
    public static final String EVIDENCE_TYPE = "evidence";
    public static final String EXCEPTION_GROUP_TYPE = "exceptiongroup";
    public static final String EXCEPTION_TYPE = "exception";
    public static final String FINDING_GROUP_TYPE = "finding_group";
    public static final String FINDING_TYPE = "finding";
    public static final String INCIDENT_GROUP_TYPE = "incident_group";
    public static final String INCIDENT_TYPE = "incident";
    public static final String INTERVIEW_GROUP_TYPE = "interview_group";
    public static final String INTERVIEW_TYPE = "interview";
    public static final String ORGANIZATION_TYPE = "org";
    public static final String PERSON_GROUP_TYPE = "persongroup";
    public static final String PERSON_ISO_TYPE = "person-iso";
    public static final String PROCESS_GROUP_TYPE = "process_group";
    public static final String PROCESS_TYPE = "process";
    public static final String RECORD_GROUP_TYPE = "record_group";
    public static final String RECORD_TYPE = "record";
    public static final String REQUIREMENT_GROUP_TYPE = "requirementgroup";
    public static final String REQUIREMENT_TYPE = "requirement";
    public static final String RESPONSE_GROUP_TYPE = "response_group";
    public static final String RESPONSE_TYPE = "response";
    public static final String SAMT_TOPIC_TYPE = "samt_topic";
    public static final String SCENARIO_GROUP_TYPE = "incident_scenario_group";
    public static final String SCENARIO_TYPE = "incident_scenario";
    public static final String THREAT_GROUP_TYPE = "threat_group";
    public static final String THREAT_TYPE = "threat";
    public static final String VULNERABILITY_GROUP_TYPE = "vulnerability_group";
    public static final String VULNERABILITY_TYPE = "vulnerability";

    // Type ids of ITBP elements from SNCA.xml
    public static final String RAUM_TYPE = "raum";
    public static final String SERVER_TYPE = "server";
    public static final String TKKOMPONENTE_TYPE = "tkkomponente";
    public static final String NETZKOMPONENTE_TYPE = "netzkomponente";
    public static final String ITVERBUND_TYPE = "itverbund";
    public static final String CLIENT_TYPE = "client";
    public static final String SONSTIT_TYPE = "sonstit";
    public static final String PERSON_ITBP_TYPE = "person";
    public static final String GEBAEUDE_TYPE = "gebaeude";
    public static final String BSTUMSETZUNG_TYPE = "bstumsetzung";
    public static final String GEFAEHRDUNGSUMSETZUNG_TYPE = "gefaehrdungsumsetzung";
    public static final String MNUMS_TYPE = "mnums";
    public static final String ANWENDUNG_TYPE = "anwendung";
    public static final String GEBAEUDEKATEGORIE_TYPE = "gebaeudekategorie";
    public static final String SONSTITKATEGORIE_TYPE = "sonstitkategorie";
    public static final String SERVERKATEGORIE_TYPE = "serverkategorie";
    public static final String NETZKATEGORIE_TYPE = "netzkategorie";
    public static final String RAEUMEKATEGORIE_TYPE = "raeumekategorie";
    public static final String TKKATEGORIE_TYPE = "tkkategorie";
    public static final String PERSONKATEGORIE_TYPE = "personkategorie";
    public static final String ANWENDUNGENKATEGORIE_TYPE = "anwendungenkategorie";
    public static final String CLIENTSKATEGORIE_TYPE = "clientskategorie";
    public static final String RISKANALYSIS_TYPE = "riskanalysis";
    public static final String STELLUNGNAHMEDSB_TYPE = "stellungnahmedsb";
    public static final String VERANTWORTLICHESTELLE_TYPE = "verantwortlichestelle";
    public static final String VERARBEITUNGSANGABEN_TYPE = "verarbeitungsangaben";
    public static final String DATENVERARBEITUNG_TYPE = "datenverarbeitung";
    public static final String PERSONENGRUPPEN_TYPE = "personengruppen";

    private ElementFactory() {
        // Do not instantiate this class, use final methods
    }

    public static Element newInstance(String typeId) {
        Element element = new Element();
        element.setTypeId(typeId);
        return element;
    }

}
