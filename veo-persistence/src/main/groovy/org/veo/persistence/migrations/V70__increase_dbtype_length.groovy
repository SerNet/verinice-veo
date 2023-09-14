/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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

class V70__increase_dbtype_length extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).execute("""

    alter table if exists profile_tailoring_reference
       alter column dtype type varchar(128);

    alter table if exists profile_item
       add constraint FK_owner_id
       foreign key (owner_db_id)
       references profile;

    alter table if exists profile_item
       add constraint FK_applied_catalog_item_id
       foreign key (applied_catalog_item_db_id)
       references catalogitem;

    alter table if exists profile_tailoring_reference
       add constraint FK_catalog_item_id
       foreign key (catalog_item_db_id)
       references profile_item;

    alter table if exists profile_tailoring_reference
       add constraint FK_mitigation_id
       foreign key (mitigation_db_id)
       references profile_item;

    alter table if exists profile_tailoring_reference
       add constraint FK_risk_owner_id
       foreign key (risk_owner_db_id)
       references profile_item;

    alter table if exists profile_tailoring_reference
        add constraint FK_owner_id
        foreign key (owner_db_id)
        references profile_item;

""")
    }
}
