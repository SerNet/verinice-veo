/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.veo.service.ie;

import org.veo.model.Element;

/**
 * @author Daniel Murygin
 */
public class ElementFactory {

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
    public static final String PERSON_TYPE = "person";
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
    
    private ElementFactory() {
    	// Do not instantiate this class, use final methods
    }
    
    public static Element newInstance(String typeId) {
    	Element element = new Element();
    	element.setTypeId(typeId);
    	return element;
    }
    
}
