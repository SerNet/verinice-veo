# Content-Development-Relase-and-Deployment

Deploy VEO content as described below.

## Abbreviations used:

-  DT: Domain Template
-  FTB: Form Template Bundle
-  CC: Content Creator
-  DEV: Developer
-  OPS: Operations Team

# Notes
_Any additional side notes / remarks if required._

# Action Items

Complete this work instruction by filling out all fields when creating the issue. Mark as `?` if unknown. Change defaults when needed.
Mark individual items as DONE during implementation.


## __Source environment:__ `STAGE`
1. __Client-ID in source:__ _Content-Creator Client_ (or specify alternate UUID here:) `...`
1. [ ] CC: Update all profiles in source domain as necessary
1. [ ] CC: Update all catalog-items in source domain as necessary
1. [ ] CC: Ensure no translation conflicts are present in source domain
1. [ ] DEV/CC: Call the endpoint `breaking-changes/` and check for entries
1. [ ] DEV: If _breaking-changes_ are present, write migration instructions
1. __Create domain template from domain:__
	1. __Domain-ID in source:__ `<UUID>`
	1. __New version no for domain template:__ `0.0.0 `
	1. [ ] DEV: Create DT
	1. [ ] DEV: Download DT and store in content-repository
	1. [ ] DEV: Download FTB and store in content-repository

## __CC: Specify Internal Target environment(s):__

- [X] `STAGE`
- [X] `DEVELOP`

## __CC: Specify External Target environment(s):__

CC: Remove any external environments from the list that should **not** be targeted.
DEV: After successful AT on develop/stage, give this list of environments to the Ops-Team via ticket and then mark these items here as DONE.

- [ ] `GCP`
- [ ] `PROD`
- [ ] `SANDBOX`
- [ ] `TALOS-SERNET`
- [ ] `TALOS-ONPREM-REFERENCE`
- [ ] `...`


## __DEV: Create domains from domain template in develop/staging environments:__
1. __Limit to these clients:__ `ALL WITH PREVIOUS DOMAIN FROM SAME TEMPLATE` (default)
1. [ ] DEV: Upload DT into specified environments
1. [ ] DEV: Upload FTB into specified environments
1. [ ] DEV: Create domains in specified environments
1. [ ] DEV: Migrate clients in specified environments

# Created Artifacts after sucessful creation

- Domain template: `URL-TO-GIT-RESOURCE`
- Form template bundle: `URL-TO-GIT-RESOURCE`

# Acceptance criteria

Mark items as DONE during acceptance test. Acceptance test must be prformed in a different domain from the source domain from above.

- [ ] DT was successfully deployed to  environments
	- [ ] STAGE
	- [ ] DEV
- [ ] Form template bundle successfully deployed
	- [ ] STAGE
	- [ ] DEV
- [ ] Domains were created in __all__ relevant clients as expected (sample at least one client in each env. - this should __not__ be the content-creators' client)
- [ ] __All__ clients' data was migrated to new domain as expected (sample at least one client in each env.)
- [ ] All relevant artifacts (domain template, form template bundle, ...) are committed to GIT repository
- [ ] Ticket for `SANDBOX-/PROD-/TALOS-/...` deployments as specified above was created for Ops-Team (when all previous checks have passed)

---

/label ~"1.Component::3.Content"

/label ~"2.IssueType::1.Story"

/label ~"3.Status::1.Specification"
