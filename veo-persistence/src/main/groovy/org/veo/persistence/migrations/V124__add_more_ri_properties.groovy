/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Jochen Kemnade
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

class V124__add_more_ri_properties extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            migrateTable('profile_tailoring_reference', 'profile_item', it)
            migrateTable('requirement_implementation', 'element', it)
        }
    }

    private static boolean migrateTable(String table, String referencedTable, Sql context) {
        context.execute("""
            alter table $table
            add column assessment_date date,
            add column assessment_by_db_id uuid,
            add constraint fk_assessment_by_id
                foreign key (assessment_by_db_id)
                references $referencedTable(db_id);

            create index idx_${table}_assessment_by_db_id
                on ${table}(assessment_by_db_id);
""".toString())
    }
}
