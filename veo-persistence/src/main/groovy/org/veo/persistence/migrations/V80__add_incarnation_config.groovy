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

class V80__add_incarnation_config extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            migrateTable("domain", it)
            migrateTable("domaintemplate", it)
        }
    }

    private static boolean migrateTable(String table, Sql context) {
        context.execute("""
            alter table $table
                add column incarnation_configuration jsonb;
            update $table
                set incarnation_configuration =
                CASE WHEN name = 'IT-Grundschutz'
                    THEN '{"mode": "DEFAULT", "useExistingIncarnations": "ALWAYS", "exclude": ["COMPOSITE", "LINK", "LINK_EXTERNAL"]}'::jsonb
                    ELSE '{"mode": "DEFAULT", "useExistingIncarnations": "FOR_REFERENCED_ITEMS"}'::jsonb
                END;
            alter table $table
                alter column incarnation_configuration set not null;
        """.toString())
    }
}
