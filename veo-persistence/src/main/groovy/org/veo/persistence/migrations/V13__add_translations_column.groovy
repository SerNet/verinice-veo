/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

class V13__add_translations_column extends BaseJavaMigration {

    static def entityTypes = [
        'asset',
        'control',
        'document',
        'incident',
        'person',
        'process',
        'scenario',
        'scope'
    ]

    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.getConnection()).with { sql->
            sql.execute('alter table element_type_definition add column translations jsonb;')

            entityTypes.each {type ->
                def translations = V13__add_translations_column.getResourceAsStream("v13/${type}/lang.json").getText("UTF-8")
                sql.execute([
                    translations: translations,
                    elementType: type,
                ], "UPDATE element_type_definition set translations = :translations :: jsonb where element_type = :elementType")
            }
            sql.execute('alter table element_type_definition alter column translations set not null;')
        }
    }
}
