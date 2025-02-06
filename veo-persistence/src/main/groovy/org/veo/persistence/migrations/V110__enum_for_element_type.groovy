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

class V110__enum_for_element_type extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute("""

    CREATE TYPE element_type AS ENUM (
        'ASSET',
        'CONTROL',
        'DOCUMENT',
        'INCIDENT',
        'PERSON',
        'PROCESS',
        'SCENARIO',
        'SCOPE'
    );

    UPDATE element SET dtype = UPPER(dtype);
    ALTER TABLE element
        ALTER COLUMN dtype TYPE element_type
        USING dtype::text::element_type;

    UPDATE catalogitem SET elementtype = UPPER(elementtype);
    ALTER TABLE catalogitem
        ALTER COLUMN elementtype TYPE element_type
        USING elementtype::text::element_type;

    UPDATE profile_item SET elementtype = UPPER(elementtype);
    ALTER TABLE profile_item
        ALTER COLUMN elementtype TYPE element_type
        USING elementtype::text::element_type;
        """.toString())
        }
    }
}