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

import static org.veo.rest.test.UserType.ADMIN
import static org.veo.rest.test.UserType.CONTENT_CREATOR
import static org.veo.rest.test.UserType.SECONDARY_CLIENT_USER

import org.apache.http.HttpStatus

abstract class DomainRestTest extends VeoRestTest{

    def postAssetObjectSchema(String domainId) {
        post("/content-creation/domains/$domainId/element-type-definitions/asset/object-schema",
                [
                    properties: [
                        domains: [
                            properties: [
                                (domainId): [
                                    properties: [
                                        subType: [
                                            enum: ["server"],
                                        ]
                                    ],
                                    allOf: [
                                        [
                                            "if": [
                                                properties: [
                                                    subType: [
                                                        const: "server"
                                                    ]
                                                ]
                                            ],
                                            then: [
                                                properties: [
                                                    status: [
                                                        enum: ["off", "on"]
                                                    ]
                                                ]
                                            ]
                                        ]
                                    ]
                                ]
                            ]
                        ],
                        customAspects: [
                            properties: [:]
                        ],
                        links: [
                            properties: [:]
                        ],
                        translations: [
                            en: [
                                asset_server_status_off: "off",
                                asset_server_status_on: "on"
                            ]
                        ]
                    ]
                ],
                204, CONTENT_CREATOR)
    }

    def postPersonObjectSchema(String domainId) {
        post("/content-creation/domains/$domainId/element-type-definitions/person/object-schema",
                [
                    properties: [
                        domains: [
                            properties: [
                                (domainId): [
                                    properties: [
                                        subType: [
                                            enum: ["PER_Person"],
                                        ]
                                    ],
                                    allOf: [
                                        [
                                            "if": [
                                                properties: [
                                                    subType: [
                                                        const: "PER_Person"
                                                    ]
                                                ]
                                            ],
                                            then: [
                                                properties: [
                                                    status: [
                                                        enum: ["NEW", "on"]
                                                    ]
                                                ]
                                            ]
                                        ]
                                    ]
                                ]
                            ]
                        ],
                        customAspects: [
                            properties: [:]
                        ],
                        links: [
                            properties: [:]
                        ],
                        translations: [:]
                    ]
                ],
                204, CONTENT_CREATOR)
    }
    def postProcessObjectSchema(String domainId) {
        post("/content-creation/domains/$domainId/element-type-definitions/process/object-schema",
                [
                    properties: [
                        domains: [
                            properties: [
                                (domainId): [
                                    properties: [
                                        subType: [
                                            enum: ["PRO_Process"],
                                        ]
                                    ],
                                    allOf: [
                                        [
                                            "if": [
                                                properties: [
                                                    subType: [
                                                        const: "PRO_Process"
                                                    ]
                                                ]
                                            ],
                                            then: [
                                                properties: [
                                                    status: [
                                                        enum: ["NEW", "on"]
                                                    ]
                                                ]
                                            ]
                                        ]
                                    ]
                                ]
                            ]
                        ],
                        customAspects: [
                            properties: [:]
                        ],
                        links: [
                            properties: [:]
                        ],
                        translations: [:]
                    ]
                ],
                204, CONTENT_CREATOR)
    }
}
