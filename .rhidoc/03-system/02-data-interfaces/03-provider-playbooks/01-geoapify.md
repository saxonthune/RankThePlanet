---
title: Geoapify
summary: Operational playbook for the Geoapify BYOK provider — free-tier shape, the per-user-key posture that keeps RTP out of GDPR DPA territory, attribution, typeahead and caching callouts
tags: [system, providers, location, geoapify, byok, playbook]
deps: [doc03.02.02, doc01.06.04]
---

# Geoapify

Geoapify is a candidate BYOK provider ([[02-location-providers]], doc03.02.02) and the lowest-friction "better OSM" upgrade — same open-data lineage as the keyless `osm` default (OpenStreetMap plus OpenAddresses), broadened, with permissive storage terms and a real free tier. The research that placed it first among the candidates is doc01.06.04. Nothing here is built; this playbook describes how a `GeoapifyLocationProvider` behaves and how a user configures it, so the work is decided once when it is scheduled.

Reach for Geoapify when address and coverage quality is the gap. Reach for Foursquare instead when the gap is specifically restaurants, cafés, and bars — venue density is a different axis Geoapify does not lead on.

## Free tier and the key flow

Geoapify is BYOK in the clean sense the seam assumes: the user owns the account and the key.

- The user signs up at geoapify.com — **no credit card** — creates a project, and copies the auto-generated API key.
- They paste it on the `ProviderConfig` surface in a `geoapify` mode ([[/02-design/02-interaction/02-screens/13-provider-config]], doc02.02.02.13), the same key-entry flow the `google` provider uses. The surface calls `addProvider(Geoapify, key)` on the registry.
- The free plan allows **3,000 credits/day** (1 credit per geocoding or autocomplete request), commercial use included, rate-limited to **5 requests/second**. A hobbyist never leaves it.

The key travels in a request header or the `apiKey` query parameter; prefer the header path so the key stays out of any URL that might be logged, matching the `google` provider's `X-Goog-Api-Key` discipline.

## Privacy posture — the per-user key keeps RTP out of a DPA

This is the load-bearing callout. A GDPR data-processing agreement (Art. 28) binds a *controller* to a *processor* — it is required only when someone processes personal data **on RTP's behalf**. RTP avoids needing one by never becoming that controller:

- **BYOK, never a shared key.** Each user supplies their own Geoapify key, so the user is Geoapify's customer directly. Their typed queries and IP reach Geoapify under *their* account, governed by Geoapify's own privacy policy — RTP is not interposed in the data flow and has no processor to delegate to.
- **No RTP backend.** The device talks to Geoapify directly. RTP runs no server that receives, proxies, or stores queries; persistence is local (SQLCipher on the user's device, doc03.01). There is no RTP-side processing of personal data to cover with an agreement.
- **No telemetry.** RTP logs nothing and is open source — there is no analytics pipeline holding user PII, and the absence is auditable in the source.

What RTP owes instead is lighter than a DPA: **notice, not contract.** At the moment a user enables Geoapify, the `ProviderConfig` surface should state plainly that their search text and IP will be sent to Geoapify under Geoapify's privacy policy (link it), and that Geoapify retains successful-request data for up to 24 hours. Informed choice by the data subject is the clean cut; it is the user, not RTP, who becomes Geoapify's data subject.

> **The trap that reintroduces a DPA: a default shared key.** The moment RTP ships its own Geoapify account proxying every user's searches, RTP becomes the controller routing personal data to Geoapify-as-processor, and the Art. 28 conversation returns — alongside one account's logs pooling every user's search history for 24h. The BYOK design is precisely what keeps RTP clear. Do not add a bundled fallback key for Geoapify (or any logging geocoder) without re-opening this analysis. The keyless `osm` default already fills the zero-config slot, so there is no product reason to.

This is the architectural posture, not legal advice — confirm with counsel before relying on it.

## Attribution

The free plan makes attribution mandatory on two counts, and both are display obligations on the search surface, not the basemap:

- **OpenStreetMap attribution** — "© OpenStreetMap contributors" — because the underlying data is OSM-derived.
- **A Geoapify credit or link** — required specifically by the free plan's terms.

RTP already renders OSM attribution for the tile layer (doc03.02.02 §licensing); Geoapify adds a required Geoapify credit wherever its results are shown. Fold both into the same attribution manifest `Location.sourceType` already drives on export — a `geoapify`-sourced entry carries the OSM obligation like an `osm` one, plus the Geoapify credit.

## Typeahead

`GeoapifyLocationProvider.supportsTypeahead` is `true` — Geoapify's Address Autocomplete API is built for per-keystroke querying, so it lights up the typeahead field. Two constraints shape the debounce:

- The free plan's **5 req/s** ceiling is generous for one user typing, but the provider still coalesces identical-query bursts client-side, as `google` does.
- A `typeaheadDebounceMillis` near the seam's 300ms default fits; there is no need for the 500ms the `osm` provider takes to stay polite to shared Photon, because the user is spending their own quota against a commercial endpoint.

Forward geocoding (`/geocode/search`) backs `resolve`; reverse (`/geocode/reverse`) backs `resolveNearby`. The viewport `bias` maps onto Geoapify's `bias=proximity:lon,lat` (a `Point`) or `bias=rect:lon1,lat1,lon2,lat2` (a `Box`), best-effort like every provider.

## Caching

Geoapify sits in the **permissive** column of the seam's caching table (doc03.02.02 §caching) — its terms allow retaining results, so an adopted Geoapify Location keeps its `cachedMetadata` snapshot offline indefinitely and `refreshable` is optional, the same posture as `osm` and the opposite of `google`'s thin-pointer obligation.

One caveat worth a clause-level read before leaning on it hard: Geoapify's terms permit retention by **silence** — they do not forbid caching or derived databases rather than affirmatively granting it. For RTP's personal-scale `cachedMetadata` this is fine, but a self-publishing-at-scale path (doc03.02.02 §licensing) should verify the clause, or request a signed DPA from Geoapify, before treating retention as guaranteed.
