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

class V123__add_ri_properties extends BaseJavaMigration {

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
            add column cost integer,
            add column implementation_date date,
            add column implemented_by_db_id uuid,
            add column document_db_id uuid,
            add column last_revision_date date,
            add column last_revision_by_db_id uuid,
            add column next_revision_date date,
            add column next_revision_by_db_id uuid,
            add constraint fk_implemented_by_id
                foreign key (implemented_by_db_id)
                references $referencedTable(db_id),
            add constraint fk_document_id
                foreign key (document_db_id)
                references $referencedTable(db_id),
            add constraint fk_last_revision_by_id
                foreign key (last_revision_by_db_id)
                references $referencedTable(db_id),
            add constraint fk_next_revision_by_id
                foreign key (next_revision_by_db_id)
                references $referencedTable(db_id);

            create index idx_${table}_implemented_by_db_id
                on ${table}(implemented_by_db_id);
            create index idx_${table}_document_db_id
                on ${table}(document_db_id);
            create index idx_${table}_last_revision_by_db_id
                on ${table}(last_revision_by_db_id);
            create index idx_${table}_next_revision_by_db_id
                on ${table}(next_revision_by_db_id);
""".toString())
    }
}
