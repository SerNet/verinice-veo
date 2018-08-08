# veo-schemas
veo supports dynamic modeling. Hence very few assumption on the structure on elements stored and edited
can be made.

To handle (display, store etc.) such elements properly, veo makes use of JSON schemas draft-4. This document describes how these
schemas correlate and are used by veo.

We use JSON schema draft-4 because [java-json-tools/json-schema-validator](https://github.com/java-json-tools/json-schema-validator)
doesn't support any later draft. Unfortunately [everit-org/json-schema](https://github.com/everit-org/json-schema)
depends on [org.json API](https://github.com/stleary/JSON-java) whose license we cannot accept.

## Notations
Throughout this document we use the following terms.

-	**element**: the data edited by the users stored by the API
-	**schema**: the schema, customizable by the client, e.g. [order.json][]
-	**meta-schema**: the schema describing the structure of the schema, restricting the possibilities the client can
	customize the schema, e.g. [meta.json][].

We say that:

-	An element is an instance of a schema.
-	A schema is an instance of *the* meta-schema.

and

-	The meta-schema defines subschemas, called *type-schemas* which, if matched, define a type of a property defined in the
	schema.
-	The schema defines schemas for properties, called *property-schemas*. A schema can only define properties of types
	defined in the meta-schema.

E.g. [order.json][] is an instance of the meta-schema [meta.json][].

**NOTE** There can be several schemas, arbitrary and unknown to the developer, but only one meta-schema.

## Strict Typing
To keep things simple, the meta-schema only defines primitive types or sets (array with unique elements)
of such. No nested objects are allowed.

In addition, types shall be well-defined, i.e. a property-schema defined in the schema has to match one type-schema in
the meta-schema only. This is achieved by the `oneOf` directive in the `patternProperties` object in the meta-schema. We
say that the typing by the meta-schema is strict.

By convention

- each type-schema is defined in `#definitions`
- each type-schema in the `oneOf` array is defined using JSON reference

This ensures that each type-schema can be named by the JSON path in the meta-schema, e.g. the property-schema `orderTime` of an
order defined in [order.json][] matches the type-schema `#/definitions/dateTime` in [meta.json][] only (`oneOf`) and can
therefore be called a property of type `dateTime`.

### Example
[meta.json] defines the type `dateTime` or `#/definitions/dateTime` in the following way:

	{
	  "definitions": {
	    "dateTime": {
	      "type": "object",
	      "properties": {
	        "type": {
	          "enum": [
	            "string"
	          ]
	        },
	        "title": {
	          "type": "string"
	        },
	        "description": {
	          "type": "string"
	        },
	        "format": {
	          "enum": [
	            "date-time"
	          ]
	        }
	      },
	      "required": [
	        "type",
	        "title",
	        "format"
	      ]
	    }
	  }
	}

Then in [order.json][] a property `orderTime` of type `dateTime` can be defined like this:

	{
	  "properties": {
	    "orderTime": {
	      "type": "string",
	      "title": "The time the order has been made",
	      "format": "date-time",
	      "description": "An example property of type dateTime"
	    }
	  }
	}

**NOTE** Typing is implicit.

## UI Rendering
Strict typing and named type-schemas make UI rendering quite simple. A UI render engine is free to define UI elements
to render for each type-schema. No further meta information is needed.

### UI-Schema
As a matter of fact the meta-schema lacks information to cover flexible UI rendering. A property is of type `boolean`
and has no information whether to use a checkbox or toggle switch. This restriction has been made deliberately.
meta-schema and schema describe the structure of the data and not how they are presented, similar to HTML and CSS. Such
information shall be given by a UI-schema which is still to be defined.

[order.json]: v2020-json-validation/src/test/resources/order.json "An example element schema"
[meta.json]: v2020-json-validation/src/main/resources/meta.json "The meta-schema"

