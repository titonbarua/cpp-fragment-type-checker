(ns cpp-parser-loader
  "Functions to load bnfc parsed C++ AST in java class form. The AST is then
  converted to a clojure map."
  {:author "Titon Barua <baruat@email.sc.edu>"}
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :as pprint])
  (:import [cpp Yylex parser]))

(defn non-terminal?
  [tree]
  (str/starts-with? (str (type tree)) "class cpp.Absyn"))

(defn syntax-list?
  [tree]
  (str/starts-with? (str (type tree)) "class cpp.Absyn.List"))

(defn tree-root->symbol
  [node]
  (-> (str (class node))
      (str/split #"\.")
      (last)
      (symbol)))

(defn get-fields
  [obj]
  (let [cls    (class obj)
        fields (.getFields cls)]
    (->> fields
         (map #(vector (.getName %) (.get % obj)))
         (into {}))))

(defn extract-absyn-tree
  [tree]
  (if (non-terminal? tree)
    (if (syntax-list? tree)
      (vec (for [subtree tree]
             (extract-absyn-tree subtree)))

      (cons (tree-root->symbol tree)
            (for [[_ subtree] (get-fields tree)]
              (extract-absyn-tree subtree))))

    tree))

(defn parse-syntax
  [text]
  (let [l          (Yylex. (io/reader (char-array text)))
        p          (parser. l)
        parse-tree (.pProgram p)]
    (doall (extract-absyn-tree parse-tree))))

(comment
  (def some-syntax
    "
int main(int a, int b) {
  int y = \"hello\";
  int x;
  x = 1 + 2;
  { 3 + 4; }
  ++x;
  while(1) {2 + 3;}
  --u;
  print(a, b); return 42;
}
")
  (->> (parse-syntax some-syntax)
       (pprint/pprint)))

