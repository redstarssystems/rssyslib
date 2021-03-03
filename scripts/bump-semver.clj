;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; the code below is taken from Leiningen
;; https://github.com/technomancy/leiningen/blob/master/src/leiningen/release.clj

(def ^:dynamic *level* nil)

(defn string->semantic-version [version-string]
  "Create map representing the given version string. Returns nil if the
  string does not follow guidelines setforth by Semantic Versioning 2.0.0,
  http://semver.org/"
  ;; <MajorVersion>.<MinorVersion>.<PatchVersion>[-<Qualifier>][-SNAPSHOT]
  (if-let [[_ major minor patch qualifier snapshot]
           (re-matches
             #"(\d+)\.(\d+)\.(\d+)(?:-(?!SNAPSHOT)([^\-]+))?(?:-(SNAPSHOT))?"
             version-string)]
    (->> [major minor patch]
      (map #(Integer/parseInt %))
      (zipmap [:major :minor :patch])
      (merge {:qualifier qualifier
              :snapshot  snapshot}))))

(defn parse-semantic-version [version-string]
  "Create map representing the given version string. Aborts with exit code 1
  if the string does not follow guidelines setforth by Semantic Versioning 2.0.0,
  http://semver.org/"
  (or (string->semantic-version version-string)
    (throw (ex-info "Unrecognized version string:" {:version version-string}))))

(defn version-map->string
  "Given a version-map, return a string representing the version."
  [version-map]
  (let [{:keys [major minor patch qualifier snapshot]} version-map]
    (cond-> (str major "." minor "." patch)
      qualifier (str "-" qualifier)
      snapshot (str "-" snapshot))))

(defn next-qualifier
  "Increments and returns the qualifier.  If an explicit `sublevel`
  is provided, then, if the original qualifier was using that sublevel,
  increments it, else returns that sublevel with \"1\" appended.
  Supports empty strings for sublevel, in which case the return value
  is effectively a BuildNumber."
  ([qualifier]
   (if-let [[_ sublevel] (re-matches #"([^\d]+)?(?:\d+)?"
                           (or qualifier ""))]
     (next-qualifier sublevel qualifier)
     "1"))
  ([sublevel qualifier]
   (let [pattern (re-pattern (str sublevel "([0-9]+)"))
         [_ n] (and qualifier (re-find pattern qualifier))]
     (str sublevel (inc (Integer. (or n 0)))))))

(defn bump-version-map
  "Given version as a map of the sort returned by parse-semantic-version, return
  a map of the version incremented in the level argument.  Always returns a
  SNAPSHOT version, unless the level is :release.  For :release, removes SNAPSHOT
  if the input is a SNAPSHOT, removes qualifier if the input is not a SNAPSHOT."
  [{:keys [major minor patch qualifier snapshot]} level value]
  (let [level (or level
                (if qualifier :qualifier)
                :patch)]
    (case (keyword (name level))
      :major {:major (if value value (inc major)) :minor 0 :patch 0 :qualifier nil :snapshot "SNAPSHOT"}
      :minor {:major major :minor (if value value (inc minor)) :patch 0 :qualifier nil :snapshot "SNAPSHOT"}
      :patch {:major major :minor minor :patch (if value value (inc patch)) :qualifier nil :snapshot "SNAPSHOT"}
      :alpha {:major     major :minor minor :patch patch
              :qualifier (next-qualifier "alpha" qualifier)
              :snapshot  "SNAPSHOT"}
      :beta {:major     major :minor minor :patch patch
             :qualifier (next-qualifier "beta" qualifier)
             :snapshot  "SNAPSHOT"}
      :rc {:major     major :minor minor :patch patch
           :qualifier (next-qualifier "RC" qualifier)
           :snapshot  "SNAPSHOT"}
      :qualifier {:major     major :minor minor :patch patch
                  :qualifier (next-qualifier qualifier)
                  :snapshot  "SNAPSHOT"}
      :release (merge {:major major :minor minor :patch patch}
                 (if snapshot
                   {:qualifier qualifier :snapshot nil}
                   {:qualifier nil :snapshot nil})))))

(defn bump-version
  "Given a version string, return the bumped version string -
   incremented at the indicated level. Add qualifier unless releasing
   non-snapshot. Level defaults to *level*."
  [version-str & [level value]]
  (-> version-str
    (parse-semantic-version)
    (bump-version-map (or level *level*) value)
    (version-map->string)))

;; end of Leiningen code
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(let [version (first *command-line-args*)
      level   (second *command-line-args*)
      value   (first (drop 2 *command-line-args*))]
  (try
    (println (bump-version version level value))
    (catch Exception e
      (binding [*out* *err*]
        (println "Error processing version value: " version " " (.getMessage e)))
      (println version))))
