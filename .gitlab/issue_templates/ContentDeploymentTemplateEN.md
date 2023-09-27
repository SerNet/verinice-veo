# Content-Deployment
Please deploy the VEO content as described below.

# Notes
_Any additional side notes / remarks if required._

# Required Information and IDs
Fill out all fields. Mark as `?` if unknown.

1. __Source environment:__ `DEV, STAGE, PROD, ...`
1. __Target environment(s):__ `DEV, STAGE, PROD, ...`
1. __Client-ID in source:__ `<UUID>`
1. __Deploy whole domain:__ `YES, NO`
1. __If NO: deploy only these types__: `process, asset, scope, ...`
1. __Create domain template from domain:__ `YES, NO`
	1. __Domain-ID in source:__ `<UUID>`
	1. __New version no for domain template:__ `0.0.0 `
	1. __If NO: Use existing domain template:__ `YES, NO`
1. __Unit-ID to become a profile:__ `<UUID>`
	1. __Profile key:__ <Profilekey>
	1. __Profile name:__ `<Profilename>`
	1. __Profile description:__ `<A description...>`
1. __Also deploy forms:__ `YES, NO`
	2. __Create new form template bundle:__ `YES, NO`
	1. __If NO: use existing form template bundle:__ `<UUID>`
1. __Create domains from domain template in new environment:__ `YES, NO`
	1. __Yes, but only for these clients:__ `<UUIDs>`
1. __Migrate clients to new domain version:__ `YES, NO`
	1. __Yes—but only these clients:__ `<UUIDs>`

# Artifacts

- Domain template: `URL-TO-GIT-RESOURCE`
- Form template bundle: `URL-TO-GIT-RESOURCE`

# Acceptance criteria

- [ ] DT (or single element types) successfully deployed
- [ ] Form template bundle successfully deployed
- [ ] Domains were created
- [ ] Clients' data was migrated to new domain
- [ ] All relevant artifacts (domain template, form template bundle, ...) are committed to GIT repository

---

/label ~"1.Component::5.Undefined"

/label ~"2.IssueType::2.Bug"

/label ~"3.Status::1.Specification"

/label ~"4.Priority::5.Undefined"

/label ~"5.Impact::5.Undefined"

/label ~"6.Urgency::5.Undefined"
