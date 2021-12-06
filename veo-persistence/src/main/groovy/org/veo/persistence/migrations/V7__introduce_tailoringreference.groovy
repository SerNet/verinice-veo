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

class V7__introduce_tailoringreference extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).execute("""

    create table customlinkdescriptor (
       db_id varchar(255) not null,
        attributes jsonb,
        type varchar(255),
        source_id varchar(255),
        target_id varchar(255),
        primary key (db_id)
    );

    alter table tailoringreference
       add column dtype varchar(31) not null default 'tailoringreference';

    alter table tailoringreference
       add column external_link_db_id varchar(255);

    alter table tailoringreference
       add constraint FK_customlinkdescriptor_external_link_db_id
       foreign key (external_link_db_id)
       references customlinkdescriptor;

    alter table customlinkdescriptor
       add constraint FK_element_source_id
       foreign key (source_id)
       references element;

    alter table customlinkdescriptor
       add constraint FK_element_target_id
       foreign key (target_id)
       references element;
""")
    }
}
