# Building the runtime layer

## Why this is the only sibling with a Lambda Layer

Every other `lambda-mvp-*` sibling ships one self-contained deployment
artifact: a zip with the compiled binary (or, for `lambda-mvp-cljs`, the
compiled Node.js bundle) at its root. This project can't do that,
because of one line in blambda's shipped `resources/bootstrap` — the
shell script AWS Lambda actually execs as the container entrypoint:

```sh
LAYERS_DIR=/opt
...
PATH="$PATH:$LAYERS_DIR" $LAYERS_DIR/bb -cp $CLASSPATH $LAYERS_DIR/bootstrap.clj
```

`/opt` is where Lambda mounts **layers** — not `/var/task`
(`$LAMBDA_TASK_ROOT`), where the function's own deployment package
lands. So the Babashka binary and the Runtime API loop have to be
delivered as a Lambda Layer; there's no way to fold them into a single
function zip without patching that script, which this project
deliberately doesn't do (see the design spec's Non-goals).

## The two artifacts

`bb build` runs two of blambda's own CLI subcommands in sequence:

1. **`bb blambda build-runtime-layer`** — downloads the Babashka static
   binary for the target architecture (`$LAMBDA_ARCH`, default `arm64`)
   straight from Babashka's GitHub releases, bundles it with
   `bootstrap` and `bootstrap.clj` (copied from blambda's own
   `resources/`), and zips the three into
   `target/lambda-mvp-bb-runtime.zip`.
2. **`bb blambda build-lambda`** — zips this project's own
   `src/net/b12n/lambda_mvp/handler.clj`, preserving its nested
   directory path, into `target/lambda-mvp-bb.zip`.

No deps layer (`build-deps-layer`) — the demo handler has zero external
dependencies, matching blambda's own `hello-world` example.

`bb deploy` publishes the runtime-layer zip as a Lambda Layer version
and attaches it to the function; the function zip is uploaded as the
function's own code, same as any zip-based Lambda deploy.

## Real measured sizes

Measured via a live `bb build` run during this project's own
spec-writing (Babashka 1.3.186, arm64): the runtime-layer zip is
24,522,906 bytes (~23.4 MiB — almost entirely the Babashka static
binary itself), and the function zip is 438 bytes. For scale, blambda's
own `site-analyser` example (which bundles the AWS SDK in a separate
deps layer) states its own layers at "22 MB and 5 MB" — this project's
much smaller function zip reflects having no dependencies at all.

## What this means for the cold/warm comparison

This project is the only one of the five whose cold-start path includes
mounting a Lambda Layer before the handler namespace is even resolved.
Whether that shows up as measurably different `Init Duration` compared
to the self-contained-zip siblings is exactly the kind of question
[Five-way comparison](five-way-comparison.md) exists to surface, once
this project has a real bench run.

## See also

- [Architecture](architecture.md): the whole-system view.
- [Cold vs. warm boot](cold-warm-boot.md): what `bb bench` measures.
