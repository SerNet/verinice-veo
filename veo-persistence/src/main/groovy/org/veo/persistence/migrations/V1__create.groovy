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

class V1__create extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        context.getConnection().createStatement().execute("""

    create table abstractriskdata (
       dtype varchar(31) not null,
        db_id varchar(255) not null,
        created_at timestamp not null,
        created_by varchar(255) not null,
        updated_at timestamp not null,
        updated_by varchar(255) not null,
        version int8 not null,
        designator varchar(255) not null,
        entity_db_id varchar(255) not null,
        control_id varchar(255),
        person_id varchar(255),
        scenario_db_id varchar(255) not null,
        primary key (db_id)
    );

    create table abstractriskdata_domains (
       abstractriskdata_db_id varchar(255) not null,
        domains_db_id varchar(255) not null,
        primary key (abstractriskdata_db_id, domains_db_id)
    );

    create table aspect (
       dtype varchar(31) not null,
        db_id varchar(255) not null,
        sub_type varchar(255),
        domain_id varchar(255) not null,
        owner_db_id varchar(255) not null,
        primary key (db_id)
    );

    create table asset_parts (
       composite_id varchar(255) not null,
        part_id varchar(255) not null,
        primary key (composite_id, part_id)
    );

    create table catalog (
       db_id varchar(255) not null,
        created_at timestamp not null,
        created_by varchar(255) not null,
        updated_at timestamp not null,
        updated_by varchar(255) not null,
        version int8 not null,
        abbreviation varchar(255),
        description varchar(65535),
        name varchar(255) not null,
        domaintemplate_id varchar(255) not null,
        primary key (db_id)
    );

    create table catalogitem (
       db_id varchar(255) not null,
        created_at timestamp not null,
        created_by varchar(255) not null,
        updated_at timestamp not null,
        updated_by varchar(255) not null,
        version int8 not null,
        namespace varchar(255),
        catalog_db_id varchar(255) not null,
        primary key (db_id)
    );

    create table client (
       db_id varchar(255) not null,
        created_at timestamp not null,
        created_by varchar(255) not null,
        updated_at timestamp not null,
        updated_by varchar(255) not null,
        version int8 not null,
        abbreviation varchar(255),
        description varchar(65535),
        name varchar(255),
        primary key (db_id)
    );

    create table control_parts (
       composite_id varchar(255) not null,
        part_id varchar(255) not null,
        primary key (composite_id, part_id)
    );

    create table customproperties (
       dtype varchar(31) not null,
        db_id varchar(255) not null,
        attributes jsonb,
        type varchar(255),
        owner_db_id varchar(255),
        source_id varchar(255),
        target_id varchar(255),
        primary key (db_id)
    );

    create table customproperties_domains (
       customproperties_db_id varchar(255) not null,
        domains_db_id varchar(255) not null,
        primary key (customproperties_db_id, domains_db_id)
    );

    create table document_parts (
       composite_id varchar(255) not null,
        part_id varchar(255) not null,
        primary key (composite_id, part_id)
    );

    create table domain (
       db_id varchar(255) not null,
        created_at timestamp not null,
        created_by varchar(255) not null,
        updated_at timestamp not null,
        updated_by varchar(255) not null,
        version int8 not null,
        abbreviation varchar(255),
        authority varchar(255) not null,
        description varchar(65535),
        name varchar(255) not null,
        revision varchar(255) not null,
        templateversion varchar(255) not null,
        active boolean,
        domain_template_db_id varchar(255),
        owner_db_id varchar(255) not null,
        primary key (db_id)
    );

    create table domaintemplate (
       db_id varchar(255) not null,
        created_at timestamp not null,
        created_by varchar(255) not null,
        updated_at timestamp not null,
        updated_by varchar(255) not null,
        version int8 not null,
        abbreviation varchar(255),
        authority varchar(255) not null,
        description varchar(65535),
        name varchar(255) not null,
        revision varchar(255) not null,
        templateversion varchar(255) not null,
        primary key (db_id)
    );

    create table entitylayersupertype (
       dtype varchar(31) not null,
        db_id varchar(255) not null,
        created_at timestamp not null,
        created_by varchar(255) not null,
        updated_at timestamp not null,
        updated_by varchar(255) not null,
        version int8 not null,
        abbreviation varchar(255),
        description varchar(65535),
        designator varchar(255) not null,
        name varchar(255) not null,
        status varchar(255),
        containing_catalog_item_id varchar(255),
        owner_id varchar(255),
        primary key (db_id)
    );

    create table entitylayersupertype_applied_catalog_items (
       entitylayersupertype_db_id varchar(255) not null,
        applied_catalog_items_db_id varchar(255) not null,
        primary key (entitylayersupertype_db_id, applied_catalog_items_db_id)
    );

    create table entitylayersupertype_domains (
       entitylayersupertype_db_id varchar(255) not null,
        domains_db_id varchar(255) not null,
        primary key (entitylayersupertype_db_id, domains_db_id)
    );

    create table incident_parts (
       composite_id varchar(255) not null,
        part_id varchar(255) not null,
        primary key (composite_id, part_id)
    );

    create table person_parts (
       composite_id varchar(255) not null,
        part_id varchar(255) not null,
        primary key (composite_id, part_id)
    );

    create table process_parts (
       composite_id varchar(255) not null,
        part_id varchar(255) not null,
        primary key (composite_id, part_id)
    );

    create table scenario_parts (
       composite_id varchar(255) not null,
        part_id varchar(255) not null,
        primary key (composite_id, part_id)
    );

    create table scope_members (
       scope_id varchar(255) not null,
        member_id varchar(255) not null,
        primary key (scope_id, member_id)
    );

    create table stored_event_data (
       id int8 not null,
        content varchar(100000),
        lock_time timestamp,
        processed boolean not null,
        routing_key varchar(255),
        timestamp timestamp,
        primary key (id)
    );

    create table tailoringreference (
       db_id varchar(255) not null,
        created_at timestamp not null,
        created_by varchar(255) not null,
        updated_at timestamp not null,
        updated_by varchar(255) not null,
        version int8 not null,
        referencetype int4,
        catalog_item_db_id varchar(255),
        owner_db_id varchar(255) not null,
        primary key (db_id)
    );

    create table unit (
       db_id varchar(255) not null,
        created_at timestamp not null,
        created_by varchar(255) not null,
        updated_at timestamp not null,
        updated_by varchar(255) not null,
        version int8 not null,
        abbreviation varchar(255),
        description varchar(65535),
        name varchar(255) not null,
        client_id varchar(255) not null,
        parent_db_id varchar(255),
        primary key (db_id)
    );

    create table unit_domains (
       unit_db_id varchar(255) not null,
        domains_db_id varchar(255) not null,
        primary key (unit_db_id, domains_db_id)
    );

    create table updatereference (
       db_id varchar(255) not null,
        created_at timestamp not null,
        created_by varchar(255) not null,
        updated_at timestamp not null,
        updated_by varchar(255) not null,
        version int8 not null,
        updatetype int4,
        catalog_item_db_id varchar(255),
        owner_db_id varchar(255) not null,
        primary key (db_id)
    );

create sequence seq_events start 1 increment 50;

    alter table abstractriskdata
       add constraint FKt0x95j2pjbs5xshwto5w39mpw
       foreign key (entity_db_id)
       references entitylayersupertype;

    alter table abstractriskdata
       add constraint FKij08a8y2ph2coytt991npejxr
       foreign key (control_id)
       references entitylayersupertype;

    alter table abstractriskdata
       add constraint FKcnxmlc44v78y432yhb1luk47u
       foreign key (person_id)
       references entitylayersupertype;

    alter table abstractriskdata
       add constraint FKkjua2jtn7p31gvik8gg85h2nv
       foreign key (scenario_db_id)
       references entitylayersupertype;

    alter table abstractriskdata_domains
       add constraint FKhtk37bdlqjhnnyu0872qtn3we
       foreign key (domains_db_id)
       references domain;

    alter table abstractriskdata_domains
       add constraint FKkhvag73rf7q4vc76s5byki5i3
       foreign key (abstractriskdata_db_id)
       references abstractriskdata;

    alter table aspect
       add constraint FK3flxr5nu6rey2j5qffjv6lhg1
       foreign key (domain_id)
       references domain;

    alter table aspect
       add constraint FK2dvenyhxa1n6g039kj8lyq8f4
       foreign key (owner_db_id)
       references entitylayersupertype;

    alter table asset_parts
       add constraint FKkxjnsgn0gjjv4cq6v0xi9syi0
       foreign key (part_id)
       references entitylayersupertype;

    alter table asset_parts
       add constraint FK8vo67xu4okjqc0rdx9aym2qf6
       foreign key (composite_id)
       references entitylayersupertype;

    alter table catalogitem
       add constraint FK1pfnilsti2yadxk21wwu4dsx5
       foreign key (catalog_db_id)
       references catalog;

    alter table control_parts
       add constraint FK8tf54o9sodfjygcvplsr35xu0
       foreign key (part_id)
       references entitylayersupertype;

    alter table control_parts
       add constraint FKbdoim8t94tc0fbdkwdwwetv4n
       foreign key (composite_id)
       references entitylayersupertype;

    alter table customproperties
       add constraint FKf1okru70icekwxj8b02ib9xjf
       foreign key (owner_db_id)
       references entitylayersupertype;

    alter table customproperties
       add constraint FKrnkiqyp2njxbwb6ul83svd81t
       foreign key (source_id)
       references entitylayersupertype;

    alter table customproperties
       add constraint FKewlyugsifkj3lnbqjy361vi0n
       foreign key (target_id)
       references entitylayersupertype;

    alter table customproperties_domains
       add constraint FK76widxvad8cor8u9pl8jemf17
       foreign key (domains_db_id)
       references domain;

    alter table customproperties_domains
       add constraint FKl3e16136b5vf5b6hcfe01m27x
       foreign key (customproperties_db_id)
       references customproperties;

    alter table document_parts
       add constraint FK1asiihoudapau2nxildl2s67d
       foreign key (part_id)
       references entitylayersupertype;

    alter table document_parts
       add constraint FKoxijqceribi3eqjqe4xdlvjhy
       foreign key (composite_id)
       references entitylayersupertype;

    alter table domain
       add constraint FKe68rg68ksqxb6lh1sobpvyx66
       foreign key (owner_db_id)
       references client;

    alter table entitylayersupertype
       add constraint FKi76utour485s81ji2sulrt7fl
       foreign key (containing_catalog_item_id)
       references catalogitem;

    alter table entitylayersupertype
       add constraint FK63basimykk4tng4c542s04g7y
       foreign key (owner_id)
       references unit;

    alter table entitylayersupertype_applied_catalog_items
       add constraint FKdkycpe0ury0s3f80ur9mrywf8
       foreign key (applied_catalog_items_db_id)
       references catalogitem;

    alter table entitylayersupertype_applied_catalog_items
       add constraint FKpmlxjy1moteqk7fniostc8c90
       foreign key (entitylayersupertype_db_id)
       references entitylayersupertype;

    alter table entitylayersupertype_domains
       add constraint FK2pg41kgg8acd6vaxis2busncg
       foreign key (domains_db_id)
       references domain;

    alter table entitylayersupertype_domains
       add constraint FK5gec7tcmgjn8sa1qr0vs9866j
       foreign key (entitylayersupertype_db_id)
       references entitylayersupertype;

    alter table incident_parts
       add constraint FKgskq3x3pm3e5msm9v6xbmsswt
       foreign key (part_id)
       references entitylayersupertype;

    alter table incident_parts
       add constraint FKu9ui65ut2cuue9xvc8fs7p58
       foreign key (composite_id)
       references entitylayersupertype;

    alter table person_parts
       add constraint FKj0sy9yrb0t9r40sowmphof6bk
       foreign key (part_id)
       references entitylayersupertype;

    alter table person_parts
       add constraint FKd56fn9r3yehvsl9f6qw4xvvdx
       foreign key (composite_id)
       references entitylayersupertype;

    alter table process_parts
       add constraint FK3v2tovvhq4wtibn7ln1ydoa0y
       foreign key (part_id)
       references entitylayersupertype;

    alter table process_parts
       add constraint FKl1dbgird02b409c83dpvx4ijy
       foreign key (composite_id)
       references entitylayersupertype;

    alter table scenario_parts
       add constraint FKn553h8hnia55iron4w1y7bnev
       foreign key (part_id)
       references entitylayersupertype;

    alter table scenario_parts
       add constraint FKjqec32wkxi1745spsdhohlgd9
       foreign key (composite_id)
       references entitylayersupertype;

    alter table scope_members
       add constraint FKoc68so075d198cemlti412abc
       foreign key (member_id)
       references entitylayersupertype;

    alter table scope_members
       add constraint FKklfdev12o68eif79ga2yms7p
       foreign key (scope_id)
       references entitylayersupertype;

    alter table tailoringreference
       add constraint FK2ah6fybcgf8ihaj0crtsq1ow
       foreign key (catalog_item_db_id)
       references catalogitem;

    alter table tailoringreference
       add constraint FKk4ng8encdx9qomi72ii6hwhd7
       foreign key (owner_db_id)
       references catalogitem;

    alter table unit
       add constraint FKatalq4eest82ckgmmqdg8j855
       foreign key (client_id)
       references client;

    alter table unit
       add constraint FKo7lqmt3rjp2o59voveljdj7la
       foreign key (parent_db_id)
       references unit;

    alter table unit_domains
       add constraint FKoy7hpibx3jpyrh8je7nvg4941
       foreign key (domains_db_id)
       references domain;

    alter table unit_domains
       add constraint FKj51bxblm18unv5unxx37x1810
       foreign key (unit_db_id)
       references unit;

    alter table updatereference
       add constraint FKgf20k65sejvwe8wauyabco4pi
       foreign key (catalog_item_db_id)
       references catalogitem;

    alter table updatereference
       add constraint FKo5wdhadvjytymvodvv02gmiu4
       foreign key (owner_db_id)
       references catalogitem;

""")
    }
}
