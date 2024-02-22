/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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

class V82__add_CI_tailoring_refs extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute("""
            alter table profile_tailoring_reference
                add column description varchar(65535),
                add column responsible_db_id varchar(36),
                add constraint fk_responsible_id
                    foreign key (responsible_db_id) references profile_item,
                drop constraint profile_tailoring_reference_reference_type_range,
                add constraint profile_tailoring_reference_reference_type_range
                    check ((reference_type >= 0) AND (reference_type <= 10));
            alter table catalog_tailoring_reference
                drop constraint catalog_tailoring_reference_reference_type_range,
                add constraint catalog_tailoring_reference_reference_type_range
                    check ((reference_type >= 0) AND (reference_type <= 10));
        """.toString())
        }
    }
}
