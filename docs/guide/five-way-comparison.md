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

There are two rounds on this page. The first, immediately below, was
assembled in September 2026 by transcribing each project's own recorded
table, measured on 2026-09-12 or 2026-09-13. The second,
[further down](#a-second-round-all-five-on-one-day-2026-09-19), re-measured
all five on a single day, 2026-09-19, after jolt released v0.8.9. Both are
kept so the family's numbers read as a progression.

## At 2048 MB

| Metric | bb (Babashka) | cljs (Node.js) | jlt (Jolt) | jnk (jank) | rst (Jolt+Rust) |
|---|---|---|---|---|---|
| Cold Init Duration | 380.7 ms | 104.6 ms | 288.3 ms | 510.3 ms | 295.4 ms |
| Cold Duration | 7.7 ms | 4.1 ms | 2.1 ms | 1.7 ms | 2.1 ms |
| Warm Duration (median) | 1.8 ms | 1.7 ms | 1.8 ms | 1.4 ms | 1.9 ms |
| Max Memory Used | 102 MB | 81 MB | 164 MB | 26 MB | 168 MB |

## At 3008 MB

| Metric | bb (Babashka) | cljs (Node.js) | jlt (Jolt) | jnk (jank) | rst (Jolt+Rust) |
|---|---|---|---|---|---|
| Cold Init Duration | 368.6 ms | 109.0 ms | 401.9 ms | 64.0 ms | 304.8 ms |
| Cold Duration | 7.0 ms | 4.4 ms | 3.0 ms | 1.5 ms | 2.0 ms |
| Warm Duration (median) | 1.7 ms | 1.7 ms | 2.2 ms | 1.3 ms | 1.9 ms |
| Max Memory Used | 104 MB | 83 MB | 164 MB | 26 MB | 168 MB |

**Sources, exactly as recorded in each project's own doc — this page
doesn't re-derive anything:**

- `bb`: `lambda-mvp-bb/docs/guide/cold-warm-boot.md`, "This project's
  own baseline" table, `ap-southeast-2`, arm64, `provided.al2023`,
  2026-09-13.
- `cljs`: `lambda-mvp-cljs/docs/guide/cold-warm-boot.md`, measured
  same AWS account, `ap-southeast-2`, 2026-09-12 (that page's own table
  is itself a 3-way jolt/jank/cljs comparison, cited here for its own
  cljs numbers only).
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

## A second round, all five on one day (2026-09-19)

The two tables above stay as first recorded. This round was triggered by jolt
releasing v0.8.9 on 2026-09-18, and rather than update only the two Jolt-based
siblings, all five were re-measured against the same account and region
(`ap-southeast-2`) on 2026-09-19, so the columns are same-day for once.

### At 2048 MB

| Metric | bb (Babashka) | cljs (Node.js) | jlt (Jolt 0.8.9) | jnk (jank) | rst (Jolt 0.8.9+Rust) |
|---|---|---|---|---|---|
| Cold Init Duration | 397.6 ms | 110.9 ms | 361.6 ms | 615.4 ms | 470.7 ms |
| Cold Duration | 8.1 ms | 4.2 ms | 1.8 ms | 1.7 ms | 2.3 ms |
| Warm Duration (median) | 1.9 ms | 1.7 ms | 1.7 ms | 1.4 ms | 1.9 ms |
| Max Memory Used | 104 MB | 81 MB | 182 MB | 26 MB | 187 MB |

### At 3008 MB

| Metric | bb (Babashka) | cljs (Node.js) | jlt (Jolt 0.8.9) | jnk (jank) | rst (Jolt 0.8.9+Rust) |
|---|---|---|---|---|---|
| Cold Init Duration | 370.8 ms | 106.2 ms | 346.1 ms | 61.3 ms | 365.0 ms |
| Cold Duration | 8.2 ms | 4.1 ms | 1.9 ms | 1.6 ms | 2.3 ms |
| Warm Duration (median) | 1.8 ms | 1.6 ms | 1.7 ms | 1.5 ms | 1.8 ms |
| Max Memory Used | 104 MB | 82 MB | 182 MB | 26 MB | 187 MB |

**Read the sampling difference before comparing columns.** `jlt` and `rst`
are the median of three `bench` runs per tier, because the jolt version bump
was the reason for the round and Cold Init is too noisy to call from one
sample. `bb`, `cljs` and `jnk` are a single run each, the same methodology
their own tables above used. So the two Jolt columns are steadier numbers than
the other three by construction, not by merit.

### What moved since the first round

**The three non-Jolt siblings barely moved**, which is the useful part. bb's
Cold Init came within 5% at both tiers, 4.4% at 2048 MB and 0.6% at 3008 MB,
and its Max Memory within 2 MB.
cljs moved about 6% at 2048 MB and 3% at 3008 MB. jnk reproduced its own
order effect exactly, 615.4 ms at 2048 MB against 61.3 ms at 3008 MB, the same
ECR-layer-caching artifact its own doc already explains. Since nothing in
those three projects changed between the rounds, that spread is a fair
estimate of the run-to-run noise floor on this account.

**Both Jolt siblings gained about 11% in resident memory.** jlt went 164 to
182 MB and rst went 168 to 187 MB, holding steady across every run. Both
binaries grew roughly 12% alongside it. That is jolt v0.8.9's own change, and
the fact that it shows up identically on a binary with Rust FFI and one
without is what makes it attributable to the runtime rather than to either
project.

**Warm time improved on both Jolt siblings, and jlt's handler time improved
clearly.** jlt's Cold Duration dropped from 2.3 to 1.8 ms at 2048 MB with
almost no overlap between the two versions' samples. rst's Cold Duration is a
wash, plausibly because its Rust builder work sits in the same few
milliseconds. Neither project showed a Cold Init improvement that three runs
per tier could separate from noise. Each project's own doc carries the full
per-version tables:
[jlt](https://github.com/b12n-oss/lambda-mvp-jlt/blob/main/docs/guide/cold-warm-boot.md)
and
[rst](https://github.com/b12n-oss/lambda-mvp-rst/blob/main/docs/guide/cold-warm-boot.md).

**Sources for this round:** measured directly by running each project's own
`bench` task against one account in `ap-southeast-2` on 2026-09-19, rather
than transcribed from the five separate docs the way the first round was.
jlt and rst ran at `JOLT_VERSION=0.8.9`, which is now both repos' default.
jnk built from the current jank package (`0.1-noble`), which its Dockerfile
installs unpinned from the PPA, so its build is not version-controlled the way
the Jolt pair's is.

## Caveats, carried forward from each project's own docs rather than re-derived

- **jnk's numbers aren't directly comparable to the four zip-based
  siblings'.** `lambda-mvp-jnk` deploys as a **container image**, not a
  zip — its own `docs/guide/cold-warm-boot.md` says plainly that
  container-image cold start and zip-based cold start aren't the same
  measurement, and that the surprising "3008 MB colder than 2048 MB"
  result in its own table is attributed to ECR image-layer caching
  order, not memory size.
- **rst's upstream dependency has landed.** `lambda-mvp-rst`'s Rust
  JSON-builder capability (`jolt-diplomat`'s builder API) was developed
  on a public fork branch (`burinc/jolt-diplomat`,
  `feat/json-builder-api`) and vendored in via `bb vendor`. That branch
  was **merged into `jolt-lang/jolt-diplomat` on 2026-09-14**, so the
  "pending upstream PR" caveat this bullet used to carry no longer
  applies. `lambda-mvp-rst` still vendors its snapshot rather than
  pinning a git ref, which is now a cleanup its own README tracks.
- **bb is the only sibling with a Lambda Layer in its cold-start
  path, and its own Cold Duration numbers stand out.** Cold Init
  Duration (mounting the layer, starting `bb`, interpreting
  `bootstrap.clj`) lands in the middle of the pack — 368-381 ms,
  between cljs's ~105-109 ms and jnk's 2048 MB figure of 510 ms. But
  **Cold Duration** (the handler's own execution time) is
  consistently the highest of the five at both tiers — 7.0-7.7 ms,
  versus 1.5-4.4 ms for every other sibling. bb is the only one of
  the five whose handler is interpreted (via Babashka's SCI) rather
  than AOT-compiled or JIT-warmed ahead of the first request, which
  is a plausible explanation, though this single run isn't enough to
  separate that from measurement noise or the layer-mount overhead
  bleeding into the handler's own timing. See
  [Building the runtime layer](runtime-layer-build.md) for the
  mechanism.
- **None of these numbers are a live guarantee.** Every source doc says
  the same thing: a single run each, on one account, one region, one
  day. The spread between projects (down to ~1.3 ms warm, cold init
  anywhere from ~65 ms to ~510 ms) is itself the point — not a precise
  number to target. Run each project's own `bench` task against your
  own account for numbers you can trust.

## See also

- [Cold vs. warm boot](cold-warm-boot.md): this project's own
  methodology and its own baseline table.
- [Building the runtime layer](runtime-layer-build.md): the
  architectural difference this page's caveats reference.
