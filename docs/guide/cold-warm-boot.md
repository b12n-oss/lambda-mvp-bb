# Cold vs. warm boot on a Babashka custom runtime (via blambda)

## Three numbers, one log line

Every Lambda invocation ends with a `REPORT` line in CloudWatch:

```
REPORT RequestId: ... Duration: 4.60 ms Billed Duration: 5 ms Memory Size: 512 MB Max Memory Used: 237 MB [Init Duration: 322.20 ms]
```

- **Init Duration** (cold invokes only): time to start the execution environment — mount the runtime layer, exec `bootstrap`, start the `bb` binary, interpret `bootstrap.clj`, `require` the handler namespace — before the handler's first request is served.
- **Duration**: the handler's own execution time (the Runtime API loop's `next-invocation` → `handler` → `send-response!` round trip). Present on every invocation, cold or warm.
- **Billed Duration**: what you're actually charged for. For a custom runtime this *includes* Init Duration on a cold invoke.

`bb invoke` prints this line directly. `script/bench.clj`'s `parse-report-line` turns it into a Clojure map; `Init Duration`'s absence is exactly how a warm sample is told apart from a cold one.

## What `bb bench` does

For each memory tier in `BENCH_MEMORY_TIERS` (default `2048,3008`):

1. `aws lambda update-function-configuration --memory-size <tier>`: any configuration update invalidates the function's existing execution environments, so the very next invoke runs in a fresh one. No separate "force cold start" trick needed.
2. Waits for the update to finish applying, then invokes once, the cold sample.
3. Invokes `BENCH_WARM_SAMPLES` more times back-to-back (no further config change), the warm samples, landing on the same execution environment.
4. Prints a table: Cold Init Duration, Cold Duration, Warm Duration (min/median/max), Max Memory Used, one column per tier.

Run it:

```sh
bb deploy
bb bench
bb teardown   # when you're done -- nothing should keep running in your account
```

Two things worth knowing before you run it. `bb bench` leaves the function configured at its last tier's memory size (3008 MB by default): a later `bb deploy` resets it back to 2048 MB, but if you only ever run `bb bench` you're left on the higher-cost tier until you set it back yourself. And `bb deploy`/`bb bench`/`bb teardown` all act on whatever function/role name is configured (`LAMBDA_MVP_FUNCTION_NAME`, default `lambda-mvp-bb`) — the runtime layer name stays fixed regardless — so don't point it at an existing unrelated resource.

If a "cold" sample shows no Init Duration, `bb bench` prints a warning rather than silently reporting incomplete data.

## This project's own baseline

Measured against a real deployment (`ap-southeast-2`, arm64, `provided.al2023`, 2026-09-13), via `bb bench` with the default tiers and sample count:

| Metric | 2048 MB | 3008 MB |
|---|---|---|
| Cold Init Duration | 380.7 ms | 368.6 ms |
| Cold Duration | 7.7 ms | 7.0 ms |
| Warm Duration (min/median/max) | 1.7 / 1.8 / 1.8 ms | 1.7 / 1.7 / 1.9 ms |
| Max Memory Used | 102 MB | 104 MB |

As with every sibling's own recorded table: **illustrative, not a live guarantee** — a single run, on one account, one region, one day. Your numbers will differ by account, region, and the hardware allocation AWS happens to give you. Run `bb bench` for your own.

## See also

- [Five-way comparison](five-way-comparison.md): this project's numbers alongside all four siblings', once measured.
- [Building the runtime layer](runtime-layer-build.md): the two-artifact build this project's cold start goes through.
