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

class V67__natural_sorting_for_element extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).execute("""
            create collation natural_sort (locale = 'en@colNumeric=yes', provider = 'icu');

            alter table element
                alter column abbreviation type varchar(255) collate natural_sort,
                alter column designator type varchar(255) collate natural_sort,
                alter column name type varchar(255) collate natural_sort;
""")
    }
}