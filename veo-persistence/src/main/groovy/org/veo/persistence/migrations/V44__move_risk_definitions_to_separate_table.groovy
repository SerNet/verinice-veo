/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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

class V44__move_risk_definitions_to_separate_table extends BaseJavaMigration{
    @Override
    void migrate(Context context) throws Exception {
        def sql = new Sql(context.connection)
        sql.execute("""
            create table risk_definition_set (
               id int8 not null,
                risk_definitions jsonb not null,
                primary key (id)
            );
            create sequence seq_risk_definition_sets start 1 increment 50;
        """)
        migrateTable(sql, "domain")
        migrateTable(sql, "domainTemplate")
    }

    private static void migrateTable(Sql sql, String domainTable) {
        sql.execute("""
            alter table $domainTable
                    add column risk_definition_set_id int8;

            alter table $domainTable
                   add constraint FK_risk_definition_set_id
                   foreign key (risk_definition_set_id)
                   references risk_definition_set;

            create index IDX_${domainTable.toUpperCase()}_RISK_DEFINITION_SET_ID on $domainTable(risk_definition_set_id);
        """.toString())

        sql.eachRow("SELECT db_id, risk_definitions FROM $domainTable;".toString()) { domain ->
            def riskDefinitionSetId = sql
                    .executeInsert(
                    [riskDefinitions: domain.risk_definitions],
                    "INSERT INTO risk_definition_set (id, risk_definitions) VALUES (nextval('seq_risk_definition_sets'), :riskDefinitions)"
                    )
                    .first()
                    .first()
            sql.execute([
                id: domain.db_id,
                riskDefinitionSetId: riskDefinitionSetId,
            ], "UPDATE $domainTable SET risk_definition_set_id = :riskDefinitionSetId WHERE db_id = :id")
        }

        sql.execute("""
            alter table $domainTable drop column risk_definitions;
            alter table $domainTable alter column risk_definition_set_id set not null;
        """.toString())
    }
}
