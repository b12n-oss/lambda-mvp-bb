# Contributing

## Build and test

```sh
bb probe   # offline e2e: mock Runtime API + blambda's real bootstrap.clj loop, ~2s
bb test    # script/bench.clj's unit tests
```

Both run with no AWS account and no Docker — this is the fast, offline
loop. Run it after every change to `src/handler.clj`, `script/bench.clj`,
`tools/mock_runtime_api.py`, or `bb.edn`.

Live-testing against a real AWS account (`bb deploy`/`bb invoke`/
`bb bench`/`bb teardown`) isn't part of the fast loop, but it's the
check that actually matters for anything touching
`script/aws_lifecycle.clj` or `script/bench_run.clj` — the offline
loop can't exercise the Lambda Layer publish/attach/cleanup path or a
real memory-tier resize. If you're changing either of those files,
deploy to a throwaway function in your own account, exercise the
change for real, and tear it down again (`bb teardown`) before
opening a PR.

## Conventions

- **No hardcoded AWS profile, account, or region, anywhere.** Every
  AWS-touching command relies on the caller's own `aws` CLI
  configuration. Grep for `--profile` and a 12-digit account ID
  before committing anything that touches `script/aws_lifecycle.clj`
  or `script/bench_run.clj`.
- **`bb.edn` task bodies stay EDN-reader-safe.** No `#"regex"`,
  `@deref`, or `#(...)` anonymous-fn literals in a task's `:task`
  form — babashka's EDN reader doesn't support them. Run `bb tasks`
  after any `bb.edn` edit to confirm it still parses.
- **Every AWS-mutating call checks its own exit code.** A `sh` call
  that can fail (`create-function`, `publish-layer-version`,
  `delete-layer-version`) dies with the AWS CLI's own error text
  rather than silently continuing past a failure.
- **Namespace root is `net.b12n.lambda-mvp`.** Directory
  `src/net/b12n/lambda_mvp/`.
- **Handler response shape** (`message`/`runtime`/`request_id`/
  `warm_invocation`/`event`) matches the other four `lambda-mvp-*`
  siblings exactly — keep it that way if you touch `handler.clj`.
- **Keep the demo handler minimal.** Anything that grows it risks
  confounding the cold-start story this project exists to measure.

## Docs site

```sh
bb site:build   # into _site/ (needs a docs-engine checkout, see below)
bb site:serve   # build and serve locally
```

Both need a local checkout of
[`jlt-commons/docs-engine`](https://github.com/jlt-commons/docs-engine),
found via `$DOCS_ENGINE`, a sibling `../docs-engine` directory, or
`~/dev/jlt-commons/docs-engine`. CI checks the engine out itself, so
this is for local preview only. New guide pages go in
`docs/guide/*.md`, no frontmatter — title and description come from
`docs/site.edn`.

## Pull requests

Open against `main`. Describe what changed and why, not just what.

If the change touches `script/aws_lifecycle.clj` or
`script/bench_run.clj`, say what you live-tested, against what memory
tier, and on which architecture (`arm64`/`x86_64`) if the change
could plausibly affect the `LAMBDA_ARCH` path.

If a change affects `docs/guide/cold-warm-boot.md`'s methodology or
`docs/guide/five-way-comparison.md`'s numbers, update both in the
same PR.
