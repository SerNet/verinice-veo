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

class V129__remove_ri_assessment extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            migrateTable('profile_tailoring_reference', it)
            migrateTable('requirement_implementation', it)
        }
    }

    private static boolean migrateTable(String table, Sql context) {
        context.execute("""
            drop index idx_${table}_assessment_by_db_id;

            alter table $table
            drop column assessment_date,
            drop constraint fk_assessment_by_id,
            drop column assessment_by_db_id;

""".toString())
    }
}
