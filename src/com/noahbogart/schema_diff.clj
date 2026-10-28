(ns com.noahbogart.schema-diff
  (:require
    [babashka.fs :as fs]
    [babashka.process :as p]
    [clojure.string :as str]
    [clojure.tools.namespace.file :refer [read-file-ns-decl]]
    [clojure.tools.namespace.find :refer [find-sources-in-dir]]
    [fipp.clojure :as fipp]
    [schema.core :as s]) 
  (:import
    [java.io StringWriter]))

(defn find-ns-decls
  [dirs]
  (into []
    (comp (mapcat find-sources-in-dir)
      (keep read-file-ns-decl)
      (keep second))
    dirs))

(defn require-dirs
  "returns a seq of namespace symbols"
  [{:keys [paths]}]
  (let [nses (find-ns-decls paths)]
    (when (empty? nses)
      (throw (ex-info "No namespaces to run" {:paths paths})))
    (apply require nses)
    nses))

(defn gather-schemas
  [nses]
  (->> (mapcat ns-interns nses)
    (keep (fn [[sym var']]
            (try
              (let [m (meta var')]
                (cond
                  ;; s/defn, s/defmulti, etc
                  (:schema m)
                  (sorted-map
                    :name (:name m)
                    :ns (ns-name (:ns m))
                    :schema (s/explain (:schema m)))
                  ;; s/defschema
                  (:name m)
                  (sorted-map
                    :name (:name m)
                    :ns (ns-name (:ns m))
                    :schema (s/explain (deref var')))))
              (catch Throwable t
                (println "Something broke when processing" sym)
                (prn t)
                nil))))
    (sort-by (juxt :ns :name))))

(defn pprint-str [form]
  (let [s (StringWriter.)]
    (fipp/pprint form {:writer s
                       :width 88})
    (str/trim (str s))))

(defn ns->output-file
  [config ns']
  (fs/file (:output config) (subs (#'clojure.core/root-resource ns') 1)))

(defn export-schemas
  [config schemas]
  (doseq [[ns' schemas] (group-by :ns schemas)]
    (let [schemas (->> schemas
                    (mapv #(str (:name %) "\n" (pprint-str (:schema %))))
                    (str/join "\n\n"))
          f (ns->output-file config ns')]
      (fs/create-dirs (fs/parent f))
      (spit f (str ns' "\n\n" schemas "\n")))))

(defn run [{:as opts
            :keys [output paths]
            :or {output "output"}}]
  (assert (sequential? paths) "gotta give me a seq of paths")
  (assert (seq paths) "gotta give me some paths")
  (println "Checking" (str/join ", " paths))
  (let [git-branch (str/trim (:out @(p/$ {:out :string} "git" "branch" "--show-current")))
        opts (-> opts
               (assoc :output (fs/file (or output "output") git-branch))
               (update :paths #(mapv fs/file %)))
        nses (require-dirs opts)
        schemas (gather-schemas nses)]
    (export-schemas opts schemas)
    (println "Done!")))

(comment
  (run {:paths ["corpus"]}))
