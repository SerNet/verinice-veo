/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core

import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate

import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.ControlDataRepository
import org.veo.persistence.access.jpa.DocumentDataRepository
import org.veo.persistence.access.jpa.IncidentDataRepository
import org.veo.persistence.access.jpa.PersonDataRepository
import org.veo.persistence.access.jpa.ProcessDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.test.VeoSpec

/**
 * Base class for veo specifications that use Spring
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
abstract class VeoSpringSpec extends VeoSpec {

    @Autowired
    ClientDataRepository clientDataRepository

    @Autowired
    UnitDataRepository unitDataRepository

    @Autowired
    AssetDataRepository assetDataRepository

    @Autowired
    ControlDataRepository controlDataRepository

    @Autowired
    DocumentDataRepository documentDataRepository

    @Autowired
    IncidentDataRepository incidentDataRepository

    @Autowired
    PersonDataRepository personDataRepository

    @Autowired
    ProcessDataRepository processDataRepository

    @Autowired
    TransactionTemplate txTemplate

    def setup() {
        txTemplate.execute {
            [
                assetDataRepository,
                controlDataRepository,
                documentDataRepository,
                incidentDataRepository,
                personDataRepository,
                processDataRepository
            ].each {
                it.findAll().forEach {
                    it.links.clear()
                }
            }
            [
                assetDataRepository,
                controlDataRepository,
                documentDataRepository,
                incidentDataRepository,
                personDataRepository,
                processDataRepository,
                unitDataRepository,
                clientDataRepository
            ]*.deleteAll()
        }
    }
}
