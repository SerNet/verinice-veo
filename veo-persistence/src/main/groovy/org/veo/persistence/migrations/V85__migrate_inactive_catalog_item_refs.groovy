/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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

import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.util.logging.Slf4j

/**
 * Databases currently contain references from elements to inactive catalog items (items that are owned by inactive domains). This migration
 * replaces these outdated references with a reference to an active catalog item with the same abbreviation as the old one, or removes the
 * outdated reference if no matching active catalog item could be found. The abbreviation must serve as an identifier here, because catalog
 * items didn't have symbolic IDs in the old days.
 */
@Slf4j
class V85__migrate_inactive_catalog_item_refs extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        new Sql(context.connection).with { con ->
            def clients = rows("select db_id from client;")
            clients.eachWithIndex { client, idx ->
                def clientId = client.db_id
                log.info("Migrating catalog item refs in client $clientId ($idx / ${clients.size()})")
                eachRow("select distinct(name) from domain where owner_db_id = $clientId;") { standard ->
                    def domainName = standard.name
                    log.info("Migrating catalog item refs in '$domainName' domains for client $clientId")
                    migrateItemRefs(con, domainName, clientId)
                }
            }
        }
    }

    private static void migrateItemRefs(Sql con, String domainName, String clientId) {
        def catalogItemInfos = con.rows("""
                        select ci.db_id, ci.abbreviation, d.active
                        from catalogitem as ci
                                 left join domain as d on d.db_id = ci.domain_db_id
                        where d.owner_db_id = $clientId
                          and d.name = $domainName;
                  """)
        log.info("${catalogItemInfos.size()} items found")

        def activeItemIdByAbbreviation = new HashMap<String, String>()
        def inactiveItemIdsByAbbreviation = new HashMap<String, List<String>>()
        catalogItemInfos.forEach {
            if (it.active) {
                activeItemIdByAbbreviation.putIfAbsent(it.abbreviation, it.db_id)
            } else {
                inactiveItemIdsByAbbreviation.computeIfAbsent(it.abbreviation, { [] }).add(it.db_id)
            }
        }

        def statements = ''

        // Redirect outdated references to new item
        activeItemIdByAbbreviation.forEach { abbreviation, activeItemId ->
            def inactiveItemIds =
                    inactiveItemIdsByAbbreviation.getOrDefault(abbreviation, []).collect { "'$it'" }.join(',')
            if (!inactiveItemIds.empty) {
                statements += "update element_applied_catalog_items set applied_catalog_items_db_id = '$activeItemId' where applied_catalog_items_db_id in ($inactiveItemIds);"
                inactiveItemIdsByAbbreviation.remove(abbreviation)
            }
        }

        // Remove outdated references where no matching new item exists
        def obsoleteItemIds =
                inactiveItemIdsByAbbreviation.collectMany { it.value }.collect { "'$it'" }.join(',')
        if (!obsoleteItemIds.empty) {
            statements += "delete from element_applied_catalog_items where applied_catalog_items_db_id in ($obsoleteItemIds);"
        }

        if (!statements.empty) {
            con.execute(statements)
        }
    }
}