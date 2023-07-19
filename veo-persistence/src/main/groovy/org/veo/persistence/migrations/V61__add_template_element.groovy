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

class V61__add_template_element extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with {
            execute("""

    alter table if exists catalogitem
       alter column created_at set data type timestamp(6) with time zone;

    alter table if exists catalogitem
       alter column updated_at set data type timestamp(6) with time zone;

    alter table if exists catalogitem
       add column abbreviation varchar(255);

    alter table if exists catalogitem
       add column aspects jsonb;

    alter table if exists catalogitem
       add column custom_aspects jsonb not null default '{}'::jsonb;

    alter table if exists catalogitem
       add column description varchar(65535);

     alter table if exists catalogitem
       add column elementtype varchar(255);

    alter table if exists catalogitem
       add column name varchar(255);

    alter table if exists catalogitem
       add column status varchar(255);

    alter table if exists catalogitem
       add column subtype varchar(255);

    with s as (select e.containing_catalog_item_id as catalog_item_id,
                      e.dtype                      as element_type,
                      e.name                       as name,
                      e.abbreviation               as abbreviation,
                      e.description                as description,
                      min(sa.sub_type)             as sub_type,
                      min(sa.status)               as status
               from element as e
                        inner join subtype_aspect as sa on sa.owner_db_id = e.db_id
               where e.containing_catalog_item_id is not null
               group by e.db_id)
    update catalogitem
    set name = s.name,
        abbreviation = s.abbreviation,
        description = s.description,
        elementtype = s.element_type,
        subtype = s.sub_type,
        status = s.status
    from s
    where s.catalog_item_id = db_id;

    with s as (select e.containing_catalog_item_id            as catalog_item_id,
                      json_object_agg(ca.type, ca.attributes) as custom_aspects
               from element as e
                        left join custom_aspect as ca on ca.owner_db_id = e.db_id
               where e.containing_catalog_item_id is not null
                 and ca.type is not null
               group by e.db_id)
    update catalogitem
    set custom_aspects = s.custom_aspects
    from s
    where s.catalog_item_id = db_id;

    alter table catalogitem
        alter column name SET NOT NULL,
        alter column elementtype SET NOT NULL,
        alter column subtype SET NOT NULL,
        alter column status SET NOT NULL;
            """)
        }
    }
}
