/*******************************************************************************
 * Copyright (c) 2019 Urs Zeidler.
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
package org.veo.core.entity;

/**
 * Describes the model.
 */
public interface ModelPackage {
    public static final String ELEMENT_ENTITYLAYERSUPERTYPE = "element_EntityLayerSupertype";
    public static final String ELEMENT_ENTITYLAYERSUPERTYPE_NAME = "element_EntityLayerSupertype_name";
    public static final String ELEMENT_ENTITYLAYERSUPERTYPE_ABBREVIATION = "element_EntityLayerSupertype_abbreviation";
    public static final String ELEMENT_ENTITYLAYERSUPERTYPE_DESCRIPTION = "element_EntityLayerSupertype_description";
    public static final String ELEMENT_ENTITYLAYERSUPERTYPE_DOMAINS = "element_EntityLayerSupertype_domains";
    public static final String ELEMENT_ENTITYLAYERSUPERTYPE_LINKS = "element_EntityLayerSupertype_links";
    public static final String ELEMENT_ENTITYLAYERSUPERTYPE_CUSTOMASPECTS = "element_EntityLayerSupertype_customAspects";
    public static final String ELEMENT_ENTITYLAYERSUPERTYPE_OWNER = "element_EntityLayerSupertype_owner";
    public static final String ELEMENT_PERSON = "element_Person";
    public static final String ELEMENT_PERSON_NAME = "element_Person_name";
    public static final String ELEMENT_PERSON_ABBREVIATION = "element_Person_abbreviation";
    public static final String ELEMENT_PERSON_DESCRIPTION = "element_Person_description";
    public static final String ELEMENT_PERSON_DOMAINS = "element_Person_domains";
    public static final String ELEMENT_PERSON_LINKS = "element_Person_links";
    public static final String ELEMENT_PERSON_CUSTOMASPECTS = "element_Person_customAspects";
    public static final String ELEMENT_PERSON_OWNER = "element_Person_owner";
    public static final String ELEMENT_ASSET = "element_Asset";
    public static final String ELEMENT_ASSET_NAME = "element_Asset_name";
    public static final String ELEMENT_ASSET_ABBREVIATION = "element_Asset_abbreviation";
    public static final String ELEMENT_ASSET_DESCRIPTION = "element_Asset_description";
    public static final String ELEMENT_ASSET_DOMAINS = "element_Asset_domains";
    public static final String ELEMENT_ASSET_LINKS = "element_Asset_links";
    public static final String ELEMENT_ASSET_CUSTOMASPECTS = "element_Asset_customAspects";
    public static final String ELEMENT_ASSET_OWNER = "element_Asset_owner";
    public static final String ELEMENT_PROCESS = "element_Process";
    public static final String ELEMENT_PROCESS_NAME = "element_Process_name";
    public static final String ELEMENT_PROCESS_ABBREVIATION = "element_Process_abbreviation";
    public static final String ELEMENT_PROCESS_DESCRIPTION = "element_Process_description";
    public static final String ELEMENT_PROCESS_DOMAINS = "element_Process_domains";
    public static final String ELEMENT_PROCESS_LINKS = "element_Process_links";
    public static final String ELEMENT_PROCESS_CUSTOMASPECTS = "element_Process_customAspects";
    public static final String ELEMENT_PROCESS_OWNER = "element_Process_owner";
    public static final String ELEMENT_DOCUMENT = "element_Document";
    public static final String ELEMENT_DOCUMENT_NAME = "element_Document_name";
    public static final String ELEMENT_DOCUMENT_ABBREVIATION = "element_Document_abbreviation";
    public static final String ELEMENT_DOCUMENT_DESCRIPTION = "element_Document_description";
    public static final String ELEMENT_DOCUMENT_DOMAINS = "element_Document_domains";
    public static final String ELEMENT_DOCUMENT_LINKS = "element_Document_links";
    public static final String ELEMENT_DOCUMENT_CUSTOMASPECTS = "element_Document_customAspects";
    public static final String ELEMENT_DOCUMENT_OWNER = "element_Document_owner";
    public static final String ELEMENT_CONTROL = "element_Control";
    public static final String ELEMENT_CONTROL_NAME = "element_Control_name";
    public static final String ELEMENT_CONTROL_ABBREVIATION = "element_Control_abbreviation";
    public static final String ELEMENT_CONTROL_DESCRIPTION = "element_Control_description";
    public static final String ELEMENT_CONTROL_DOMAINS = "element_Control_domains";
    public static final String ELEMENT_CONTROL_LINKS = "element_Control_links";
    public static final String ELEMENT_CONTROL_CUSTOMASPECTS = "element_Control_customAspects";
    public static final String ELEMENT_CONTROL_OWNER = "element_Control_owner";
    public static final String ELEMENT_CLIENT = "element_Client";
    public static final String ELEMENT_CLIENT_NAME = "element_Client_name";
    public static final String ELEMENT_CLIENT_UNITS = "element_Client_units";
    public static final String ELEMENT_CLIENT_DOMAINS = "element_Client_domains";
    public static final String ELEMENT_DOMAIN = "element_Domain";
    public static final String ELEMENT_DOMAIN_NAME = "element_Domain_name";
    public static final String ELEMENT_DOMAIN_ABBREVIATION = "element_Domain_abbreviation";
    public static final String ELEMENT_DOMAIN_DESCRIPTION = "element_Domain_description";
    public static final String ELEMENT_DOMAIN_ACTIVE = "element_Domain_active";
    public static final String ELEMENT_NAMEABLE = "element_NameAble";
    public static final String ELEMENT_NAMEABLE_NAME = "element_NameAble_name";
    public static final String ELEMENT_NAMEABLE_ABBREVIATION = "element_NameAble_abbreviation";
    public static final String ELEMENT_NAMEABLE_DESCRIPTION = "element_NameAble_description";
    public static final String ELEMENT_UNIT = "element_Unit";
    public static final String ELEMENT_UNIT_NAME = "element_Unit_name";
    public static final String ELEMENT_UNIT_ABBREVIATION = "element_Unit_abbreviation";
    public static final String ELEMENT_UNIT_DESCRIPTION = "element_Unit_description";
    public static final String ELEMENT_UNIT_CLIENT = "element_Unit_client";
    public static final String ELEMENT_UNIT_OBJECTS = "element_Unit_objects";
    public static final String ELEMENT_UNIT_UNITS = "element_Unit_units";
    public static final String ELEMENT_UNIT_PARENT = "element_Unit_parent";
    public static final String ELEMENT_UNIT_DOMAINS = "element_Unit_domains";
    public static final String ELEMENT_CUSTOMLINK = "element_CustomLink";
    public static final String ELEMENT_CUSTOMLINK_NAME = "element_CustomLink_name";
    public static final String ELEMENT_CUSTOMLINK_ABBREVIATION = "element_CustomLink_abbreviation";
    public static final String ELEMENT_CUSTOMLINK_DESCRIPTION = "element_CustomLink_description";
    public static final String ELEMENT_CUSTOMLINK_TYPE = "element_CustomLink_type";
    public static final String ELEMENT_CUSTOMLINK_APPLICABLETO = "element_CustomLink_applicableTo";
    public static final String ELEMENT_CUSTOMLINK_DOMAINS = "element_CustomLink_domains";
    public static final String ELEMENT_CUSTOMLINK_TARGET = "element_CustomLink_target";
    public static final String ELEMENT_CUSTOMLINK_SOURCE = "element_CustomLink_source";
    public static final String ELEMENT_CUSTOMPROPERTIES = "element_CustomProperties";
    public static final String ELEMENT_CUSTOMPROPERTIES_TYPE = "element_CustomProperties_type";
    public static final String ELEMENT_CUSTOMPROPERTIES_APPLICABLETO = "element_CustomProperties_applicableTo";
    public static final String ELEMENT_CUSTOMPROPERTIES_DOMAINS = "element_CustomProperties_domains";
    public static final String ELEMENT_ABSTRACTASPECT = "element_AbstractAspect";
    public static final String ELEMENT_ABSTRACTASPECT_DOMAINS = "element_AbstractAspect_domains";
    public static final String ELEMENT_TIMERANGE = "element_TimeRange";
    public static final String ELEMENT_TIMERANGE_DOMAINS = "element_TimeRange_domains";
    public static final String ELEMENT_TIMERANGE_VALUE = "element_TimeRange_value";

}
