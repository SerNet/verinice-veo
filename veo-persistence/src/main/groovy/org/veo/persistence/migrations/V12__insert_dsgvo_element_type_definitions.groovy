/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

import groovy.json.JsonOutput
import groovy.sql.Sql

/**
 * Add DSGVO element type definitions to all existing domains (assuming that all domains have been used with the old
 * static DSGVO object schemas). This clears any existing element type definitions.
 */
class V12__insert_dsgvo_element_type_definitions extends BaseJavaMigration {
    @Override
    void migrate(Context context) throws Exception {
        def sql = new Sql(context.connection)
        sql.execute("DELETE FROM domaintemplate_element_type_definitions;")
        sql.execute("DELETE FROM element_type_definition;")

        def definitions = getDsgvoDefinitions()
        sql.eachRow("SELECT db_id FROM domain;") { domain ->
            def domainId = domain.getString(1)
            definitions.each { entry ->
                def definitionId = UUID.randomUUID()
                sql.execute([
                    id: definitionId,
                    ownerId: domainId,
                    elementType: entry.key,
                    customAspects: JsonOutput.toJson(entry.value.customAspects),
                    links: JsonOutput.toJson(entry.value.links),
                    subTypes: JsonOutput.toJson(entry.value.subTypes),
                ], "INSERT INTO element_type_definition (db_id, owner_db_id, element_type, custom_aspects, links, sub_types) VALUES (:id, :ownerId, :elementType, :customAspects :: jsonb, :links :: jsonb, :subTypes :: jsonb)")

                sql.execute([
                    ownerId: domainId,
                    defId: definitionId,
                ], "INSERT INTO domaintemplate_element_type_definitions (domaintemplate_db_id, element_type_definitions_db_id) VALUES (:ownerId, :defId)"
                )
            }
        }
    }

    private Map<String, Object> getDsgvoDefinitions() {
        [
            'asset': [
                'customAspects': [
                    'asset_details': [
                        'attributeSchemas': [
                            'asset_details_number': [
                                'type': 'integer'
                            ],
                            'asset_details_operatingStage': [
                                'enum': [
                                    'asset_details_operatingStage_operation',
                                    'asset_details_operatingStage_planning',
                                    'asset_details_operatingStage_test',
                                    'asset_details_operatingStage_rollout'
                                ]
                            ]]
                    ],
                    'asset_generalInformation': [
                        'attributeSchemas': [
                            'asset_generalInformation_document': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ]
                        ]]
                ],
                'links': [:],
                'subTypes': [
                    'AST_Application': [
                        'statuses': [
                            'IN_PROGRESS',
                            'NEW',
                            'RELEASED',
                            'FOR_REVIEW',
                            'ARCHIVED'
                        ]
                    ],
                    'AST_Datatype': [
                        'statuses': [
                            'IN_PROGRESS',
                            'NEW',
                            'RELEASED',
                            'FOR_REVIEW',
                            'ARCHIVED'
                        ]
                    ],
                    'AST_IT-System': [
                        'statuses': [
                            'IN_PROGRESS',
                            'NEW',
                            'RELEASED',
                            'FOR_REVIEW',
                            'ARCHIVED'
                        ]
                    ]]
            ],
            'control': [
                'customAspects': [
                    'control_dataProtection': [
                        'attributeSchemas': [
                            'control_dataProtection_objectives': [
                                'items': [
                                    'enum': [
                                        'control_dataProtection_objectives_pseudonymization',
                                        'control_dataProtection_objectives_confidentiality',
                                        'control_dataProtection_objectives_integrity',
                                        'control_dataProtection_objectives_availability',
                                        'control_dataProtection_objectives_resilience',
                                        'control_dataProtection_objectives_recoverability',
                                        'control_dataProtection_objectives_effectiveness',
                                        'control_dataProtection_objectives_encryption'
                                    ]
                                ],
                                'type': 'array'
                            ]
                        ]],
                    'control_generalInformation': [
                        'attributeSchemas': [
                            'control_generalInformation_document': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ]
                        ]],
                    'control_implementation': [
                        'attributeSchemas': [
                            'control_implementation_date': [
                                'format': 'date',
                                'type': 'string'
                            ],
                            'control_implementation_explanation': [
                                'type': 'string'
                            ],
                            'control_implementation_status': [
                                'enum': [
                                    'control_implementation_status_yes',
                                    'control_implementation_status_no',
                                    'control_implementation_status_partially',
                                    'control_implementation_status_notApplicable'
                                ]
                            ]]
                    ],
                    'control_revision': [
                        'attributeSchemas': [
                            'control_revision_comment': [
                                'type': 'string'
                            ],
                            'control_revision_last': [
                                'format': 'date',
                                'type': 'string'
                            ],
                            'control_revision_next': [
                                'format': 'date',
                                'type': 'string'
                            ]
                        ]]
                ],
                'links': [
                    'control_tom': [
                        'attributeSchemas': [:],
                        'targetType': 'control'
                    ]
                ],
                'subTypes': [
                    'CTL_TOM': [
                        'statuses': [
                            'IN_PROGRESS',
                            'NEW',
                            'RELEASED',
                            'FOR_REVIEW',
                            'ARCHIVED'
                        ]
                    ]]
            ],
            'document': [
                'customAspects': [
                    'document_details': [
                        'attributeSchemas': [
                            'document_details_approvalDate': [
                                'format': 'date',
                                'type': 'string'
                            ],
                            'document_details_classification': [
                                'enum': [
                                    'document_details_classification_public',
                                    'document_details_classification_internalUse',
                                    'document_details_classification_confidential',
                                    'document_details_classification_strictlyConfidential'
                                ]
                            ],
                            'document_details_otherTypeOfDocument': [
                                'type': 'string'
                            ],
                            'document_details_status': [
                                'enum': [
                                    'document_details_status_draft',
                                    'document_details_status_inProgress',
                                    'document_details_status_approvalRequested',
                                    'document_details_status_approved',
                                    'document_details_status_outOfDate',
                                    'document_details_status_notApplicable'
                                ]
                            ],
                            'document_details_typeOfDocument': [
                                'enum': [
                                    'document_details_typeOfDocument_policy',
                                    'document_details_typeOfDocument_contract',
                                    'document_details_typeOfDocument_agreement',
                                    'document_details_typeOfDocument_systemDocumentation',
                                    'document_details_typeOfDocument_researchResults',
                                    'document_details_typeOfDocument_trainingMaterials',
                                    'document_details_typeOfDocument_other'
                                ]
                            ],
                            'document_details_version': [
                                'type': 'string'
                            ]
                        ]],
                    'document_generalInformation': [
                        'attributeSchemas': [
                            'document_generalInformation_externalLink': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ]
                        ]],
                    'document_revision': [
                        'attributeSchemas': [
                            'document_revision_comment': [
                                'type': 'string'
                            ],
                            'document_revision_last': [
                                'format': 'date',
                                'type': 'string'
                            ],
                            'document_revision_next': [
                                'format': 'date',
                                'type': 'string'
                            ]
                        ]]
                ],
                'links': [:],
                'subTypes': [
                    'DOC_Document': [
                        'statuses': [
                            'IN_PROGRESS',
                            'NEW',
                            'RELEASED',
                            'FOR_REVIEW',
                            'ARCHIVED'
                        ]
                    ]]
            ],
            'incident': [
                'customAspects': [:],
                'links': [:],
                'subTypes': [
                    'INC_Incident': [
                        'statuses': [
                            'IN_PROGRESS',
                            'NEW',
                            'RELEASED',
                            'FOR_REVIEW',
                            'ARCHIVED'
                        ]
                    ]]
            ],
            'person': [
                'customAspects': [
                    'person_address': [
                        'attributeSchemas': [
                            'person_address_address1': [
                                'type': 'string'
                            ],
                            'person_address_address2': [
                                'type': 'string'
                            ],
                            'person_address_city': [
                                'type': 'string'
                            ],
                            'person_address_country': [
                                'type': 'string'
                            ],
                            'person_address_postcode': [
                                'type': 'string'
                            ],
                            'person_address_state': [
                                'type': 'string'
                            ]
                        ]],
                    'person_contactInformation': [
                        'attributeSchemas': [
                            'person_contactInformation_email': [
                                'type': 'string'
                            ],
                            'person_contactInformation_fax': [
                                'type': 'string'
                            ],
                            'person_contactInformation_mobile': [
                                'type': 'string'
                            ],
                            'person_contactInformation_office': [
                                'type': 'string'
                            ],
                            'person_contactInformation_private': [
                                'type': 'string'
                            ],
                            'person_contactInformation_website': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ]
                        ]],
                    'person_dataProtectionOfficer': [
                        'attributeSchemas': [
                            'person_dataProtectionOfficer_expertise': [
                                'type': 'boolean'
                            ],
                            'person_dataProtectionOfficer_profession': [
                                'type': 'string'
                            ]
                        ]],
                    'person_generalInformation': [
                        'attributeSchemas': [
                            'person_generalInformation_familyName': [
                                'type': 'string'
                            ],
                            'person_generalInformation_givenName': [
                                'type': 'string'
                            ],
                            'person_generalInformation_salutation': [
                                'type': 'string'
                            ],
                            'person_generalInformation_title': [
                                'type': 'string'
                            ]
                        ]]
                ],
                'links': [:],
                'subTypes': [
                    'PER_DataProtectionOfficer': [
                        'statuses': [
                            'IN_PROGRESS',
                            'NEW',
                            'RELEASED',
                            'FOR_REVIEW',
                            'ARCHIVED'
                        ]
                    ],
                    'PER_Person': [
                        'statuses': [
                            'IN_PROGRESS',
                            'NEW',
                            'RELEASED',
                            'FOR_REVIEW',
                            'ARCHIVED'
                        ]
                    ]]
            ],
            'process': [
                'customAspects': [
                    'process_accessAuthorization': [
                        'attributeSchemas': [
                            'process_accessAuthorization_concept': [
                                'type': 'boolean'
                            ],
                            'process_accessAuthorization_description': [
                                'type': 'string'
                            ],
                            'process_accessAuthorization_document': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ]
                        ]],
                    'process_dataProcessing': [
                        'attributeSchemas': [
                            'process_dataProcessing_document': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ],
                            'process_dataProcessing_explanation': [
                                'type': 'string'
                            ],
                            'process_dataProcessing_legalBasis': [
                                'items': [
                                    'enum': [
                                        'process_dataProcessing_legalBasis_Art6Abs1litaDSGVO',
                                        'process_dataProcessing_legalBasis_Art6Abs1litbDSGVO',
                                        'process_dataProcessing_legalBasis_Art6Abs1litcDSGVO',
                                        'process_dataProcessing_legalBasis_Art6Abs1litdDSGVO',
                                        'process_dataProcessing_legalBasis_Art6Abs1liteDSGVO',
                                        'process_dataProcessing_legalBasis_Art6Abs1litfDSGVO',
                                        'process_dataProcessing_legalBasis_Art6Abs2DSGVO',
                                        'process_dataProcessing_legalBasis_Art6Abs3DSGVO',
                                        'process_dataProcessing_legalBasis_Art9Abs2litaDSGVO',
                                        'process_dataProcessing_legalBasis_Art9Abs2litbDSGVO',
                                        'process_dataProcessing_legalBasis_Art9Abs2litcDSGVO',
                                        'process_dataProcessing_legalBasis_Art9Abs2litfDSGVO',
                                        'process_dataProcessing_legalBasis_Art9Abs2litgDSGVO',
                                        'process_dataProcessing_legalBasis_Art9Abs2lithDSGVO',
                                        'process_dataProcessing_legalBasis_Art9Abs2litiDSGVO',
                                        'process_dataProcessing_legalBasis_Art9Abs2litjDSGVO',
                                        'process_dataProcessing_legalBasis_Art88DSGVO',
                                        'process_dataProcessing_legalBasis_P26Abs1BDSG',
                                        'process_dataProcessing_legalBasis_P26Abs2BDSG',
                                        'process_dataProcessing_legalBasis_P26Abs4Satz1BDSG',
                                        'process_dataProcessing_legalBasis_P4BDSG'
                                    ]
                                ],
                                'type': 'array'
                            ],
                            'process_dataProcessing_otherLegalBasis': [
                                'type': 'string'
                            ]
                        ]],
                    'process_dataSubjects': [
                        'attributeSchemas': [
                            'process_dataSubjects_dataSubjects': [
                                'items': [
                                    'enum': [
                                        'process_dataSubjects_dataSubjects_shareholders',
                                        'process_dataSubjects_dataSubjects_employers',
                                        'process_dataSubjects_dataSubjects_employees',
                                        'process_dataSubjects_dataSubjects_controllers',
                                        'process_dataSubjects_dataSubjects_applicants',
                                        'process_dataSubjects_dataSubjects_parents',
                                        'process_dataSubjects_dataSubjects_externalConsultants',
                                        'process_dataSubjects_dataSubjects_externalServiceProvider',
                                        'process_dataSubjects_dataSubjects_interestedParties',
                                        'process_dataSubjects_dataSubjects_customers',
                                        'process_dataSubjects_dataSubjects_supplier',
                                        'process_dataSubjects_dataSubjects_clients',
                                        'process_dataSubjects_dataSubjects_tenants',
                                        'process_dataSubjects_dataSubjects_members',
                                        'process_dataSubjects_dataSubjects_newsletterSubscribers',
                                        'process_dataSubjects_dataSubjects_partners',
                                        'process_dataSubjects_dataSubjects_patients',
                                        'process_dataSubjects_dataSubjects_pupils',
                                        'process_dataSubjects_dataSubjects_students',
                                        'process_dataSubjects_dataSubjects_websiteVisitors'
                                    ]
                                ],
                                'type': 'array'
                            ],
                            'process_dataSubjects_otherDataSubjects': [
                                'type': 'string'
                            ]
                        ]],
                    'process_dataTransfer': [
                        'attributeSchemas': [
                            'process_dataTransfer_document': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ],
                            'process_dataTransfer_explanation': [
                                'type': 'string'
                            ],
                            'process_dataTransfer_legalBasis': [
                                'items': [
                                    'enum': [
                                        'process_dataTransfer_legalBasis_Art6Abs2DSGVO',
                                        'process_dataTransfer_legalBasis_Art6Abs3DSGVO',
                                        'process_dataTransfer_legalBasis_Art9Abs2litfDSGVO',
                                        'process_dataTransfer_legalBasis_P31aBPolG',
                                        'process_dataTransfer_legalBasis_P67bAbs1S1SGBX',
                                        'process_dataTransfer_legalBasis_P25BDSG',
                                        'process_dataTransfer_legalBasis_P24BDSG',
                                        'process_dataTransfer_legalBasis_P8IfSG',
                                        'process_dataTransfer_legalBasis_17aAbs4RoeVP28Abs8RooeV',
                                        'process_dataTransfer_legalBasis_P61StrlSchV',
                                        'process_dataTransfer_legalBasis_P18PStG',
                                        'process_dataTransfer_legalBasis_P294SGBV',
                                        'process_dataTransfer_legalBasis_P298SGBV',
                                        'process_dataTransfer_legalBasis_P138_139StGB',
                                        'process_dataTransfer_legalBasis_P64SGBVIII'
                                    ]
                                ],
                                'type': 'array'
                            ],
                            'process_dataTransfer_otherLegalBasis': [
                                'type': 'string'
                            ]
                        ]],
                    'process_generalInformation': [
                        'attributeSchemas': [
                            'process_generalInformation_document': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ]
                        ]],
                    'process_informationsObligations': [
                        'attributeSchemas': [
                            'process_informationsObligations_document': [
                                'type': 'string'
                            ],
                            'process_informationsObligations_explanation': [
                                'type': 'string'
                            ],
                            'process_informationsObligations_status': [
                                'type': 'boolean'
                            ]
                        ]],
                    'process_intendedPurpose': [
                        'attributeSchemas': [
                            'process_intendedPurpose_intendedPurpose': [
                                'type': 'string'
                            ]
                        ]],
                    'process_opinionDPO': [
                        'attributeSchemas': [
                            'process_opinionDPO_comment': [
                                'type': 'string'
                            ],
                            'process_opinionDPO_document': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ],
                            'process_opinionDPO_findings': [
                                'type': 'string'
                            ],
                            'process_opinionDPO_opinionDPO': [
                                'type': 'string'
                            ],
                            'process_opinionDPO_privacyImpactAssessment': [
                                'type': 'boolean'
                            ],
                            'process_opinionDPO_recommendations': [
                                'type': 'string'
                            ]
                        ]],
                    'process_privacyImpactAssessment': [
                        'attributeSchemas': [
                            'process_privacyImpactAssessment_blacklistComment': [
                                'type': 'string'
                            ],
                            'process_privacyImpactAssessment_blacklistDocument': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ],
                            'process_privacyImpactAssessment_comment': [
                                'type': 'string'
                            ],
                            'process_privacyImpactAssessment_document': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ],
                            'process_privacyImpactAssessment_otherExclusions': [
                                'type': 'boolean'
                            ],
                            'process_privacyImpactAssessment_otherExclusionsComment': [
                                'type': 'string'
                            ],
                            'process_privacyImpactAssessment_otherReasons': [
                                'type': 'string'
                            ],
                            'process_privacyImpactAssessment_processingCriteria': [
                                'items': [
                                    'enum': [
                                        'process_privacyImpactAssessment_processingCriteria_profiling',
                                        'process_privacyImpactAssessment_processingCriteria_automated',
                                        'process_privacyImpactAssessment_processingCriteria_monitoring',
                                        'process_privacyImpactAssessment_processingCriteria_specialCategories',
                                        'process_privacyImpactAssessment_processingCriteria_Art9',
                                        'process_privacyImpactAssessment_processingCriteria_Art10',
                                        'process_privacyImpactAssessment_processingCriteria_matching',
                                        'process_privacyImpactAssessment_processingCriteria_vulnerability',
                                        'process_privacyImpactAssessment_processingCriteria_newTechnology',
                                        'process_privacyImpactAssessment_processingCriteria_execution'
                                    ]
                                ],
                                'type': 'array'
                            ],
                            'process_privacyImpactAssessment_processingOnBlacklistArt35': [
                                'type': 'boolean'
                            ],
                            'process_privacyImpactAssessment_processingOnWhitelist': [
                                'type': 'boolean'
                            ],
                            'process_privacyImpactAssessment_processingOperationAccordingArt35': [
                                'type': 'boolean'
                            ],
                            'process_privacyImpactAssessment_supervisoryAuthorityComment': [
                                'type': 'string'
                            ],
                            'process_privacyImpactAssessment_supervisoryAuthorityConsulted': [
                                'type': 'boolean'
                            ],
                            'process_privacyImpactAssessment_twoProcessingCriteria': [
                                'type': 'boolean'
                            ],
                            'process_privacyImpactAssessment_twoProcessingCriteriaComment': [
                                'type': 'string'
                            ]
                        ]],
                    'process_processing': [
                        'attributeSchemas': [
                            'process_processing_asProcessor': [
                                'type': 'boolean'
                            ]
                        ]],
                    'process_processingDetails': [
                        'attributeSchemas': [
                            'process_processingDetails_comment': [
                                'type': 'string'
                            ],
                            'process_processingDetails_operatingStage': [
                                'enum': [
                                    'process_processingDetails_operatingStage_operation',
                                    'process_processingDetails_operatingStage_planning',
                                    'process_processingDetails_operatingStage_test',
                                    'process_processingDetails_operatingStage_rollout'
                                ]
                            ],
                            'process_processingDetails_responsibleDepartment': [
                                'type': 'string'
                            ],
                            'process_processingDetails_surveyConductedOn': [
                                'format': 'date',
                                'type': 'string'
                            ],
                            'process_processingDetails_typeOfSurvey': [
                                'enum': [
                                    'process_processingDetails_typeOfSurvey_new',
                                    'process_processingDetails_typeOfSurvey_change'
                                ]
                            ]]
                    ],
                    'process_recipient': [
                        'attributeSchemas': [
                            'process_recipient_type': [
                                'enum': [
                                    'process_recipient_type_internal',
                                    'process_recipient_type_external',
                                    'process_recipient_type_processor'
                                ]
                            ]]
                    ],
                    'process_tomAssessment': [
                        'attributeSchemas': [
                            'process_tomAssessment_certification': [
                                'type': 'boolean'
                            ],
                            'process_tomAssessment_certificationStandard': [
                                'type': 'string'
                            ],
                            'process_tomAssessment_comment': [
                                'type': 'string'
                            ],
                            'process_tomAssessment_overallAssessment': [
                                'type': 'string'
                            ],
                            'process_tomAssessment_securityConcept': [
                                'type': 'boolean'
                            ]
                        ]]
                ],
                'links': [
                    'process_controller': [
                        'attributeSchemas': [
                            'process_controller_document': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ]
                        ],
                        'targetSubType': 'SCP_Controller',
                        'targetType': 'scope'
                    ],
                    'process_dataTransmission': [
                        'attributeSchemas': [:],
                        'targetSubType': 'PRO_DataTransfer',
                        'targetType': 'process'
                    ],
                    'process_dataType': [
                        'attributeSchemas': [
                            'process_dataType_comment': [
                                'type': 'string'
                            ],
                            'process_dataType_dataOrigin': [
                                'enum': [
                                    'process_dataType_dataOrigin_direct',
                                    'process_dataType_dataOrigin_transmission'
                                ]
                            ],
                            'process_dataType_deletionPeriod': [
                                'enum': [
                                    'process_dataType_deletionPeriod_immediately',
                                    'process_dataType_deletionPeriod_7d',
                                    'process_dataType_deletionPeriod_10d',
                                    'process_dataType_deletionPeriod_14d',
                                    'process_dataType_deletionPeriod_21d',
                                    'process_dataType_deletionPeriod_1m',
                                    'process_dataType_deletionPeriod_3m',
                                    'process_dataType_deletionPeriod_6m',
                                    'process_dataType_deletionPeriod_9m',
                                    'process_dataType_deletionPeriod_1y',
                                    'process_dataType_deletionPeriod_4y',
                                    'process_dataType_deletionPeriod_7y',
                                    'process_dataType_deletionPeriod_30y'
                                ]
                            ],
                            'process_dataType_deletionPeriodStart': [
                                'enum': [
                                    'process_dataType_deletionPeriodStart_completion',
                                    'process_dataType_deletionPeriodStart_appplicationClosure',
                                    'process_dataType_deletionPeriodStart_deletionRequest',
                                    'process_dataType_deletionPeriodStart_endOfRelationship',
                                    'process_dataType_deletionPeriodStart_purposeDiscontinuation',
                                    'process_dataType_deletionPeriodStart_dataCollection',
                                    'process_dataType_deletionPeriodStart_consentWithdrawal'
                                ]
                            ],
                            'process_dataType_descriptionDeletionProcedure': [
                                'type': 'string'
                            ],
                            'process_dataType_otherDataOrigin': [
                                'type': 'string'
                            ],
                            'process_dataType_otherDeletionPeriod': [
                                'type': 'string'
                            ],
                            'process_dataType_otherDeletionPeriodStart': [
                                'type': 'string'
                            ]
                        ],
                        'targetSubType': 'AST_Datatype',
                        'targetType': 'asset'
                    ],
                    'process_externalRecipient': [
                        'attributeSchemas': [
                            'process_externalRecipient_thirdCountryDocument': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ],
                            'process_externalRecipient_thirdCountryExplanation': [
                                'type': 'string'
                            ],
                            'process_externalRecipient_thirdCountryGuarantees': [
                                'type': 'string'
                            ],
                            'process_externalRecipient_thirdCountryName': [
                                'type': 'string'
                            ],
                            'process_externalRecipient_thirdCountryProcessing': [
                                'type': 'boolean'
                            ]
                        ],
                        'targetSubType': 'PER_Person',
                        'targetType': 'person'
                    ],
                    'process_internalRecipient': [
                        'attributeSchemas': [
                            'process_internalRecipient_thirdCountryDocument': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ],
                            'process_internalRecipient_thirdCountryExplanation': [
                                'type': 'string'
                            ],
                            'process_internalRecipient_thirdCountryGuarantees': [
                                'type': 'string'
                            ],
                            'process_internalRecipient_thirdCountryName': [
                                'type': 'string'
                            ],
                            'process_internalRecipient_thirdCountryProcessing': [
                                'type': 'boolean'
                            ]
                        ],
                        'targetSubType': 'PER_Person',
                        'targetType': 'person'
                    ],
                    'process_jointControllership': [
                        'attributeSchemas': [
                            'process_jointControllership_document': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ]
                        ],
                        'targetSubType': 'SCP_JoinController',
                        'targetType': 'scope'
                    ],
                    'process_processor': [
                        'attributeSchemas': [
                            'process_processor_thirdCountryDocument': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ],
                            'process_processor_thirdCountryExplanation': [
                                'type': 'string'
                            ],
                            'process_processor_thirdCountryGuarantees': [
                                'type': 'string'
                            ],
                            'process_processor_thirdCountryName': [
                                'type': 'string'
                            ],
                            'process_processor_thirdCountryProcessing': [
                                'type': 'boolean'
                            ]
                        ],
                        'targetSubType': 'SCP_Processor',
                        'targetType': 'scope'
                    ],
                    'process_requiredApplications': [
                        'attributeSchemas': [:],
                        'targetSubType': 'AST_Application',
                        'targetType': 'asset'
                    ],
                    'process_requiredITSystems': [
                        'attributeSchemas': [:],
                        'targetSubType': 'AST_IT-System',
                        'targetType': 'asset'
                    ],
                    'process_responsibleBody': [
                        'attributeSchemas': [:],
                        'targetSubType': 'SCP_ResponsibleBody',
                        'targetType': 'scope'
                    ],
                    'process_responsiblePerson': [
                        'attributeSchemas': [:],
                        'targetSubType': 'PER_Person',
                        'targetType': 'person'
                    ],
                    'process_tom': [
                        'attributeSchemas': [:],
                        'targetSubType': 'CTL_TOM',
                        'targetType': 'control'
                    ]
                ],
                'subTypes': [
                    'PRO_DataProcessing': [
                        'statuses': [
                            'IN_PROGRESS',
                            'NEW',
                            'RELEASED',
                            'FOR_REVIEW',
                            'ARCHIVED'
                        ]
                    ],
                    'PRO_DataTransfer': [
                        'statuses': [
                            'IN_PROGRESS',
                            'NEW',
                            'RELEASED',
                            'FOR_REVIEW',
                            'ARCHIVED'
                        ]
                    ]]
            ],
            'scenario': [
                'customAspects': [
                    'scenario_generalInformation': [
                        'attributeSchemas': [
                            'scenario_generalInformation_document': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ]
                        ]],
                    'scenario_threat': [
                        'attributeSchemas': [
                            'scenario_threat_otherType': [
                                'type': 'string'
                            ],
                            'scenario_threat_type': [
                                'enum': [
                                    'scenario_threat_type_undirectedAttacks',
                                    'scenario_threat_type_criminalAct',
                                    'scenario_threat_type_forceMajeure',
                                    'scenario_threat_type_identityAbuse',
                                    'scenario_threat_type_internalOffender',
                                    'scenario_threat_type_dependencyOnThirdParties',
                                    'scenario_threat_type_unauthorizedAccess',
                                    'scenario_threat_type_damageIT',
                                    'scenario_threat_type_malware',
                                    'scenario_threat_type_socialEngineering',
                                    'scenario_threat_type_targetedAttacks',
                                    'scenario_threat_type_lossOfStoredInformation',
                                    'scenario_threat_type_damageEquipment',
                                    'scenario_threat_type_overload',
                                    'scenario_threat_type_criminal',
                                    'scenario_threat_type_lossOfConfidentiality',
                                    'scenario_threat_type_lossOfLiability',
                                    'scenario_threat_type_lossOfAdministration',
                                    'scenario_threat_type_exploitation',
                                    'scenario_threat_type_systemFailure',
                                    'scenario_threat_type_wlanFailure',
                                    'scenario_threat_type_other'
                                ]
                            ]]
                    ],
                    'scenario_vulnerability': [
                        'attributeSchemas': [
                            'scenario_vulnerability_otherType': [
                                'type': 'string'
                            ],
                            'scenario_vulnerability_type': [
                                'enum': [
                                    'scenario_vulnerability_type_organisationalDeficiencies',
                                    'scenario_vulnerability_type_technicalVulnerabilities',
                                    'scenario_vulnerability_type_technicalBreakdown',
                                    'scenario_vulnerability_type_humanMistakes',
                                    'scenario_vulnerability_type_lackOfInfrastructure',
                                    'scenario_vulnerability_type_networkDefects',
                                    'scenario_vulnerability_type_couplingOfServices',
                                    'scenario_vulnerability_type_intentionalAction',
                                    'scenario_vulnerability_type_other'
                                ]
                            ]]
                    ]],
                'links': [:],
                'subTypes': [
                    'SCN_Scenario': [
                        'statuses': [
                            'IN_PROGRESS',
                            'NEW',
                            'RELEASED',
                            'FOR_REVIEW',
                            'ARCHIVED'
                        ]
                    ]]
            ],
            'scope': [
                'customAspects': [
                    'scope_address': [
                        'attributeSchemas': [
                            'scope_address_address1': [
                                'type': 'string'
                            ],
                            'scope_address_address2': [
                                'type': 'string'
                            ],
                            'scope_address_city': [
                                'type': 'string'
                            ],
                            'scope_address_country': [
                                'type': 'string'
                            ],
                            'scope_address_postcode': [
                                'type': 'string'
                            ],
                            'scope_address_state': [
                                'type': 'string'
                            ]
                        ]],
                    'scope_contactInformation': [
                        'attributeSchemas': [
                            'scope_contactInformation_email': [
                                'type': 'string'
                            ],
                            'scope_contactInformation_fax': [
                                'type': 'string'
                            ],
                            'scope_contactInformation_phone': [
                                'type': 'string'
                            ],
                            'scope_contactInformation_website': [
                                'type': 'string'
                            ]
                        ]],
                    'scope_generalInformation': [
                        'attributeSchemas': [
                            'scope_generalInformation_document': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ]
                        ]],
                    'scope_regulatoryAuthority': [
                        'attributeSchemas': [
                            'scope_regulatoryAuthority_address1': [
                                'type': 'string'
                            ],
                            'scope_regulatoryAuthority_address2': [
                                'type': 'string'
                            ],
                            'scope_regulatoryAuthority_city': [
                                'type': 'string'
                            ],
                            'scope_regulatoryAuthority_country': [
                                'type': 'string'
                            ],
                            'scope_regulatoryAuthority_description': [
                                'type': 'string'
                            ],
                            'scope_regulatoryAuthority_email': [
                                'type': 'string'
                            ],
                            'scope_regulatoryAuthority_fax': [
                                'type': 'string'
                            ],
                            'scope_regulatoryAuthority_name': [
                                'type': 'string'
                            ],
                            'scope_regulatoryAuthority_phone': [
                                'type': 'string'
                            ],
                            'scope_regulatoryAuthority_postcode': [
                                'type': 'string'
                            ],
                            'scope_regulatoryAuthority_state': [
                                'type': 'string'
                            ],
                            'scope_regulatoryAuthority_website': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ]
                        ]],
                    'scope_thirdCountry': [
                        'attributeSchemas': [
                            'scope_thirdCountry_address1': [
                                'type': 'string'
                            ],
                            'scope_thirdCountry_address2': [
                                'type': 'string'
                            ],
                            'scope_thirdCountry_city': [
                                'type': 'string'
                            ],
                            'scope_thirdCountry_country': [
                                'type': 'string'
                            ],
                            'scope_thirdCountry_description': [
                                'type': 'string'
                            ],
                            'scope_thirdCountry_email': [
                                'type': 'string'
                            ],
                            'scope_thirdCountry_fax': [
                                'type': 'string'
                            ],
                            'scope_thirdCountry_name': [
                                'type': 'string'
                            ],
                            'scope_thirdCountry_phone': [
                                'type': 'string'
                            ],
                            'scope_thirdCountry_postcode': [
                                'type': 'string'
                            ],
                            'scope_thirdCountry_state': [
                                'type': 'string'
                            ],
                            'scope_thirdCountry_status': [
                                'type': 'boolean'
                            ],
                            'scope_thirdCountry_website': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ]
                        ]]
                ],
                'links': [
                    'scope_dataProtectionOfficer': [
                        'attributeSchemas': [
                            'scope_dataProtectionOfficer_affiliation': [
                                'enum': [
                                    'scope_dataProtectionOfficer_affiliation_internal',
                                    'scope_dataProtectionOfficer_affiliation_external'
                                ]
                            ],
                            'scope_dataProtectionOfficer_comment': [
                                'type': 'string'
                            ],
                            'scope_dataProtectionOfficer_nomination': [
                                'type': 'boolean'
                            ],
                            'scope_dataProtectionOfficer_nominationDate': [
                                'format': 'date',
                                'type': 'string'
                            ],
                            'scope_dataProtectionOfficer_nominationEvidence': [
                                'format': 'uri',
                                'pattern': '^(https?|ftp)://',
                                'type': 'string'
                            ]
                        ],
                        'targetSubType': 'PER_DataProtectionOfficer',
                        'targetType': 'person'
                    ],
                    'scope_headOfDataProcessing': [
                        'attributeSchemas': [:],
                        'targetSubType': 'PER_Person',
                        'targetType': 'person'
                    ],
                    'scope_informationSecurityOfficer': [
                        'attributeSchemas': [:],
                        'targetSubType': 'SCP_Controller',
                        'targetType': 'scope'
                    ],
                    'scope_management': [
                        'attributeSchemas': [:],
                        'targetSubType': 'PER_Person',
                        'targetType': 'person'
                    ]
                ],
                'subTypes': [
                    'SCP_Controller': [
                        'statuses': [
                            'IN_PROGRESS',
                            'NEW',
                            'RELEASED',
                            'FOR_REVIEW',
                            'ARCHIVED'
                        ]
                    ],
                    'SCP_JointController': [
                        'statuses': [
                            'IN_PROGRESS',
                            'NEW',
                            'RELEASED',
                            'FOR_REVIEW',
                            'ARCHIVED'
                        ]
                    ],
                    'SCP_Processor': [
                        'statuses': [
                            'IN_PROGRESS',
                            'NEW',
                            'RELEASED',
                            'FOR_REVIEW',
                            'ARCHIVED'
                        ]
                    ],
                    'SCP_ResponsibleBody': [
                        'statuses': [
                            'IN_PROGRESS',
                            'NEW',
                            'RELEASED',
                            'FOR_REVIEW',
                            'ARCHIVED'
                        ]
                    ],
                    'SCP_Scope': [
                        'statuses': [
                            'IN_PROGRESS',
                            'NEW',
                            'RELEASED',
                            'FOR_REVIEW',
                            'ARCHIVED'
                        ]
                    ]]
            ]]
    }
}
