# Changelog

All notable changes to this project are documented here.

## [Unreleased]

- Initial public release preparation: handler, blambda-based build
  (runtime layer + function zip), offline probe against a mock Runtime
  API, layer-aware deploy/invoke/teardown, `bb bench` cold/warm
  comparison tooling, and docs (including a five-way comparison against
  the other four `lambda-mvp-*` siblings).
- First real `bb bench` run against a live AWS account
  (`ap-southeast-2`, arm64, 2026-09-13): `docs/guide/cold-warm-boot.md`
  and `docs/guide/five-way-comparison.md` now carry this project's own
  measured numbers instead of placeholders.
- `docs/guide/five-way-comparison.md` covers seven siblings now: it adds
  lambda-mvp-cljrfc (Clojurific) and lambda-mvp-glj (Glojure), and a third
  round measured on 2026-11-14 with cljs and jlt (Jolt 0.8.27).
- The comparison page has all seven siblings benched on one day,
  2026-11-14, including the first invoke after each deploy.
- The Site workflow pins docs-engine v0.4.6, since v0.3.2 no longer builds
  under current jolt.
