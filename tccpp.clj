(ns tccpp
  "C++ fragment type checker CLI program."
  {:author "Titon Barua <baruat@email.sc.edu>"}
  (:require [cpp-parser-loader :refer [parse-syntax]]
            [type-checker :refer [check]]))

(def HELP-MSG "USAGE: clj -M -m tcpp.clj <cpp-fragment-filename>")

(defn -main [& args]
  (let [source-file (first args)]
    (println "+============================================================")
    (printf "| Checking %s ...\n" source-file)
    (println "+------------------------------------------------------------")
    (try
      ;; Print OK if everything is good.
      (->> source-file
           (slurp)
           (parse-syntax)
           (check))
      (println "OK\n")

      (catch Exception e
        (let [data (ex-data e)]
          (if (= (:type data) :type-error)
            (do (println "TYPE ERROR")
                (println (ex-message e)))

            ;; Assuming all other errors are of type syntax error
            ;; with tremendous faith in our own application. :-)
            (do (println "SYNTAX ERROR")
                (println (ex-message e)))))))))
