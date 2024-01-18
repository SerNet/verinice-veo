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

class V77__create_userconfiguration extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).execute("""

    create table userconfiguration (
        db_id varchar(255) not null,
        application_id varchar(255) not null,
        configuration jsonb,
        user_name varchar(255) not null,
        client_id varchar(255) not null,
        primary key (db_id)
    );

    alter table if exists userconfiguration
       add constraint FK_client_id
       foreign key (client_id)
       references client;

    create unique index IDX_APPLICATION_AND_USER_AND_CLIENT
        on userconfiguration(application_id,user_name, client_id);

    alter table userconfiguration
        add constraint UK_application_user_client
        unique using index IDX_APPLICATION_AND_USER_AND_CLIENT;

 """)
    }
}
