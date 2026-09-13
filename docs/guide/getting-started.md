# Getting started

## What you'll have at the end of this page

- The offline probe passing locally, with no AWS account.
- A real `target/lambda-mvp-bb-runtime.zip` and `target/lambda-mvp-bb.zip`.
- A live Lambda function deployed to your own AWS account, invoked once, and torn down again.

## Prerequisites

| Tool | Why | Install |
|---|---|---|
| **[babashka](https://babashka.org)** | Every task in this project (`bb <task>`), and the actual Lambda runtime language too | `brew install borkdude/brew/babashka` |
| **AWS CLI v2** | Every AWS-touching task shells out to it | `brew install awscli` |
| **An AWS account** | To deploy, invoke, and benchmark against | Credentials set up via `AWS_PROFILE`/`AWS_REGION` env vars or `aws configure`. Nothing in this repo hardcodes a profile, account, or region. |

No Docker, unlike the Jolt/jank-based siblings — blambda's build downloads a prebuilt static binary directly, no container build step. (LocalStack was tried as a no-AWS-account alternative for the deploy/invoke/teardown lifecycle and doesn't currently work for this project's layered artifact — see the design spec's "Local dev without a real AWS account" section if you're tempted to try it yourself before reading why.)

## Clone and probe

```sh
git clone git@github.com:b12n-oss/lambda-mvp-bb.git
cd lambda-mvp-bb
bb probe
```

`bb probe` runs blambda's actual `bootstrap.clj` Runtime API loop (not a reimplementation of it) against an offline mock of the Lambda Runtime API on an OS-assigned local port. No AWS account. If this doesn't pass, nothing later on this page will either.

Run the unit suite the same way:

```sh
bb test
```

## Build the real artifacts

```sh
bb build
```

This downloads the Babashka static binary for your target architecture (cached after the first run) and zips it with blambda's `bootstrap`/`bootstrap.clj` into a runtime-layer zip, then zips this project's own handler into a separate function zip. See [Building the runtime layer](runtime-layer-build.md) for why there are two artifacts instead of one.

## Deploy, invoke, tear down

```sh
bb deploy
bb invoke
bb teardown
```

`bb deploy` is idempotent: it publishes a runtime-layer version and creates the IAM role and Lambda function the first time, updating them (and publishing a fresh layer version) on every later call. `bb invoke` runs a single ad-hoc invocation and prints the response body plus the CloudWatch `REPORT` line, the same line [Cold vs. warm boot](cold-warm-boot.md) explains how to read. `bb teardown` deletes the function, every published layer version, and the role, so nothing keeps running in your account.

### Or all at once

```sh
bb demo
```

`bb demo` runs `build`, `deploy` and `invoke` in order. Before any of them it checks that `aws` is on PATH and that the AWS CLI has credentials and a region (and prints the account it will deploy to). A missing piece stops it with the fix, before anything changes in your account.

## Measure cold vs. warm boot time

```sh
bb deploy
bb bench
bb teardown
```

`bb bench` is the reason this project exists: it deploys at a few different memory tiers, measures one cold invocation and several warm ones at each, and prints a comparison table. See [Cold vs. warm boot](cold-warm-boot.md) for what the numbers mean, and [Five-way comparison](five-way-comparison.md) for how they sit alongside the other four siblings'.

## Next steps

Run `bb tasks` to see every available command, including the documentation site tasks (`bb site:build`/`bb site:serve`). [Architecture](architecture.md) covers how the runtime layer, the function zip, and the bench tool fit together.
