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

class V118__add_prepare_for_client_deletion extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute('''
create or replace procedure prepare_for_client_deletion(client_uuid uuid)
language plpgsql
as $$
begin
    delete from userconfiguration where client_id = client_uuid;

    delete from profile_tailoring_reference ptr
    using profile_item pi
    join profile p on pi.owner_db_id = p.db_id
    join domain d on p.domain_db_id = d.db_id
    where ptr.owner_db_id = pi.db_id
      and d.owner_db_id = client_uuid;

    delete from profile_item pi
    using profile p
    join domain d on p.domain_db_id = d.db_id
    where pi.owner_db_id = p.db_id
      and d.owner_db_id = client_uuid;

    delete from profile p
    using domain d
    where p.domain_db_id = d.db_id
      and d.owner_db_id = client_uuid;

    delete from catalog_tailoring_reference ctr
    using catalogitem ci
    join domain d on ci.domain_db_id = d.db_id
    where ctr.owner_db_id = ci.db_id
      and d.owner_db_id = client_uuid;

    delete from catalogitem ci
    using domain d
    where ci.domain_db_id = d.db_id
      and d.owner_db_id = client_uuid;

    delete from domain d
    where d.owner_db_id = client_uuid;

    delete from element_type_definition etd
    using domain d
    where etd.owner_db_id = d.db_id
      and d.owner_db_id = client_uuid;

    delete from decision_set ds
    using domain d
    where ds.id = d.decision_set_id
      and d.owner_db_id = client_uuid;

    delete from inspection_set ins
    using domain d
    where ins.id = d.inspection_set_id
      and d.owner_db_id = client_uuid;

    delete from risk_definition_set rds
    using domain d
    where rds.id = d.risk_definition_set_id
      and d.owner_db_id = client_uuid;
end;
$$;
''')
        }
    }
}