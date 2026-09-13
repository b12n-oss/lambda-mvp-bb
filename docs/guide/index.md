# lambda-mvp-bb, User guide

`lambda-mvp-bb` runs [Babashka](https://babashka.org) on AWS Lambda as a **custom runtime**, via [blambda](https://github.com/jmglov/blambda) (which wraps [bb-lambda](https://github.com/tatut/bb-lambda)'s Runtime API loop). Unlike this family's other custom-runtime siblings, nothing here is compiled ahead of time: the handler namespace is `require`d and interpreted at cold start by Babashka's own SCI interpreter. On top of it sits a demo handler and a `bb bench` tool built specifically to answer one question: how much does a Lambda invocation actually cost in wall-clock time, cold versus warm, at different memory tiers, on your own AWS account.

Every AWS-touching command in this repo relies entirely on the caller's own `aws` CLI configuration (`AWS_PROFILE`/`AWS_REGION`, or `aws configure`). Nothing here hardcodes a profile, an account, or a region: clone it, point it at your account, and the same commands that produced this project's own measured numbers will produce yours.

## How to read this guide

**New to the project?** Start with [Getting started](getting-started.md) for the offline probe and your first live deploy. Then read [Architecture](architecture.md) for how the pieces fit together: blambda's Runtime API loop, the runtime-layer build, and the bench tool.

**Wondering why this is the only sibling with a Lambda Layer?** [Building the runtime layer](runtime-layer-build.md) covers the `/opt` constraint blambda's shipped `bootstrap` script hardcodes, why that rules out a single self-contained zip, and the two-artifact build it takes instead.

**Want to understand or reproduce the cold/warm boot-time story?** [Cold vs. warm boot](cold-warm-boot.md) is the reason this project exists: what `bb bench` measures, how to read the table it prints, and this project's own measured numbers.

**Want the full 5-way comparison across every sibling?** [Five-way comparison](five-way-comparison.md) pulls every sibling's own recorded numbers into one place, with the caveats each one's own docs already carry.

**Want to contribute a change?** [Contributing](contributing.md) covers the build, the test commands, and this project's conventions.

## Guide map

| Page | What you'll learn |
|---|---|
| [Getting started](getting-started.md) | Install, offline probe, first live deploy |
| [Architecture](architecture.md) | How the runtime layer, the function zip, and the bench tool fit together |
| [Building the runtime layer](runtime-layer-build.md) | Why this project needs two artifacts and a Lambda Layer, unlike its siblings |
| [Cold vs. warm boot](cold-warm-boot.md) | What `bb bench` measures and how to reproduce the comparison |
| [Five-way comparison](five-way-comparison.md) | This project's numbers alongside all four siblings' |
| [Contributing](contributing.md) | Build, test, and PR conventions |

## Find your scenario

| Scenario | Pages to read |
|---|---|
| "I want to try this out" | Getting started |
| "I want to understand how it works" | Architecture, then Building the runtime layer |
| "I want to measure cold/warm boot time on my own account" | Getting started, then Cold vs. warm boot |
| "I want to compare all five lambda-mvp-* runtimes" | Five-way comparison |
| "I want to contribute a change" | Contributing |

## See also

- [Project README](https://github.com/b12n-oss/lambda-mvp-bb/blob/main/README.md): the same quickstart in prose.
- [`CHANGELOG.md`](https://github.com/b12n-oss/lambda-mvp-bb/blob/main/CHANGELOG.md): version history.
