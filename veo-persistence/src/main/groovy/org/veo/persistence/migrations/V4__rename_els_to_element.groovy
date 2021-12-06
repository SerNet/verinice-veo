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

class V4__rename_els_to_element extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).execute("""

    alter table entitylayersupertype rename to element;

    alter table entitylayersupertype_applied_catalog_items rename to element_applied_catalog_items;
    alter table element_applied_catalog_items
        rename column entitylayersupertype_db_id to element_db_id;
    alter table element_applied_catalog_items
        rename constraint FK_entitylayersupertype_db_id to FK_element_db_id;

    alter table entitylayersupertype_domains rename to element_domains;
    alter table element_domains
        rename column entitylayersupertype_db_id to element_db_id;
    alter table element_domains
        rename constraint FK_entitylayersupertype_db_id to FK_element_db_id;

""")
    }
}
