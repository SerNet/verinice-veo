# 3. Define Domain Boundaries

Date: 2024-09-10

## Status

Accepted

## Context

Elements can belong to one or more domains. We have to decide what this assignment expresses, as well as what this should and should not enable the user to do.


## Decision

- An element is not *in* a domain
- An element is associated with a domain
	- The association is defined by having a subtype from that domain
	- The subtype has to be set explicitely by the user
	- This means that the element is associated with the domain explicitely
	- Each subtype has a list of status values
	- The status must not be empty
- Associating an element with a domain means that the element becomes visible to users who operate in the context of that domain and that the element is now allowed to hold attributes / aspects that are defined in that domain
- Although an element is never contained in a domain, it is possible to manage an element in the context of an associated domain (or "from the viewpoint of a domain"). Using the domain-specific element API you can work with an element representation that only contains aspects for a certain domain, plus basic (non-domain-specific) attributes. The paths for this API (e.g. GET /domains/{domainId}/assets/{assetId}) present the element as a sub-resource of the domain, but this does NOT mean that the domain contains the element. It means that you are viewing the element in the context of that domain.
- An element can have attributes that are defined in an associated domain
- These attributes include custom aspects, risk values, decision results and others
- An element has one single identity across all domains, i.e. the version no of the element increases when a change is made in any domain.
	- An element has one transaction boundary across all domains, i.e. changing values in one domain will cause conflicts when a change is made at the same time in another domain. The conflict is detected by optimistic locking.
- An element has only one subtype per domain. When adding an existing element to another domain, the user must choose the additionally assigned subtype for every element.


### Delimination / Exclusions

- Domains are not used to document the structure of elements, only their attributes.
	- i.e. composite relationships, scope memberships, risks and CIs/RIs should not be domain-specific.

- An exception to this are custom links which are defined in a domain. They document relationships between elements.

- Domains are not a secure way to separate element visibility between users. This needs to be implemented i.e. using account-groups and -roles. It is possible to design a role attribute that requires visibility of a certain domain. This needs to be defined separately and is outside the scope of this ADR.


## Consequences


An element's attachment to one or more domains must be managed over its lifecycle and the lifecycle of the domain.
