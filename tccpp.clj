(ns tccpp
  (:require [clojure.pprint :refer [pprint]]

            [cpp-parser-loader :refer [parse-syntax]]
            [type-checker :refer [check]]))


(def HELP-MSG "USAGE: clojure tcpp.clj <cpp-fragment-filename>")

(defn main []
  (let [args *command-line-args*]
    (if (< (count args) 1)
      (do
        (println HELP-MSG)
        (System/exit 1))

      (let [source-file (first args)]
        (try
          ;; Print OK if everything is good.
          (->> source-file
               (slurp)
               (parse-syntax)
               (check))
          (println "OK")

          (catch Exception e
            (let [data (ex-data e)]
              #_(print e)
              (if (= (:type data) :type-error)
                (do (println "TYPE ERROR\n")
                    (println (ex-message e)))

                ;; Assuming all other errors are of type syntax error
                ;; with tremendous faith in our own application. :-)
                (println "SYNTAX ERROR")))))))))

(main)
