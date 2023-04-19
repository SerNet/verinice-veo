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

class V56__add_custom_link_table extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute("""
                create table customlink (
                    db_id varchar(255) not null,
                    attributes jsonb,
                    type varchar(255),
                    source_id varchar(255),
                    target_id varchar(255),
                    domain_id varchar(36),
                    primary key (db_id)
                );

                create unique index IDX_CUSTOMLINK_SOURCE_TARGET_TYPE_DOMAIN
                    on customlink(source_id, target_id, type, domain_id);

                alter table customlink
                    add constraint UK_source_target_type_domain
                    unique using index IDX_CUSTOMLINK_SOURCE_TARGET_TYPE_DOMAIN;

                insert into customlink(db_id, attributes, type, source_id, target_id, domain_id)
                select db_id, attributes, type, source_id, target_id, domain_id from custom_aspect where dtype = 'customlink';

                delete from custom_aspect where dtype = 'customlink';

                alter table custom_aspect
                    drop column dtype;

                alter table custom_aspect
                    alter column owner_db_id set not null;
            """)
        }
    }
}
