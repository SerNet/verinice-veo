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

class V75__persist_inspections extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        def sql = new Sql(context.connection)
        sql.execute("""
            create table inspection_set (
               id int8 not null,
                inspections jsonb not null,
                primary key (id)
            );
            create sequence seq_inspection_sets start 1 increment 50;
        """)
        addReferenceCol(sql, "domain")
        addReferenceCol(sql, "domainTemplate")
    }

    private static void addReferenceCol(Sql sql, String domainTable) {
        sql.execute("""
            alter table $domainTable
                    add column inspection_set_id int8;

            alter table $domainTable
                   add constraint FK_inspection_set_id
                   foreign key (inspection_set_id)
                   references inspection_set;

            create index IDX_${domainTable.toUpperCase()}_INSPECTION_SET_ID on $domainTable(inspection_set_id);
        """.toString())

        sql.eachRow("""
            select d.db_id, d.name, ds.decisions::varchar <> '{}' as has_decisions
            from $domainTable as d
                     inner join decision_set ds on ds.id = d.decision_set_id;
         """.toString()) { domain ->
                    def inspectionSetId = sql
                            .executeInsert(
                            [inspections: domain.name == "DS-GVO" && domain.has_decisions ? dsgvoInspections : "{}"],
                            "INSERT INTO inspection_set (id, inspections) VALUES (nextval('seq_inspection_sets'), :inspections::jsonb)"
                            )
                            .first()
                            .first()
                    sql.execute([
                        id: domain.db_id,
                        inspectionSetId: inspectionSetId,
                    ], "UPDATE $domainTable SET inspection_set_id = :inspectionSetId WHERE db_id = :id")
                }
    }

    private static dsgvoInspections = """
    {
      "dpiaMissing": {
        "severity": "WARNING",
        "description": {
          "de": "Datenschutz-Folgenabschätzung wurde nicht durchgeführt, sie ist aber erforderlich.",
          "en": "Data Protection Impact Assessment was not carried out, but it is mandatory."
        },
        "elementType": "process",
        "elementSubType": "PRO_DataProcessing",
        "condition": {
          "type": "and",
          "operands": [
            {
              "type": "decisionResultValue",
              "decision": "piaMandatory"
            },
            {
              "type": "equals",
              "left": {
                "type": "partCount",
                "partSubType": "PRO_DPIA"
              },
              "right": {
                "type": "constant",
                "value": 0
              }
            }
          ]
        },
        "suggestions": [
          {
            "type": "addPart",
            "partSubType": "PRO_DPIA"
          }
        ]
      }
    }
    """
}
