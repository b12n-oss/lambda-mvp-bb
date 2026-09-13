# Five-way comparison: cold vs. warm boot across the lambda-mvp-* family

Five ways to run a Lambda function that greets you, echoes your event,
and counts warm invocations:

| Project | Language / runtime | Deploy shape |
|---|---|---|
| [`lambda-mvp-bb`](https://github.com/b12n-oss/lambda-mvp-bb) (this project) | Babashka, via blambda | Zip + **Lambda Layer** (the only one) |
| [`lambda-mvp-cljs`](https://github.com/b12n-oss/lambda-mvp-cljs) | ClojureScript on Node.js | Managed `nodejs24.x` runtime, single zip |
| [`lambda-mvp-jlt`](https://github.com/b12n-oss/lambda-mvp-jlt) | Jolt (native Clojure/Chez Scheme) | Custom runtime, single self-contained zip |
| [`lambda-mvp-jnk`](https://github.com/b12n-oss/lambda-mvp-jnk) | jank (native Clojure/LLVM) | Custom runtime, **container image** (not a zip) |
| [`lambda-mvp-rst`](https://github.com/b12n-oss/lambda-mvp-rst) | Jolt + a Rust JSON builder via jolt-diplomat/FFI | Custom runtime, single self-contained zip |

All five expose the same response shape
(`message`/`runtime`/`request_id`/`warm_invocation`/`event`) and the
same `bb bench`/`jolt bench` methodology: force a fresh execution
environment via `update-function-configuration`, measure one cold
sample and several warm ones, at `2048`/`3008` MB by default.

## At 2048 MB

| Metric | bb (Babashka) | cljs (Node.js) | jlt (Jolt) | jnk (jank) | rst (Jolt+Rust) |
|---|---|---|---|---|---|
| Cold Init Duration | not yet measured | 104.6 ms | 288.3 ms | 510.3 ms | 295.4 ms |
| Cold Duration | not yet measured | 4.1 ms | 2.1 ms | 1.7 ms | 2.1 ms |
| Warm Duration (median) | not yet measured | 1.7 ms | 1.8 ms | 1.4 ms | 1.9 ms |
| Max Memory Used | not yet measured | 81 MB | 164 MB | 26 MB | 168 MB |

## At 3008 MB

| Metric | bb (Babashka) | cljs (Node.js) | jlt (Jolt) | jnk (jank) | rst (Jolt+Rust) |
|---|---|---|---|---|---|
| Cold Init Duration | not yet measured | 109.0 ms | 401.9 ms | 64.0 ms | 304.8 ms |
| Cold Duration | not yet measured | 4.4 ms | 3.0 ms | 1.5 ms | 2.0 ms |
| Warm Duration (median) | not yet measured | 1.7 ms | 2.2 ms | 1.3 ms | 1.9 ms |
| Max Memory Used | not yet measured | 83 MB | 164 MB | 26 MB | 168 MB |

**Sources, exactly as recorded in each project's own doc — this page
doesn't re-derive anything:**

- `bb`: not yet measured. This sandbox has no AWS credentials
  configured. Fill in from `lambda-mvp-bb`'s own
  `docs/guide/cold-warm-boot.md` once a real `bb bench` run exists.
- `cljs`: `lambda-mvp-cljs/docs/guide/cold-warm-boot.md`, measured
  same AWS account, `ap-southeast-2`, 2026-09-12 (that page's own table
  is itself a 3-way jolt/jank/cljs comparison; the jolt/jank columns
  quoted here are cljs's own citations of jlt's and jnk's numbers).
- `jlt`: `lambda-mvp-jlt/docs/guide/cold-warm-boot.md`, "This repo's
  own baseline" table, `ap-southeast-2` column specifically (jlt's own
  page also has a `us-west-2` column and a separate jolt-version
  comparison; both omitted here for one-region-per-project
  consistency), jolt v0.8.7, arm64, 2026-09-12.
- `jnk`: `lambda-mvp-jnk/docs/guide/cold-warm-boot.md`, "A real
  measured run", `ap-southeast-2`, x86_64, 2026-09-12.
- `rst`: `lambda-mvp-rst/docs/guide/cold-warm-boot.md`, "This repo's
  own baseline" (the corrected, re-run table — an earlier table in the
  same doc was retracted as mislabeled and is not cited here), jolt
  v0.8.7, arm64, `ap-southeast-2`, 2026-09-12.

## Caveats, carried forward from each project's own docs rather than re-derived

- **jnk's numbers aren't directly comparable to the four zip-based
  siblings'.** `lambda-mvp-jnk` deploys as a **container image**, not a
  zip — its own `docs/guide/cold-warm-boot.md` says plainly that
  container-image cold start and zip-based cold start aren't the same
  measurement, and that the surprising "3008 MB colder than 2048 MB"
  result in its own table is attributed to ECR image-layer caching
  order, not memory size.
- **rst currently depends on a not-yet-merged upstream PR.** Per your
  own framing: `lambda-mvp-rst`'s Rust JSON-builder capability
  (`jolt-diplomat`'s builder API) lives on a public fork branch
  (`burinc/jolt-diplomat`, `feat/json-builder-api`), vendored in via
  `bb vendor`, pending a PR against `jolt-lang/jolt-diplomat` upstream.
  This is noted here as **pending, expected to be open-sourced
  shortly** — not treated as a defect in the comparison, since the
  measured binary is real and the numbers reflect what actually
  deploys today.
- **bb is the only sibling with a Lambda Layer in its cold-start
  path.** See [Building the runtime layer](runtime-layer-build.md) for
  why, and whether that shows up as a measurable difference once this
  project has real numbers.
- **None of these numbers are a live guarantee.** Every source doc says
  the same thing: a single run each, on one account, one region, one
  day. The spread between projects (down to ~1.3 ms warm, cold init
  anywhere from ~65 ms to ~510 ms) is itself the point — not a precise
  number to target. Run each project's own `bench` task against your
  own account for numbers you can trust.

## See also

- [Cold vs. warm boot](cold-warm-boot.md): this project's own
  methodology and (once measured) its own baseline table.
- [Building the runtime layer](runtime-layer-build.md): the
  architectural difference this page's caveats reference.
