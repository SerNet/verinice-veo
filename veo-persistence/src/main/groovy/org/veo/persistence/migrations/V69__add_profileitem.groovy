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

class V69__add_profileitem extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).execute("""

   create table profile (
        db_id varchar(255) not null,
        change_number bigint not null,
        created_at timestamp(6) with time zone not null,
        created_by varchar(255) not null,
        updated_at timestamp(6) with time zone not null,
        updated_by varchar(255) not null,
        version bigint not null,
        description varchar(65535),
        language varchar(255),
        name varchar(255),
        domain_db_id varchar(255),
        domain_template_db_id varchar(255),
        primary key (db_id)
    );

    create table profile_item (
        db_id varchar(255) not null,
        change_number bigint not null,
        created_at timestamp(6) with time zone not null,
        created_by varchar(255) not null,
        updated_at timestamp(6) with time zone not null,
        updated_by varchar(255) not null,
        version bigint not null,
        abbreviation varchar(255),
        custom_aspects jsonb,
        description varchar(65535),
        elementtype varchar(255) not null,
        name varchar(255) not null,
        namespace varchar(255),
        status varchar(255) not null,
        subtype varchar(255) not null,
        applied_catalog_item_db_id varchar(255),
        owner_db_id varchar(255) not null,
        primary key (db_id)
    );

    create table profile_tailoring_reference (
        dtype varchar(31) not null,
        db_id varchar(255) not null,
        referencetype int4 not null check (referencetype between 0 and 7),
        attributes jsonb,
        link_type varchar(255),
        catalog_item_db_id varchar(255),
        owner_db_id varchar(255) not null,
        mitigation_db_id varchar(255),
        risk_owner_db_id varchar(255),
        primary key (db_id)
    );

    alter table if exists profile
       add constraint FK_domain_id
       foreign key (domain_db_id)
       references domain;

    alter table if exists profile
       add constraint FK_domain_template_id
       foreign key (domain_template_db_id)
       references domaintemplate;

    alter table if exists profileitem
       add constraint FK_owner_id
       foreign key (owner_db_id)
       references profile;

    alter table if exists profileitem
       add constraint FK_applied_catalog_item_id
       foreign key (applied_catalog_item_db_id)
       references catalogitem;

    alter table if exists profiletailoringreference
       add constraint FK_catalog_item_id
       foreign key (catalog_item_db_id)
       references profileitem;

    alter table if exists profiletailoringreference
       add constraint FK_mitigation_id
       foreign key (mitigation_db_id)
       references profileitem;

    alter table if exists profiletailoringreference
       add constraint FK_risk_owner_id
       foreign key (risk_owner_db_id)
       references profileitem;

    alter table if exists profiletailoringreference
        add constraint FK_owner_id
        foreign key (owner_db_id)
        references profileitem;

""")
    }
}
