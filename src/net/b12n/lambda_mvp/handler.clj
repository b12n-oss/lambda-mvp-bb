(ns net.b12n.lambda-mvp.handler
  "The demo handler blambda's bootstrap.clj resolves via _HANDLER=
   net.b12n.lambda-mvp.handler/handler. Unlike jlt/jnk/rst (which build
   a JSON string by hand) or cljs (clj->js), this returns a plain
   Clojure map -- blambda's bootstrap.clj JSON-encodes the return value
   itself. `context` is NOT a curated map: it's the raw Lambda Runtime
   API response headers, lowercase string keys (verified by reading
   blambda's resources/bootstrap.clj directly), hence the string key
   below rather than a keyword.")

(def invocation-count (atom 0))

(defn handler
  [event context]
  (let [n (swap! invocation-count inc)]
    {:message "hello from Babashka (blambda) on lambda"
     :runtime "Babashka (Clojure interpreter, custom runtime via blambda)"
     :request_id (or (get context "lambda-runtime-aws-request-id") "")
     :warm_invocation n
     :event event}))
