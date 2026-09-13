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
