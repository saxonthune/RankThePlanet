---
title: Provider Playbooks
summary: Per-provider operational guides for the LocationProvider seam — free-tier and key flow, attribution duties, privacy posture, typeahead and caching callouts; the prose companion to the seam contract
tags: [system, providers, location, playbook, index]
deps: [doc03.02.02]
---

# Provider Playbooks

The seam contract ([[02-location-providers]], doc03.02.02) defines *what a provider is* — the `LocationProvider` interface, the registry, the caching and licensing rules every provider obeys. These playbooks cover *how to live with a specific provider*: the free-tier shape, the key flow a user walks through, the attribution a provider demands, the privacy posture its logging implies, and the per-provider quirks (rate ceilings, typeahead windows, field masks) that the contract abstracts over.

One doc per provider. A playbook is operational, not contractual — when the contract and a playbook disagree, the contract wins and the playbook is stale. Keep them sparse: a provider earns a playbook the moment a real callout exists for it, not before.

The default `osm` provider needs no playbook yet — its operational rules (the ~1 req/s politeness throttle, the `User-Agent` requirement, the self-host endpoint override) live inline in the seam contract because they *are* the default behavior. A playbook appears when a provider carries decisions a reader would otherwise get wrong: a key to acquire, terms to honor, a privacy choice to make.

## Providers with a playbook

- [[01-geoapify]] — the lowest-friction BYOK "better OSM" upgrade. Its playbook turns on the per-user-key posture that keeps RTP clear of a GDPR data-processing agreement.

Candidates surveyed but not yet given a playbook (or an implementation) live in the research log ([[/01-product/06-research-sessions/04-geocoding-provider-options]], doc01.06.04): Stadia, Mapbox, Foursquare, LocationIQ. Each earns a playbook when it is scheduled for the seam.
