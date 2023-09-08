/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jochen Kemnade
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

class V68__add_control_implementation extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).execute("""

    create table control_implementation (
        db_id bigint not null,
        description varchar(65535),
        id uuid not null,
        requirement_implementations jsonb,
        control_id varchar(255) not null,
        owner_db_id varchar(255) not null,
        person_id varchar(255),
        primary key (db_id)
    );

      create table requirement_implementation (
        db_id bigint not null,
        id uuid not null,
        implementation_statement varchar(65535),
        origination varchar(255) check (origination in ('ORGANIZATION','INHERITED','SYSTEM_SPECIFIC')),
        status varchar(255) check (status in ('UNKNOWN','YES','NO','PARTIAL','N_A')),
        control_id varchar(255) not null,
        origin_db_id varchar(255) not null,
        person_id varchar(255),
        primary key (db_id)
    );

    create sequence seq_control_impl start with 1 increment by 10;

    create sequence seq_req_impl start with 1 increment by 10;

    alter table if exists control_implementation
       add constraint FK_owner
       foreign key (owner_db_id)
       references element;

    alter table if exists control_implementation
       add constraint FK_control
       foreign key (control_id)
       references element;

    alter table if exists control_implementation
       add constraint FK_responsible
       foreign key (person_id)
       references element;

    alter table if exists requirement_implementation
       add constraint FK_origin
       foreign key (origin_db_id)
       references element;

    alter table if exists requirement_implementation
       add constraint FK_control
       foreign key (control_id)
       references element;

    alter table if exists requirement_implementation
       add constraint FK_responsible
       foreign key (person_id)
       references element;

    alter table if exists control_implementation
       drop constraint if exists UK_control_implementation_id;

    alter table if exists control_implementation
       add constraint UK_control_implementation_id unique (id);

    alter table if exists requirement_implementation
       drop constraint if exists UK_requirement_implementation_id;

    alter table if exists requirement_implementation
       add constraint UK_requirement_implementation_id unique (id);

  alter table if exists control_implementation
       drop constraint if exists UK_owner_control;

    alter table if exists control_implementation
       add constraint UK_owner_control unique (owner_db_id, control_id);

    alter table if exists requirement_implementation
       drop constraint if exists UK_origin_control;

    alter table if exists requirement_implementation
       add constraint UK_origin_control unique (origin_db_id, control_id);
""")
    }
}
