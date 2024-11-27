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

class V102__add_RI_tailoring_refs extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute("""
            alter table profile_tailoring_reference
                add column status varchar(255) check (status in ('UNKNOWN','YES','NO','PARTIAL','N_A')),
                add column implementation_statement varchar(65535),
                add column implementation_until date,
                drop constraint profile_tailoring_reference_reference_type_range,
                add constraint profile_tailoring_reference_reference_type_range
                    check ((reference_type >= 0) AND (reference_type <= 11));
            alter table catalog_tailoring_reference
                drop constraint catalog_tailoring_reference_reference_type_range,
                add constraint catalog_tailoring_reference_reference_type_range
                    check ((reference_type >= 0) AND (reference_type <= 11));
        """.toString())
        }
    }
}
