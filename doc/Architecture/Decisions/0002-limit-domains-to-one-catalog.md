# 2. Domains auf einen Katalog begrenzen

Date: 2023-03-24

## Status

Accepted

## Context

As we do not need several catalogs per domain (template) and want to offer additional template elements in the form of profiles, we should simplify the object model. If there is only one catalog per domain (template), the catalog no longer needs a name and loses its raison d'être. Instead, we can assign catalog items directly to the domain.

If there are to be several catalogs per domain, several domains would actually have to be created. The domain should describe the entire subject area, with all possible catalog items that can be created.

### See: issue #2014


## Decision

There is now only one catalog per domain.

Technically: the catalog object is no longer necessary, we only have a list of catalog items in the domain.



## Consequences

The handling of catalogs in domains is simplified.
