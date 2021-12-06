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

class V2__rename_foreign_keys extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).execute("""

    alter table abstractriskdata
       rename constraint FKt0x95j2pjbs5xshwto5w39mpw to FK_entity_db_id;

    alter table abstractriskdata
       rename constraint FKij08a8y2ph2coytt991npejxr to FK_control_id;

    alter table abstractriskdata
       rename constraint FKcnxmlc44v78y432yhb1luk47u to FK_person_id;

    alter table abstractriskdata
       rename constraint FKkjua2jtn7p31gvik8gg85h2nv to FK_scenario_db_id;

    alter table abstractriskdata_domains
       rename constraint FKhtk37bdlqjhnnyu0872qtn3we to FK_domains_db_id;

    alter table abstractriskdata_domains
       rename constraint FKkhvag73rf7q4vc76s5byki5i3 to FK_abstractriskdata_db_id;

    alter table aspect
       rename constraint FK3flxr5nu6rey2j5qffjv6lhg1 to FK_domain_id;

    alter table aspect
       rename constraint FK2dvenyhxa1n6g039kj8lyq8f4 to FK_owner_db_id;

    alter table asset_parts
       rename constraint FKkxjnsgn0gjjv4cq6v0xi9syi0 to FK_part_id;

    alter table asset_parts
       rename constraint FK8vo67xu4okjqc0rdx9aym2qf6 to FK_composite_id;

    alter table catalogitem
       rename constraint FK1pfnilsti2yadxk21wwu4dsx5 to FK_catalog_db_id;

    alter table control_parts
       rename constraint FK8tf54o9sodfjygcvplsr35xu0 to FK_part_id;

    alter table control_parts
       rename constraint FKbdoim8t94tc0fbdkwdwwetv4n to FK_composite_id;

    alter table customproperties
       rename constraint FKf1okru70icekwxj8b02ib9xjf to FK_owner_db_id;

    alter table customproperties
       rename constraint FKrnkiqyp2njxbwb6ul83svd81t to FK_source_id;

    alter table customproperties
       rename constraint FKewlyugsifkj3lnbqjy361vi0n to FK_target_id;

    alter table customproperties_domains
       rename constraint FK76widxvad8cor8u9pl8jemf17 to FK_domains_db_id;

    alter table customproperties_domains
       rename constraint FKl3e16136b5vf5b6hcfe01m27x to FK_customproperties_db_id;

    alter table document_parts
       rename constraint FK1asiihoudapau2nxildl2s67d to FK_part_id;

    alter table document_parts
       rename constraint FKoxijqceribi3eqjqe4xdlvjhy to FK_composite_id;

    alter table domain
       rename constraint FKe68rg68ksqxb6lh1sobpvyx66 to FK_owner_db_id;

    alter table entitylayersupertype
       rename constraint FKi76utour485s81ji2sulrt7fl to FK_containing_catalog_item_id;

    alter table entitylayersupertype
       rename constraint FK63basimykk4tng4c542s04g7y to FK_owner_id;

    alter table entitylayersupertype_applied_catalog_items
       rename constraint FKdkycpe0ury0s3f80ur9mrywf8 to FK_applied_catalog_items_db_id;

    alter table entitylayersupertype_applied_catalog_items
       rename constraint FKpmlxjy1moteqk7fniostc8c90 to FK_entitylayersupertype_db_id;

    alter table entitylayersupertype_domains
       rename constraint FK2pg41kgg8acd6vaxis2busncg to FK_domains_db_id;

    alter table entitylayersupertype_domains
       rename constraint FK5gec7tcmgjn8sa1qr0vs9866j to FK_entitylayersupertype_db_id;

    alter table incident_parts
       rename constraint FKgskq3x3pm3e5msm9v6xbmsswt to FK_part_id;

    alter table incident_parts
       rename constraint FKu9ui65ut2cuue9xvc8fs7p58 to FK_composite_id;

    alter table person_parts
       rename constraint FKj0sy9yrb0t9r40sowmphof6bk to FK_part_id;

    alter table person_parts
       rename constraint FKd56fn9r3yehvsl9f6qw4xvvdx to FK_composite_id;

    alter table process_parts
       rename constraint FK3v2tovvhq4wtibn7ln1ydoa0y to FK_part_id;

    alter table process_parts
       rename constraint FKl1dbgird02b409c83dpvx4ijy to FK_composite_id;

    alter table scenario_parts
       rename constraint FKn553h8hnia55iron4w1y7bnev to FK_part_id;

    alter table scenario_parts
       rename constraint FKjqec32wkxi1745spsdhohlgd9 to FK_composite_id;

    alter table scope_members
       rename constraint FKoc68so075d198cemlti412abc to FK_member_id;

    alter table scope_members
       rename constraint FKklfdev12o68eif79ga2yms7p to FK_scope_id;

    alter table tailoringreference
       rename constraint FK2ah6fybcgf8ihaj0crtsq1ow to FK_catalog_item_db_id;

    alter table tailoringreference
       rename constraint FKk4ng8encdx9qomi72ii6hwhd7 to FK_owner_db_id;

    alter table unit
       rename constraint FKatalq4eest82ckgmmqdg8j855 to FK_client_id;

    alter table unit
       rename constraint FKo7lqmt3rjp2o59voveljdj7la to FK_parent_db_id;

    alter table unit_domains
       rename constraint FKoy7hpibx3jpyrh8je7nvg4941 to FK_domains_db_id;

    alter table unit_domains
       rename constraint FKj51bxblm18unv5unxx37x1810 to FK_unit_db_id;

    alter table updatereference
       rename constraint FKgf20k65sejvwe8wauyabco4pi to FK_catalog_item_db_id;

    alter table updatereference
       rename constraint FKo5wdhadvjytymvodvv02gmiu4 to FK_owner_db_id;

""")
    }
}
