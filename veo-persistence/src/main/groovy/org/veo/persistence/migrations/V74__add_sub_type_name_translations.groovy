/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.persistence.migrations

import java.sql.ResultSet

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

import com.fasterxml.jackson.databind.ObjectMapper

import groovy.sql.Sql

class V74__add_sub_type_name_translations extends BaseJavaMigration {
    private om = new ObjectMapper()

    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            def translationCache = [:]
            query("select db_id, element_type, sub_types, translations from element_type_definition;") { ResultSet row ->
                while (row.next()) {
                    def elementType = row.getString("element_type")
                    def subTypeDefinitionsJson = row.getString("sub_types")
                    def translationsJson = row.getString("translations")
                    def cacheKey = elementType + subTypeDefinitionsJson + translationsJson
                    execute([
                        id: row.getString("db_id"),
                        translations: translationCache.computeIfAbsent(cacheKey) { addTranslations(elementType, subTypeDefinitionsJson, translationsJson) },
                    ], """
                        update element_type_definition
                            set translations = :translations::jsonb
                            where db_id = :id
                            """)
                }
            }
        }
    }

    String addTranslations(String elementType, String subTypeDefinitionsJson, String translationsJson) {
        om.writeValueAsString(addTranslations(elementType, om.readValue(subTypeDefinitionsJson, Map), om.readValue(translationsJson, Map)))
    }

    Map addTranslations(String elementType, Map subTypeDefinitions, Map translations) {
        if (translations.de == null) {
            translations.de = [:]
        }
        if (translations.en == null) {
            translations.en = [:]
        }
        subTypeDefinitions.forEach { subTypeKey, definition ->
            translations.de["${elementType}_${subTypeKey}_singular"] = defaultTranslations[subTypeKey]?.de?.get(0) ?: subTypeKey
            translations.de["${elementType}_${subTypeKey}_plural"] = defaultTranslations[subTypeKey]?.de?.get(1) ?: subTypeKey
            translations.en["${elementType}_${subTypeKey}_singular"] = defaultTranslations[subTypeKey]?.en?.get(0) ?: subTypeKey
            translations.en["${elementType}_${subTypeKey}_plural"] = defaultTranslations[subTypeKey]?.en?.get(1) ?: subTypeKey
        }
        translations
    }

    def defaultTranslations = [
        AST_Datatype: [
            de: ["Datenart", "Datenarten"],
            en: ["Datatype", "Datatypes"],
        ],
        AST_Application: [
            de: ["Anwendung", "Anwendungen"],
            en: ["Application", "Applications"]
        ],
        'AST_IT-System': [
            de: ["IT-System", "IT-Systeme"],
            en: ["IT-system", "IT-systems"],
        ],
        CTL_TOM: [
            de: ["TOM", "TOMs"],
            en: ["TOM", "TOMs"],
        ],
        DOC_Contract: [
            de: ["Vertrag", "Verträge"],
            en: ["Contract", "Contracts"]
        ],
        DOC_Document: [
            de: ["Dokument", "Dokumente"],
            en: ["Document", "Documents"]
        ],
        DOC_RequestDataSubject: [
            de: [
                "Betroffenenanfrage",
                "Betroffenenanfragen"
            ],
            en: [
                "Request data subject",
                "Request data subjects"
            ],
        ],
        INC_DataPrivacyIncident: [
            de: [
                "Datenschutzvorfall",
                "Datenschutzvorfälle"
            ],
            en: [
                "Data privacy incident",
                "Data privacy incidents"
            ]
        ],
        PER_DataProtectionOfficer: [
            de: [
                "Datenschutzbeauftragte",
                "Datenschutzbeauftragte"
            ],
            en: [
                "Data protection officer",
                "Data protection officers"
            ],
        ],
        PER_Person: [
            de: ["Person", "Personen"],
            en: ["Person", "Persons"],
        ],
        PRO_DataProcessing: [
            de: [
                "Verarbeitungstätigkeit",
                "Verarbeitungstätigkeiten"
            ],
            en: [
                "Data processing",
                "Data processings"
            ],
        ],
        PRO_DataTransfer: [
            de: [
                "Datenübertragung",
                "Datenübertragungen"
            ],
            en: [
                "Data transfer",
                "Data transfers"
            ],
        ],
        PRO_DPIA: [
            de: [
                "Datenschutz-Folgeabschätzung",
                "Datenschutz-Folgeabschätzungen"
            ],
            en: [
                "Data protection impact assessment",
                "Data protection impact assessments"
            ],
        ],
        SCN_Scenario: [
            de: ["Szenario", "Szenarien"],
            en: ["Scenario", "Scenarios"],
        ],
        SCP_ResponsibleBody: [
            de: [
                "Verantwortliche, Art. 4 Nr. 7 DS-GVO",
                "Verantwortliche, Art. 4 Nr. 7 DS-GVO"
            ],
            en: [
                "Controller, Art. 4 Nr.7 GDPR",
                "Controllers, Art. 4 Nr.7 GDPR"
            ],
        ],
        SCP_Processor: [
            de: [
                "Auftragsverarbeiter",
                "Auftragsverarbeiter"
            ],
            en: ["Processor", "Processors"],
        ],
        SCP_Controller: [
            de: [
                "Auftraggeber",
                "Auftraggeber"
            ],
            en: ["Controller", "Controllers"],
        ],
        SCP_JointController: [
            de: [
                "Gemeinsame Verantwortliche",
                "Gemeinsame Verantwortliche"
            ],
            en: [
                "Joint controllership",
                "Joint controllerships"
            ],
        ],
        SCP_Scope: [
            de: ["Scope", "Scopes"],
            en: ["Scope", "Scopes"],
        ],
        SCP_Institution: [
            de: [
                "Institution",
                "Institutionen"
            ],
            en: ["Institution", "Institutions"],
        ],
        SCP_InformationDomain: [
            "de": [
                "Informationsverbund",
                "Informationsverbünde"
            ],
            "en": [
                "Information domain",
                "Information domains"
            ],
        ],
        SCP_OutsourcingProvider: [
            "de": [
                "Anbieter von Outsourcing",
                "Anbieter von Outsourcing"
            ],
            "en": [
                "Outsourcing provider",
                "Outsourcing providers"
            ],
        ],
        SCP_OutsourcingUser: [
            "de": [
                "Nutzer von Outsourcing",
                "Nutzer von Outsourcing"
            ],
            "en": [
                "Outsourcing users",
                "Outsourcing users"
            ],
        ],
        PRO_BusinessProcess: [
            "de": [
                "Geschäftsprozess",
                "Geschäftsprozesse"
            ],
            "en": [
                "Business process",
                "Business processes"
            ],
        ],
        PRO_SpecialistMethodologies: [
            "de": [
                "Fachverfahren",
                "Fachverfahren"
            ],
            "en": [
                "Specialist methodology",
                "Specialist methodologies"
            ],
        ],
        CTL_Module: [
            "de": ["Baustein", "Bausteine"],
            "en": ["Module", "Modules"],
        ],
        CTL_Requirement: [
            "de": [
                "Anforderung",
                "Anforderungen"
            ],
            "en": ["Requirement", "Requirements"],
        ],
        CTL_Safeguard: [
            "de": ["Maßnahme", "Maßnahmen"],
            "en": ["Safeguard", "Safeguards"],
        ],
        SCN_AppliedThreat: [
            "de": [
                "Elementare Gefährdung",
                "Elementare Gefährdungen"
            ],
            "en": [
                "Elementary threat",
                "Elementary threats"
            ],
        ],
        DOC_NetworkPlan: [
            "de": ["Netzplan", "Netzpläne"],
            "en": [
                "Network plan",
                "Network plans"
            ],
        ],
        DOC_ReferenceDocument: [
            "de": [
                "Referenzdokument",
                "Referenzdokumente"
            ],
            "en": [
                "Reference document",
                "Reference documents"
            ],
        ],
        INC_InformationSecurityIncident: [
            "de": [
                "Informationssicherheitsvorfall",
                "Informationssicherheitsvorfälle"
            ],
            "en": [
                "Information security incident",
                "Information security incidents"
            ],
        ],
        AST_Information: [
            "de": [
                "Information",
                "Informationen"
            ],
            "en": ["Information", "Information"],
        ],
        AST_IT: [
            "de": ["IT-System", "IT-Systeme"],
            "en": ["IT-System", "IT-Systems"],
        ],
        AST_ICS: [
            "de": ["ICS-System", "ICS-Systeme"],
            "en": ["ICS-System", "ICS-Systems"],
        ],
        AST_Device: [
            "de": ["IoT-System", "IoT-Systeme"],
            "en": ["IoT-System", "IoT-Systems"],
        ],
        AST_Network: [
            "de": [
                "Kommunikationsverbindung",
                "Kommunikationsverbindungen"
            ],
            "en": ["Network", "Networks"],
        ],
        AST_Room: [
            "de": ["Raum", "Räume"],
            "en": ["Room", "Rooms"],
        ],
        SCP_Organization: [
            "de": [
                "Organisation",
                "Organisationen"
            ],
            "en": [
                "Organization",
                "Organizations"
            ],
        ],
        SCP_Supplier: [
            "de": ["Lieferant", "Lieferanten"],
            "en": ["Supplier", "Suppliers"],
        ],
        AST_Asset: [
            "de": ["Asset", "Assets"],
            "en": ["Asset", "Assets"],
        ],
        CTL_Measure: [
            "de": ["Maßnahme", "Maßnahmen"],
            "en": ["Measure", "Measures"],
        ],
        DOC_AffectedAreaAnalysis: [
            "de": [
                "Betroffenheitsanalyse",
                "Betroffenheitsanalysen"
            ],
            "en": [
                "Affected area analysis",
                "Affected area analyses"
            ],
        ],
        INC_SecurityIncident: [
            "de": [
                "Sicherheitsvorfall",
                "Sicherheitsvorfälle"
            ],
            "en": [
                "Security incident",
                "Security incidents"
            ],
        ]
    ]
}
