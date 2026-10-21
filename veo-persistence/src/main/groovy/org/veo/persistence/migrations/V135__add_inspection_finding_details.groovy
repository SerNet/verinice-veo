/*
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
 */
package org.veo.persistence.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

import groovy.sql.Sql

class V135__add_inspection_finding_details extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute("""
            CREATE TYPE severity AS ENUM (
                'HINT',
                'WARNING',
                'ERROR'
            );

            alter table inspection_finding_data
                    add column severity severity not null default 'HINT',
                    add column responsible_person_id uuid,
                    add column task_summary jsonb;

            alter table inspection_finding_data
                    alter column severity drop default;
        
            create index IDX_INSPECTION_FINDING_DOMAIN_UNIT_PERSON on inspection_finding_data(domain_id, unit_id, responsible_person_id);
            """)
        }
    }
}
