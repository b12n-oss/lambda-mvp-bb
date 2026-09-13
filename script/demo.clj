(ns script.demo
  "`bb demo`: build, deploy and invoke the demo handler in one go. Every
  check that can fail runs first (tools on PATH, AWS credentials and
  region), so a missing piece stops the run before a network download
  or any change to the AWS account. The steps themselves are the
  ordinary `build`, `deploy` and `invoke` tasks."
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [cheshire.core :as json]
            [clojure.string :as str]))

(defn- sh [& args]
  (let [{:keys [exit out err]} (apply p/shell {:out :string :err :string :continue true} args)]
    {:exit exit :out out :err err}))

(defn- die! [& msg]
  (binding [*out* *err*] (apply println "lambda-mvp-bb:" msg))
  (System/exit 1))

(defn- require-tools! []
  (let [missing (remove fs/which ["aws"])]
    (when (seq missing)
      (die! "not on PATH:" (str/join ", " missing)
            "-- see Prerequisites in docs/guide/getting-started.md."))))

(defn- require-aws! []
  (let [region (some not-empty [(System/getenv "AWS_REGION")
                                (System/getenv "AWS_DEFAULT_REGION")
                                (str/trim (:out (sh "aws" "configure" "get" "region")))])]
    (when-not region
      (die! "no AWS region set. Run `aws configure set region <region>` or set AWS_REGION, then retry."))
    (let [{:keys [exit out err]} (sh "aws" "sts" "get-caller-identity" "--output" "json")]
      (when-not (zero? exit)
        (die! "aws CLI has no usable credentials."
              "Run `aws configure` (or `aws configure sso`), or set AWS_PROFILE, then retry.\n"
              (str/trim (or err ""))))
      (let [{:strs [Account Arn]} (json/parse-string out)]
        (println "lambda-mvp-bb: will deploy to account" Account "in" region "as" Arn)))))

(defn- step! [task]
  (println (str "\nlambda-mvp-bb: == " task " =="))
  (when-not (zero? (:exit (p/shell {:continue true} "bb" task)))
    (die! task "failed; stopping.")))

(require-tools!)
(require-aws!)
(run! step! ["build" "deploy" "invoke"])
(println "\nlambda-mvp-bb: done. `bb invoke` calls it again; `bb teardown` deletes the function, role, and layer versions.")
