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
package org.veo.rest.test

import static org.veo.rest.test.UserType.CONTENT_CREATOR

abstract class DomainRestTest extends VeoRestTest {

    def putAssetDefinition(String domainId) {
        put("/content-creation/domains/$domainId/element-type-definitions/asset",
                [
                    subTypes: [
                        server: [
                            statuses: ["off", "on"]
                        ]
                    ],
                    translations: [
                        en: [
                            asset_server_singular: "Server",
                            asset_server_plural: "Servers",
                            asset_server_status_off: "off",
                            asset_server_status_on: "on"
                        ]
                    ]
                ],
                null, 204, CONTENT_CREATOR)
    }

    def putPersonDefinition(String domainId) {
        put("/content-creation/domains/$domainId/element-type-definitions/person",
                [
                    subTypes: [
                        PER_Person: [
                            statuses: ["NEW", "on"]
                        ]
                    ],
                    customAspects: [
                        sight: [
                            attributeDefinitions: [
                                needsGlasses: [
                                    type: "boolean",
                                ],
                                nightBlind: [
                                    type: "boolean"
                                ],
                            ]
                        ]
                    ]
                ],
                null, 204, CONTENT_CREATOR)
    }

    def putProcessDefinition(String domainId) {
        put("/content-creation/domains/$domainId/element-type-definitions/process",
                [
                    subTypes: [
                        PRO_Process: [
                            statuses: ["NEW", "on"]
                        ]
                    ]
                ],
                null, 204, CONTENT_CREATOR)
    }
}
