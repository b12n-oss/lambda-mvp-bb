# Contributing

## Build and test

```sh
bb probe   # offline e2e: mock Runtime API + blambda's real bootstrap.clj loop
bb test    # script/bench.clj's unit tests
```

Both run with no AWS account and no Docker. Run them before every
commit that touches `src/`, `script/`, or `tools/`.

## Conventions

- No hardcoded AWS profile, account, or region anywhere — every task
  reads whatever the caller's own `aws` CLI already resolves.
- No CDK/SAM/Terraform/CloudFormation. Deploys are plain `aws` CLI
  calls wrapped in babashka scripts.
- Handler response shape (`message`/`runtime`/`request_id`/
  `warm_invocation`/`event`) matches the other four `lambda-mvp-*`
  siblings exactly — keep it that way if you touch `handler.clj`.
- Stage files by explicit path in every commit; never `git add -A`/`.`/`-u`.

## Docs site

```sh
bb site:build    # into _site/, needs a jlt-commons/docs-engine checkout
bb site:serve    # same, plus a local server
```

CI builds the site on every push and PR (`.github/workflows/site.yml`);
these tasks are for local preview only.

## Pull requests

- Keep the demo handler minimal — anything that grows it risks
  confounding the cold-start story this project exists to measure.
- If a change affects `docs/guide/cold-warm-boot.md`'s methodology or
  `docs/guide/five-way-comparison.md`'s numbers, update both in the
  same PR.
