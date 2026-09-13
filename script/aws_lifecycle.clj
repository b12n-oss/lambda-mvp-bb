(ns script.aws-lifecycle
  "Generic (no hardcoded profile/account/region) create-or-update / invoke /
  teardown for the lambda-mvp-bb demo function, driven entirely by
  whatever the caller's aws CLI already has configured (AWS_PROFILE/
  AWS_REGION env vars, or `aws configure`). Invoked via `bb deploy`/
  `bb invoke`/`bb teardown`, or directly:
  `bb script/aws_lifecycle.clj deploy|invoke|teardown`.

  Two artifacts, one a layer -- blambda's shipped bootstrap shell script
  hardcodes LAYERS_DIR=/opt, so the Babashka binary + Runtime-API loop
  must ship as a Lambda Layer (see the design spec's Architecture
  section). publish-layer-version always creates a new version number,
  so teardown! deletes every version this project published, not just
  the function and role."
  (:require [babashka.process :as p]
            [cheshire.core :as json]
            [clojure.string :as str]))

(def function-name (or (System/getenv "LAMBDA_MVP_FUNCTION_NAME") "lambda-mvp-bb"))
(def lambda-arch (or (System/getenv "LAMBDA_ARCH") "arm64"))
(def lambda-handler "net.b12n.lambda-mvp.handler/handler")
(def runtime-layer-name "lambda-mvp-bb-runtime")
(def runtime-zip-path "target/lambda-mvp-bb-runtime.zip")
(def lambda-zip-path "target/lambda-mvp-bb.zip")
(def role-name (str function-name "-role"))
(def policy-arn "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole")

(defn- sh [& args]
  (let [{:keys [exit out err]} (apply p/shell {:out :string :err :string :continue true} args)]
    {:exit exit :out out :err err}))

(defn- die! [& msg]
  (binding [*out* *err*]
    (apply println "lambda-mvp-bb:" msg))
  (System/exit 1))

(defn- require-aws-identity!
  "Fail fast with a clear message if the aws CLI has no usable
  credentials/region, rather than letting a later call fail obscurely."
  []
  (let [{:keys [exit err]} (sh "aws" "sts" "get-caller-identity" "--output" "json")]
    (when-not (zero? exit)
      (die! "aws CLI has no usable credentials/region."
            "Set AWS_PROFILE/AWS_REGION or run `aws configure`, then retry.\n"
            (str/trim (or err ""))))))

(def ^:private trust-policy
  (json/generate-string
   {:Version "2012-10-17"
    :Statement [{:Effect "Allow"
                 :Principal {:Service "lambda.amazonaws.com"}
                 :Action "sts:AssumeRole"}]}))

(defn- role-exists? []
  (zero? (:exit (sh "aws" "iam" "get-role" "--role-name" role-name))))

(defn- ensure-role! []
  (if (role-exists?)
    (println "lambda-mvp-bb: role" role-name "already exists")
    (do
      (println "lambda-mvp-bb: creating role" role-name)
      (let [{:keys [exit err]} (sh "aws" "iam" "create-role"
                                   "--role-name" role-name
                                   "--assume-role-policy-document" trust-policy)]
        (when-not (zero? exit) (die! "create-role failed:" err)))
      ;; IAM role propagation is eventually consistent -- a create-function
      ;; immediately after create-role can fail with "role cannot be assumed".
      (println "lambda-mvp-bb: waiting 10s for IAM role propagation")
      (Thread/sleep 10000)))
  ;; Always (re-)attach the policy, whether the role is new or pre-existing --
  ;; attach-role-policy is itself idempotent (AWS no-ops on an
  ;; already-attached policy), so this closes the gap where a prior run
  ;; created the role but died before attaching the policy.
  (let [{:keys [exit err]} (sh "aws" "iam" "attach-role-policy"
                               "--role-name" role-name
                               "--policy-arn" policy-arn)]
    (when-not (zero? exit) (die! "attach-role-policy failed:" err))))

(defn- role-arn []
  (-> (sh "aws" "iam" "get-role" "--role-name" role-name
          "--query" "Role.Arn" "--output" "text")
      :out str/trim))

(defn- publish-layer!
  "Always creates a NEW layer version -- publish-layer-version has no
  'unchanged, skip' concept. Returns the new version's ARN."
  []
  (println "lambda-mvp-bb: publishing runtime layer from" runtime-zip-path)
  (let [{:keys [exit out err]} (sh "aws" "lambda" "publish-layer-version"
                                   "--layer-name" runtime-layer-name
                                   "--zip-file" (str "fileb://" runtime-zip-path)
                                   "--compatible-runtimes" "provided.al2023"
                                   "--compatible-architectures" lambda-arch
                                   "--query" "LayerVersionArn" "--output" "text")]
    (when-not (zero? exit) (die! "publish-layer-version failed:" err))
    (str/trim out)))

(defn- layer-version-numbers
  "Empty when the layer has never been published (list-layer-versions
  returns ResourceNotFoundException in that case, not an empty 200 --
  verified against AWS's own API docs). Any OTHER failure is real and
  must not be silently treated the same way, or teardown! would claim
  success while leaving orphaned layer versions behind."
  []
  (let [{:keys [exit out err]} (sh "aws" "lambda" "list-layer-versions"
                                   "--layer-name" runtime-layer-name
                                   "--query" "LayerVersions[].Version" "--output" "json")]
    (cond
      (zero? exit) (json/parse-string out)
      (str/includes? (or err "") "ResourceNotFoundException") []
      :else (die! "list-layer-versions failed:" err))))

(defn- function-exists? []
  (zero? (:exit (sh "aws" "lambda" "get-function" "--function-name" function-name))))

(defn- ensure-function! [layer-arn]
  (doseq [path [runtime-zip-path lambda-zip-path]]
    (when-not (.exists (java.io.File. path))
      (die! path "not found -- run `bb build` first.")))
  (if (function-exists?)
    (do
      (println "lambda-mvp-bb: updating function code for" function-name)
      (let [{:keys [exit err]} (sh "aws" "lambda" "update-function-code"
                                   "--function-name" function-name
                                   "--architectures" lambda-arch
                                   "--zip-file" (str "fileb://" lambda-zip-path))]
        (when-not (zero? exit) (die! "update-function-code failed:" err)))
      (let [{:keys [exit err]} (sh "aws" "lambda" "wait" "function-updated" "--function-name" function-name)]
        (when-not (zero? exit) (die! "function did not reach Active state:" err)))
      (println "lambda-mvp-bb: updating function configuration (layer" layer-arn ")")
      (let [{:keys [exit err]} (sh "aws" "lambda" "update-function-configuration"
                                   "--function-name" function-name
                                   "--layers" layer-arn
                                   "--timeout" "15" "--memory-size" "2048")]
        (when-not (zero? exit) (die! "update-function-configuration failed:" err))))
    (do
      (println "lambda-mvp-bb: creating function" function-name)
      (let [{:keys [exit err]} (sh "aws" "lambda" "create-function"
                                   "--function-name" function-name
                                   "--runtime" "provided.al2023"
                                   "--architectures" lambda-arch
                                   "--handler" lambda-handler
                                   "--zip-file" (str "fileb://" lambda-zip-path)
                                   "--layers" layer-arn
                                   "--role" (role-arn)
                                   "--timeout" "15" "--memory-size" "2048")]
        (when-not (zero? exit) (die! "create-function failed:" err)))))
  (let [{:keys [exit err]} (sh "aws" "lambda" "wait" "function-updated" "--function-name" function-name)]
    (when-not (zero? exit) (die! "function did not reach Active state:" err))))

(defn deploy! []
  (require-aws-identity!)
  (ensure-role!)
  (let [layer-arn (publish-layer!)]
    (ensure-function! layer-arn))
  (println "lambda-mvp-bb: deployed" function-name "->"
           (-> (sh "aws" "lambda" "get-function" "--function-name" function-name
                   "--query" "Configuration.FunctionArn" "--output" "text")
               :out str/trim)))

(defn invoke! []
  (require-aws-identity!)
  (let [out-file (str (System/getProperty "java.io.tmpdir") "/lambda-mvp-bb-invoke.json")
        {:keys [exit out err]}
        (sh "aws" "lambda" "invoke"
            "--function-name" function-name
            "--payload" "{}"
            "--cli-binary-format" "raw-in-base64-out"
            "--log-type" "Tail"
            "--query" "LogResult"
            "--output" "text"
            out-file)]
    (when-not (zero? exit) (die! "invoke failed:" err))
    (println "lambda-mvp-bb: response body:")
    (println (slurp out-file))
    (println "lambda-mvp-bb: log tail:")
    (println (String. (.decode (java.util.Base64/getDecoder) (str/trim out))))))

(defn teardown! []
  (require-aws-identity!)
  (when (function-exists?)
    (println "lambda-mvp-bb: deleting function" function-name)
    (let [{:keys [exit err]} (sh "aws" "lambda" "delete-function" "--function-name" function-name)]
      (when-not (zero? exit) (die! "delete-function failed:" err))))
  (doseq [version (layer-version-numbers)]
    (println "lambda-mvp-bb: deleting layer version" version)
    (let [{:keys [exit err]} (sh "aws" "lambda" "delete-layer-version"
                                 "--layer-name" runtime-layer-name
                                 "--version-number" (str version))]
      (when-not (zero? exit) (die! "delete-layer-version failed for version" version ":" err))))
  (when (role-exists?)
    (println "lambda-mvp-bb: detaching + deleting role" role-name)
    (let [{:keys [exit err]} (sh "aws" "iam" "detach-role-policy" "--role-name" role-name "--policy-arn" policy-arn)]
      (when-not (zero? exit) (die! "detach-role-policy failed:" err)))
    (let [{:keys [exit err]} (sh "aws" "iam" "delete-role" "--role-name" role-name)]
      (when-not (zero? exit) (die! "delete-role failed:" err))))
  (println "lambda-mvp-bb: teardown complete"))

(defn -main [& args]
  (case (first args)
    "deploy" (deploy!)
    "invoke" (invoke!)
    "teardown" (teardown!)
    (die! "usage: aws_lifecycle.clj deploy|invoke|teardown")))

(apply -main *command-line-args*)
