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

import groovy.sql.Sql

class V121__revert_changes_and_add_indexes_for_fk_constraints extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute('''

drop procedure delete_client;

alter table domain
drop constraint fk_owner_db_id,
add constraint fk_owner_db_id
    foreign key (owner_db_id)
    references client(db_id);

alter table profile
drop constraint fk_domain_id,
add constraint fk_domain_id
    foreign key (domain_db_id)
    references domain(db_id);

alter table profile_item
drop constraint fk_owner_id,
add constraint fk_owner_id
    foreign key (owner_db_id)
    references profile(db_id);

alter table profile_tailoring_reference
drop constraint fk_owner_id,
add constraint fk_owner_id
    foreign key (owner_db_id)
    references profile_item(db_id);

alter table profile_tailoring_reference
drop constraint fk_target_id,
add constraint fk_target_id
    foreign key (target_db_id)
    references profile_item(db_id);

alter table profile_tailoring_reference
drop constraint fk_mitigation_id,
add constraint fk_mitigation_id
    foreign key (mitigation_db_id)
    references profile_item(db_id);

alter table profile_tailoring_reference
drop constraint fk_responsible_id,
add constraint fk_responsible_id
    foreign key (responsible_db_id)
    references profile_item(db_id);

alter table profile_tailoring_reference
drop constraint fk_risk_owner_id,
add constraint fk_risk_owner_id
    foreign key (risk_owner_db_id)
    references profile_item(db_id);

alter table catalogitem
drop constraint fk_domain_id,
add constraint fk_domain_id
    foreign key (domain_db_id)
    references domain(db_id);

alter table catalog_tailoring_reference
drop constraint fk_owner_db_id,
add constraint fk_owner_db_id
    foreign key (owner_db_id)
    references catalogitem(db_id);

alter table catalog_tailoring_reference
drop constraint fk_target_db_id,
add constraint fk_target_db_id
    foreign key (target_db_id)
    references catalogitem(db_id);

alter table userconfiguration
drop constraint fk_client_id,
add constraint fk_client_id
    foreign key (client_id)
    references client(db_id);

create index idx_element_domain_association_domain_id
    on element_domain_association(domain_id);

create index idx_element_domain_association_applied_catalog_item_db_id
    on element_domain_association(applied_catalog_item_db_id);

create index idx_custom_aspect_domain_id
    on custom_aspect(domain_id);

create index idx_catalog_tailoring_reference_target_db_id
    on catalog_tailoring_reference(target_db_id);

create index idx_decision_results_aspect_domain_id
    on decision_results_aspect(domain_id);

create index idx_impact_values_aspect_domain_id
    on impact_values_aspect(domain_id);

create index idx_scenario_risk_values_aspect_domain_id
    on scenario_risk_values_aspect(domain_id);

create index idx_scope_risk_values_aspect_domain_id
    on scope_risk_values_aspect(domain_id);

create index idx_profile_item_applied_catalog_item_db_id
    on profile_item(applied_catalog_item_db_id);

create index idx_profile_tailoring_reference_target_db_id
    on profile_tailoring_reference(target_db_id);

create index idx_profile_tailoring_reference_risk_owner_db_id
    on profile_tailoring_reference(risk_owner_db_id);

create index idx_profile_tailoring_reference_responsible_db_id
    on profile_tailoring_reference(responsible_db_id);

create index idx_profile_tailoring_reference_mitigation_db_id
    on profile_tailoring_reference(mitigation_db_id);
''')
        }
    }
}
