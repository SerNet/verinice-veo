/*
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade
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

class V134__add_inspection_finding_data extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute("""
    create table inspection_finding_data (
        db_id uuid not null,
        domain_id uuid not null CONSTRAINT fk_domain_id references domain(db_id),
        inspection_id varchar(256) not null,
        element_id uuid not null CONSTRAINT fk_element_id references element(db_id),
        unit_id uuid not null CONSTRAINT fk_unit_id references unit(db_id)
    );

    create index IDX_INSPECTION_FINDING_ELEMENT_DOMAIN on inspection_finding_data(element_id, domain_id);
    create index IDX_INSPECTION_FINDING_DOMAIN_INSPECTION_ID on inspection_finding_data(domain_id, inspection_id);
""")
        }
    }
}
