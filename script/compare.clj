(ns compare
  (:require
    [babashka.process :as p]
    [clojure.string :as str]))

(defn check-clean-branch []
  (-> (p/pipeline
        (p/pb "git status --porcelain=v1")
        (p/pb {:out :string} "wc -l"))
    (last)
    (:out)
    (slurp)
    (str/trim)))

(defn task
  [_opts]
  (let [[main feature :as args] *command-line-args*]
    (assert (= 2 (count args)) "Must provide 2 branches")
    (let [clean-branch? (= "0" (check-clean-branch))]
      (assert clean-branch? "Must be called with a clean branch"))
    (try
      @(p/process "git" "switch" main)
      @(p/process "clojure" "-X:dev:test" "com.noahbogart.schema-diff/run" ":paths" "[\"corpus\"]")
      @(p/process "git" "switch" feature)
      @(p/process "clojure" "-X:dev:test" "com.noahbogart.schema-diff/run" ":paths" "[\"corpus\"]")
      (println (slurp (:out @(p/process {:inherit true} "git" "diff" "--no-index" (str "output/" main) (str "output/" feature)))))
      (finally
        @(p/process "git" "switch" main)))))

(comment
  (binding [*command-line-args* ["main" "other"]]
    (task nil)))
