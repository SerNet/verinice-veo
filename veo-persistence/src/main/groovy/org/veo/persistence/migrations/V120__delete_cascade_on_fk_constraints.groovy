/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Jochen Kemnade
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

import org.veo.core.entity.EntityType

import groovy.sql.Sql

class V120__delete_cascade_on_fk_constraints extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute('''
alter table domain
drop constraint fk_owner_db_id,
add constraint fk_owner_db_id
    foreign key (owner_db_id)
    references client(db_id)
    on delete cascade;

alter table profile
drop constraint fk_domain_id,
add constraint fk_domain_id
    foreign key (domain_db_id)
    references domain(db_id)
    on delete cascade;

alter table profile_item
drop constraint fk_owner_id,
add constraint fk_owner_id
    foreign key (owner_db_id)
    references profile(db_id)
    on delete cascade;

alter table profile_tailoring_reference
drop constraint fk_owner_id,
add constraint fk_owner_id
    foreign key (owner_db_id)
    references profile_item(db_id)
    on delete cascade;

alter table profile_tailoring_reference
drop constraint fk_target_id,
add constraint fk_target_id
    foreign key (target_db_id)
    references profile_item(db_id)
    on delete cascade;

alter table profile_tailoring_reference
drop constraint fk_mitigation_id,
add constraint fk_mitigation_id
    foreign key (mitigation_db_id)
    references profile_item(db_id)
    on delete cascade;

alter table profile_tailoring_reference
drop constraint fk_responsible_id,
add constraint fk_responsible_id
    foreign key (responsible_db_id)
    references profile_item(db_id)
    on delete cascade;

alter table profile_tailoring_reference
drop constraint fk_risk_owner_id,
add constraint fk_risk_owner_id
    foreign key (risk_owner_db_id)
    references profile_item(db_id)
    on delete cascade;

alter table catalogitem
drop constraint fk_domain_id,
add constraint fk_domain_id
    foreign key (domain_db_id)
    references domain(db_id)
    on delete cascade;

alter table catalog_tailoring_reference
drop constraint fk_owner_db_id,
add constraint fk_owner_db_id
    foreign key (owner_db_id)
    references catalogitem(db_id)
    on delete cascade;

alter table catalog_tailoring_reference
drop constraint fk_target_db_id,
add constraint fk_target_db_id
    foreign key (target_db_id)
    references catalogitem(db_id)
    on delete cascade;

alter table userconfiguration
drop constraint fk_client_id,
add constraint fk_client_id
    foreign key (client_id)
    references client(db_id)
    on delete cascade;

drop procedure prepare_for_client_deletion;

create or replace procedure delete_client(client_uuid uuid)
language plpgsql
as $$
declare
    domain_ids uuid[];
    decision_set_ids bigint[];
    inspection_set_ids bigint[];
    risk_definition_set_ids bigint[];
begin
    select array_agg(db_id),
           array_agg(decision_set_id),
           array_agg(inspection_set_id),
           array_agg(risk_definition_set_id)
    into domain_ids, decision_set_ids, inspection_set_ids, risk_definition_set_ids
    from domain
    where owner_db_id = client_uuid;

    delete from client where db_id = client_uuid;

    delete from element_type_definition
    where owner_db_id = any(domain_ids);

    delete from decision_set
    where id = any(decision_set_ids);

    delete from inspection_set
    where id = any(inspection_set_ids);

    delete from risk_definition_set
    where id = any(risk_definition_set_ids);

''' + dropDesignatorSequencesSnippet()+
                    '''
end;
$$;''')
        }
    }

    static String dropDesignatorSequencesSnippet() {
        EntityType.TYPE_DESIGNATORS.collect {
            "    execute format('drop sequence if exists %I', format('designator_%s_${it.toLowerCase()}', replace(client_uuid::text, '-', '')));"
        }.join("\n")
    }
}
