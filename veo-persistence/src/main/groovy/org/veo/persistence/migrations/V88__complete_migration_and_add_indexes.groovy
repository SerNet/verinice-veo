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

class V88__complete_migration_and_add_indexes extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute("""
                create index IDX_CONTROL_IMPLEMENTATION_OWNER_DB_ID on control_implementation(owner_db_id);
                create index IDX_REQUIREMENT_IMPLEMENTATION_ORIGIN_DB_ID on requirement_implementation(origin_db_id);

                create index IDX_CUSTOM_LINK_IDX_SOURCE_ID on customlink(source_id);
                create index IDX_CUSTOM_LINK_IDX_TARGET_ID on customlink(target_id);

                alter table custom_aspect drop column source_id;
                alter table custom_aspect drop column target_id;
        """.toString())
        }
    }
}
