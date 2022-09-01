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

class V45__fix_template_versions extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        def sql = new Sql(context.connection)
        [
            "f8ed22b1-b277-56ec-a2ce-0dbd94e24824",
            "342361a7-2c18-5cd1-9318-1594b31552db",
            "d86e3b0a-8d52-5a18-9833-a6fc6352d970",
            "24681274-3267-5d33-8c28-f0a6cd987683",
            "fbd9da2a-9dd8-5647-a4ef-eccbc029d303",
            "c0a6abdc-d350-5e3d-abbc-d53d7df82900",
            "2221bad5-53cd-5073-9f5f-683ec8a8ce1e",
            "efc3fa10-df63-5bd9-9bdf-5f52d34ab584",
            "7fdee609-5250-5586-9820-08bdc207b0ad",
            "5b273731-be23-5a58-b744-b54b74ba572f",
            "633a3fed-ece2-5791-8667-129627a621f3",
            "acf6effb-55c2-515d-8cbd-e042e21234cb",
            "42686e50-1257-5e42-8de0-3dec447ba0f8",
            "e96dd67f-090e-52be-8b76-f66b75624b45",
            "dd8bb7ed-bbba-5728-933a-e1c5c2e03c46",
            "66840169-9e94-5f92-bf1b-734f6f28b505",
            "7fbfadd3-1570-51e2-85fd-7b53778f2963",
            "ec6b89d6-d543-546a-a84b-e9c1a038060c",
            "7b3ec3d5-03f0-5541-80cd-68d339e92903",
            "65d53e89-b4b9-52da-b48e-f6a3a4af2386",
        ].eachWithIndex { templateId, idx ->
            def newVersion = "0.1.$idx"
            sql.execute("""
                update domaintemplate set templateversion = '$newVersion' where db_id = '$templateId';
                update domain set templateversion = '$newVersion' where domain_template_db_id = '$templateId';
            """.toString())
        }
        sql.execute("""
            alter table domaintemplate drop column revision;
            alter table domain drop column revision;

            create unique index IDX_DOMAIN_TEMPLATE_NAME_AND_TEMPLATE_VERSION
                on domaintemplate(name, templateversion);

            alter table domaintemplate
                add constraint UK_name_template_version
                unique using index IDX_DOMAIN_TEMPLATE_NAME_AND_TEMPLATE_VERSION;
        """)
    }
}
