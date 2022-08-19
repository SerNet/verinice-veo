/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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

class V43__move_profiles_to_separate_table extends BaseJavaMigration{
    @Override
    void migrate(Context context) throws Exception {
        def sql = new Sql(context.connection)

        sql.execute("""
            create table profile_set (
               id int8 not null,
                profiles jsonb not null,
                primary key (id)
            );
            create sequence seq_profile_sets start 1 increment 50;
        """)
        migrateTable(sql, "domain")
        migrateTable(sql, "domainTemplate")
    }

    private static void migrateTable(Sql sql, String domainTable) {
        sql.execute("""
            alter table $domainTable
                   add column profile_set_id int8;

            alter table $domainTable
                   add constraint FK_profile_set_id
                   foreign key (profile_set_id)
                   references profile_set;

            create index IDX_${domainTable.toUpperCase()}_PROFILE_SET_ID on $domainTable(profile_set_id);
        """.toString())

        sql.eachRow("SELECT db_id, profiles FROM $domainTable;".toString()) { domain ->
            def profileSetId = sql
                    .executeInsert(
                    [profiles: domain.profiles,],
                    "INSERT INTO profile_set (id, profiles) VALUES (nextval('seq_profile_sets'), :profiles)"
                    )
                    .first()
                    .first()
            sql.execute([
                id: domain.db_id,
                profileSetId: profileSetId,
            ], "UPDATE $domainTable SET profile_set_id = :profileSetId WHERE db_id = :id")
        }

        sql.execute("""
            alter table $domainTable drop column profiles;
            alter table $domainTable alter column profile_set_id set not null;
        """.toString())
    }
}
