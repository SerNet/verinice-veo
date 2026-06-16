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

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

import groovy.sql.Sql

class V57__persist_decisions extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        def sql = new Sql(context.connection)
        sql.execute("""
            create table decision_set (
               id int8 not null,
                decisions jsonb not null,
                primary key (id)
            );
            create sequence seq_decision_sets start 1 increment 50;
        """)
        addReferenceCol(sql, "domain")
        addReferenceCol(sql, "domainTemplate")
    }

    private static void addReferenceCol(Sql sql, String domainTable) {
        sql.execute("""
            alter table $domainTable
                    add column decision_set_id int8;

            alter table $domainTable
                   add constraint FK_decision_set_id
                   foreign key (decision_set_id)
                   references decision_set;

            create index IDX_${domainTable.toUpperCase()}_DECISION_SET_ID on $domainTable(decision_set_id);
        """.toString())

        sql.eachRow("SELECT db_id, name, templateversion FROM $domainTable;".toString()) { domain ->
            def decisionSetId = sql
                    .executeInsert(
                    [decisions: domain.name == "DS-GVO" && domain.templateversion == "1.9.12" ? dsgvoDecisions : "{}"],
                    "INSERT INTO decision_set (id, decisions) VALUES (nextval('seq_decision_sets'), :decisions::jsonb)"
                    )
                    .first()
                    .first()
            sql.execute([
                id: domain.db_id,
                decisionSetId: decisionSetId,
            ], "UPDATE $domainTable SET decision_set_id = :decisionSetId WHERE db_id = :id")
        }
    }

    private static dsgvoDecisions = """
      {
        "piaMandatory": {
          "name": {
            "en": "Data Protection Impact Assessment mandatory",
            "de": "Datenschutz-Folgenabschätzung verpflichtend"
          },
          "elementType": "process",
          "elementSubType": "PRO_DataProcessing",
          "rules": [
            {
              "description": {
                "en": "Missing risk analysis",
                "de": "Fehlende Risikoanalyse"
              },
              "conditions": [
                {
                  "inputProvider": {
                    "type": "maxRisk"
                  },
                  "inputMatcher": {
                    "type": "isNull"
                  }
                }
              ]
            },
            {
              "output": false,
              "description": {
                "en": "Processing on list of the kinds of processing operations not subject to a Data Protection Impact Assessment",
                "de": "VT auf Negativliste"
              },
              "conditions": [
                {
                  "inputProvider": {
                    "type": "customAspectAttributeValue",
                    "customAspect": "process_privacyImpactAssessment",
                    "attribute": "process_privacyImpactAssessment_listed"
                  },
                  "inputMatcher": {
                    "type": "equals",
                    "comparisonValue": "process_privacyImpactAssessment_listed_negative"
                  }
                }
              ]
            },
            {
              "output": false,
              "description": {
                "en": "Part of a joint processing",
                "de": "Gemeinsame VT"
              },
              "conditions": [
                {
                  "inputProvider": {
                    "type": "customAspectAttributeValue",
                    "customAspect": "process_privacyImpactAssessment",
                    "attribute": "process_privacyImpactAssessment_processingOperationAccordingArt35"
                  },
                  "inputMatcher": {
                    "type": "equals",
                    "comparisonValue": true
                  }
                }
              ]
            },
            {
              "output": false,
              "description": {
                "en": "Other exclusions",
                "de": "Anderer Ausschlusstatbestand"
              },
              "conditions": [
                {
                  "inputProvider": {
                    "type": "customAspectAttributeValue",
                    "customAspect": "process_privacyImpactAssessment",
                    "attribute": "process_privacyImpactAssessment_otherExclusions"
                  },
                  "inputMatcher": {
                    "type": "equals",
                    "comparisonValue": true
                  }
                }
              ]
            },
            {
              "output": true,
              "description": {
                "en": "High risk present",
                "de": "Hohes Risiko vorhanden"
              },
              "conditions": [
                {
                  "inputProvider": {
                    "type": "maxRisk"
                  },
                  "inputMatcher": {
                    "type": "greaterThan",
                    "comparisonValue": 1
                  }
                }
              ]
            },
            {
              "output": true,
              "description": {
                "en": "Processing on list of the kinds of processing operations subject to a Data Protection Impact Assessment",
                "de": "VT auf Positivliste"
              },
              "conditions": [
                {
                  "inputProvider": {
                    "type": "customAspectAttributeValue",
                    "customAspect": "process_privacyImpactAssessment",
                    "attribute": "process_privacyImpactAssessment_listed"
                  },
                  "inputMatcher": {
                    "type": "equals",
                    "comparisonValue": "process_privacyImpactAssessment_listed_positive"
                  }
                }
              ]
            },
            {
              "output": true,
              "description": {
                "en": "Two or more criteria applicable",
                "de": "Mehrere Kriterien zutreffend"
              },
              "conditions": [
                {
                  "inputProvider": {
                    "type": "customAspectAttributeSize",
                    "customAspectType": "process_privacyImpactAssessment",
                    "attributeType": "process_privacyImpactAssessment_processingCriteria"
                  },
                  "inputMatcher": {
                    "type": "greaterThan",
                    "comparisonValue": 1
                  }
                }
              ]
            },
            {
              "description": {
                "en": "DPIA-relevant attributes incomplete",
                "de": "DSFA-relevante Attribute unvollständig"
              },
              "conditions": [
                {
                  "inputProvider": {
                    "type": "customAspectAttributeValue",
                    "customAspect": "process_privacyImpactAssessment",
                    "attribute": "process_privacyImpactAssessment_processingCriteria"
                  },
                  "inputMatcher": {
                    "type": "isNull"
                  }
                },
                {
                  "inputProvider": {
                    "type": "customAspectAttributeValue",
                    "customAspect": "process_privacyImpactAssessment",
                    "attribute": "process_privacyImpactAssessment_listed"
                  },
                  "inputMatcher": {
                    "type": "isNull"
                  }
                },
                {
                  "inputProvider": {
                    "type": "customAspectAttributeValue",
                    "customAspect": "process_privacyImpactAssessment",
                    "attribute": "process_privacyImpactAssessment_otherExclusions"
                  },
                  "inputMatcher": {
                    "type": "isNull"
                  }
                },
                {
                  "inputProvider": {
                    "type": "customAspectAttributeValue",
                    "customAspect": "process_privacyImpactAssessment",
                    "attribute": "process_privacyImpactAssessment_processingOperationAccordingArt35"
                  },
                  "inputMatcher": {
                    "type": "isNull"
                  }
                }
              ]
            }
          ],
          "defaultResultValue": false
        }
      }
    """
}
