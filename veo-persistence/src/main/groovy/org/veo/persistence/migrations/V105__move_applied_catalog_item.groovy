/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Jonas Jordan
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

class V105__move_applied_catalog_item extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {

        new Sql(context.connection).with {
            execute("""
ALTER TABLE element_domain_association ADD column applied_catalog_item_db_id uuid;
ALTER TABLE element_domain_association ADD CONSTRAINT FK_applied_catalog_items_db_id FOREIGN KEY (applied_catalog_item_db_id) REFERENCES catalogitem(db_id);

UPDATE element_domain_association eda
SET applied_catalog_item_db_id = eaci.applied_catalog_items_db_id
FROM element_applied_catalog_items eaci
JOIN catalogitem ci ON eaci.applied_catalog_items_db_id = ci.db_id
WHERE eda.owner_db_id = eaci.element_db_id
AND eda.domain_id = ci.domain_db_id;

DROP TABLE element_applied_catalog_items;
        """)
        }
    }
}