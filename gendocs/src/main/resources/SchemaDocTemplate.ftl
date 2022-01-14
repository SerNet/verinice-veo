<#macro printCustomSchemas schemas>
<#if schemas?has_content>
<#list schemas as schema>
==== <#if schema.name?has_content>${schema.name} (${schema.key})<#else>${schema.key} (No name found.)</#if>
<#if schema.translation?has_content>
Translation: ${schema.translation} +
</#if>
<#if schema.targetType!?has_content>
Target Type: ${schema.targetType} +
</#if>
<#if schema.targetSubType!?has_content>
Target Subtype: ${schema.targetSubType} +
</#if>

<#if schema.attributes?has_content>
<#list schema.attributes as attribute>
===== ${attribute.title}
<#if attribute.translation?has_content>
Translation: ${attribute.translation} +
</#if>
Key: ${attribute.key} +
<#if attribute.type?has_content>
Type: <#compress><@printAttributeType attribute/></#compress>
</#if>

</#list>
<#else>
No attributes.

</#if>
</#list>
<#else>
None.

</#if>
</#macro>

<#macro printAttributeType attribute>
<#switch attribute.type>
<#case "boolean">
boolean, either true or false +
<#break>
<#case "string">
<#switch attribute.format!>
<#case "uri">
String with "uri" format, for example: "https://verinice.com/veo" +
<#break>
<#case "date">
String with "date" format, for example: "1/1/2021" +
<#break>
<#default>
String for example: "text" +
</#switch>
<#break>
<#case "integer">
Integer, for example: 741 +
<#break>
<#case "array">
One of: +
<#list attribute.oneOf as attributeType>
- ${attributeType.key} +
EN: ${attributeType.english} DE: ${attributeType.german}  +
</#list>
<#break>
<#default>
${attribute.type} +
</#switch>
</#macro>

= Schemas
verinice.veo CustomAspects und -Links
:toc:

<#list types as type>
== ${type.name}
Subtypes: ${type.subTypes?join(", ")}

=== Custom Aspects (${type.aspects?size})
<@printCustomSchemas type.aspects/>

=== Custom Links (${type.links?size})
<@printCustomSchemas type.links/>
</#list>