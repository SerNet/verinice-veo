/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jochen Kemnade
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

class V103__remove_migrationExpression_from_old_definitions extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            migrateTable("domain", it)
            migrateTable("domaintemplate", it)
        }
    }

    private static boolean migrateTable(String table, Sql context) {
        context.execute("""
UPDATE $table
SET domain_migration_definition = jsonb_set(
    domain_migration_definition,
    '{migrations}',
    (
        SELECT jsonb_agg(
            jsonb_set(
                migration_step,
                '{oldDefinitions}',
                (
                    SELECT jsonb_agg(
                        old_def - 'migrationExpression'
                    )
                    FROM jsonb_array_elements(migration_step->'oldDefinitions') AS old_def
                )
            )
        )
        FROM jsonb_array_elements(domain_migration_definition->'migrations') AS migration_step
    )
)
WHERE jsonb_array_length(domain_migration_definition->'migrations') > 0;
        """.toString())
    }
}