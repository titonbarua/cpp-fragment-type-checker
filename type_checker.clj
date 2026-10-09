(ns type-checker
  "C++ fragment type checker implementation."
  {:author "Titon Barua <baruat@email.sc.edu>"}
  (:require [clojure.pprint :as pprint]))

(defrecord TypedAST [tree type])

(defrecord FuncType [arg-types return-type])

(declare infer)
(declare check-statement)
(declare check-statement-list)
(declare check)
(declare lookup-var)
(declare lookup-func)
(declare extend-env)
(declare new-block)
(declare empty-env)

;; Used for user friendly error messages.
(def operator-syntax-map
  {'ETimes "*"
   'EDiv "/"
   'EPlus "+"
   'EMinus "-"
   'ELt  "<"
   'EGt  ">"
   'ELEq "<="
   'EGEq ">="
   'EAnd "&&"
   'EOr  "||"})

(def literal-type-map
  {'ETrue 'bool
   'EFalse 'bool
   'EInt 'int
   'EDouble 'double
   'EString 'string})

(def type-token-map
  {'Type_void 'void
   'Type_int 'int
   'Type_double 'double
   'Type_string 'string
   'Type_bool 'bool})

(defmulti infer
  (fn [env e]
    (let [category (first e)]
      (cond
        (contains? #{'ELt 'EGt 'ELEq 'EGEq 'EEq 'ENEq} category)
        :comparison-operator

        (contains? #{'ETrue 'EFalse, 'EInt, 'EDouble, 'EString} category)
        :literal

        (contains? #{'ETimes 'EDiv 'EPlus 'EMinus} category)
        :binary-arithmatic-operator

        (contains? #{'EAnd 'EOr} category)
        :conj-disj-operator

        (contains? #{'EIncr 'EDecr 'EPIncr 'EPDecr} category)
        :incr-decr-operator

        (= category 'EAss)
        :expression-assignment

        (= category 'EId)
        :variable

        (= category 'EApp)
        :function-application

        :else
        category))))

(defmethod infer :literal
  [env e]
  (let [[ctgy value] e
        t            (literal-type-map ctgy)]
    (->TypedAST e t)))

(defmethod infer :variable
  [env e]
  (let [[ctgy identifier] e
        t                 (lookup-var env identifier)]
    (->TypedAST e t)))

(defmethod infer :binary-arithmatic-operator
  [env e]
  (let [[ctgy a b] e
        at         (infer env a)
        t          (:type at)]
    (if (= ctgy 'EAdd)
      ;; + operator can operate on int, double and string.
      (when-not (contains? #{'int 'double 'string} t)
        (throw (ex-info "Operands to '+' must be int, double or string."
                        {:type :type-error})))

      ;; Rest of the arithmatic operators can operate on int or double.
      (when-not (contains? #{'int 'double} t)
        (throw (ex-info (format "Operands to '%s' must be int or double."
                                (operator-syntax-map ctgy))
                        {:type :type-error}))))
    (let [[_ bt] (check env b t)]
      (->TypedAST (list ctgy at bt) t))))

;; Comparison and (in)equality apply to two operands of the same type,
;; which is int, double, string, or bool.
(defmethod infer :comparison-operator
  [env e]
  (let [[ctgy a b] e
        at         (infer env a)
        t          (:type at)]
    (when-not (contains? #{'int 'double 'string 'bool} t)
      (throw (ex-info (format "Operands to '%s' must be int, double, string or bool."
                              (operator-syntax-map ctgy))
                      {:type :type-error})))
    (let [[_ bt] (check env b t)]
      (->TypedAST (list ctgy at bt) 'bool))))

;; Conjunction and disjunction apply to operands of type bool only.
(defmethod infer :conj-disj-operator
  [env e]
  (let [[ctgy a b] e
        [_ at]     (check env a 'bool)
        [_ bt]     (check env b 'bool)]
    (->TypedAST (list ctgy at bt) 'bool)))

(defmethod infer :incr-decr-operator
  [env e]
  (let [[ctgy x] e
        xt       (infer env x)
        t        (:type xt)]
    (when (or (not= (first x) 'EId)
              (not (contains? #{'int 'double} t)))
      (throw (ex-info (format "Operand to '%s' must be a variable of type int or double."
                              ctgy)
                      {:type :type-error})))
    (->TypedAST (list ctgy xt) t)))

(defmethod infer :expression-assignment
  [env e]
  (let [[ctgy l r] e]
    (when (or (not= (count l) 2)
              (= (first l) 'EId.))
      (throw (ex-info "Left side of the expression assignment can only contain variables."
                      {:type :type-error})))
    (let [lt     (infer env l)
          t      (:type lt)
          [_ rt] (check env r t)]
      (->TypedAST (list ctgy lt rt) t))))

(defmethod infer :function-application
  [env e]
  (let [[ctgy func-id arg-list] e
        func-type               (lookup-func env func-id)
        arg-types               (:arg-types func-type)
        return-type             (:return-type func-type)]
    ;; Check if argument count is appropriate.
    (when-not (= (count arg-types)
                 (count arg-list))
      (throw (ex-info (format "Function '%s' expects %s arguments, but %s were provided."
                              func-id
                              (count arg-types)
                              (count arg-list))
                      {:type :type-error})))

    ;; Check if all the arguments are of appropriate type.
    (doseq [[t e] (map vector arg-types arg-list)]
      (check env e t))

    ;; Type of a function application is the return type of function.
    (->TypedAST (list ctgy
                      func-id
                      (vec (map #(infer env %) arg-list)))
                return-type)))

(defn check-expression
  [env e t]
  (let [inferred-t (:type (infer env e))]
    (if (= inferred-t t)
      (->TypedAST e t)
      (do
        #_(pprint/pprint env)
        (throw (ex-info (format "Expression %s should have type %s, instead of type %s"
                                (let [ctgy (first e)]
                                  (if (contains? #{'EId 'EString 'EInt 'EDouble 'EBool} ctgy)
                                    (str "'" (second e) "' ")
                                    (str e " ")))
                                t
                                inferred-t)
                        {:type :type-error}))))))

(defmulti check-statement
  (fn [env statement]
    (first statement)))

;; Handle expression statement.
(defmethod check-statement 'SExp
  [env stmt]
  (let [[ctgy e] stmt
        et       (infer env e)
        t        (:type et)]
    (check-expression env e t)
    [env (-> TypedAST (list ctgy et) t)]))

;; Handle variable declaration.
(defmethod check-statement 'SDecls
  [env stmt]
  (let [[ctgy [var-type] var-names] stmt
        var-type                    (type-token-map var-type)]
    (if-not (> (count var-names) 0)
      (throw (ex-info (format "Variable declaration of type %s is missing identifier(s)"
                              var-type)
                      {:type :type-error}))
      (let [extended-env  (loop [env       env
                                 var-names var-names]
                            (let [vname (first var-names)]
                              (if-not vname
                                env
                                (recur (extend-env env vname var-type)
                                       (rest var-names)))))]
        [extended-env, stmt]))))

;; Handle variable initialization.
(defmethod check-statement 'SInit
  [env stmt]
  (let [[ctgy [var-type] var-name exp] stmt
        var-type                       (type-token-map var-type)
        extended-env                   (extend-env env var-name var-type)]
    (check-expression extended-env exp var-type)
    [extended-env stmt]))

;; Handle expression return statement.
(defmethod check-statement 'SReturn
  [env stmt]
  (let [[ctgy exp]  stmt
        ;; This is a special "variable" inserted into
        ;; the vtable each time a function scope is created.
        return-type (lookup-var env "__return__")]
    (check-expression env exp return-type)
    [env stmt]))

;; Handle void return statement.
(defmethod check-statement 'SReturnVoid
  [env stmt]
  (let [return-type (lookup-var env "__return__")]
    (when (not= return-type 'void)
      (throw (ex-info (format "Return statement missing value of type %s"
                              (name return-type))
                      {:type :type-error})))
    [env stmt]))

;; Handle while loop.
(defmethod check-statement 'SWhile
  [env stmt]
  (let [[ctgy cond-exp body] stmt]
    ;; Check condition expression.
    (check-expression env cond-exp 'bool)

    ;; Check while loop body.
    (check-statement env body)

    [env stmt]))

;; Handle blocks.
(defmethod check-statement 'SBlock
  [env stmt]
  (let [block-env         (new-block env)
        [ctgy statements] stmt]
    (check-statement-list block-env statements)
    [env stmt]))

;; Handle if-else.
(defmethod check-statement 'SIfElse
  [env stmt]
  (let [[ctgy cond-exp t-stmt f-stmt] stmt]
    (check-expression env cond-exp 'bool)
    (check-statement env t-stmt)
    (check-statement env f-stmt)
    [env stmt]))

(defn check-statement-list
  [env stmts]
  (let [altered-env (loop [env   env
                           stmts stmts]
                      (let [stmt (first stmts)]
                        (if-not stmt
                          env
                          (let [[new-env _] (check-statement env stmt)]
                            (recur new-env (rest stmts))))))]

    ;; TODO: Return annotated.
    [altered-env stmts]))

(defn check-function-arg-decl-list
  [arg-decl-list]
  (let [arg-names      (map #(nth % 2) arg-decl-list)
        arg-uniq-names (set arg-names)]
    (if (not= (count arg-names)
              (count arg-uniq-names))
      (let [dup-name (->> (frequencies arg-names)
                          (filter #(> (second %) 1))
                          (first)
                          (first))]
        (throw (ex-info (format "Multiple function argument with same name: %s"
                                dup-name)
                        {:type :type-error})))

      ;; Return a simplified list of tuples of arg type and name.
      (vec (for [[_ [var-type] var-name] arg-decl-list]
             [(type-token-map var-type) var-name])))))

(defn check-function-definition
  [env func-def]
  (let [[ctgy [ret-type] func-name arg-decl-list body] func-def

        ;; Internalize return type.
        ret-type   (type-token-map ret-type)
        ;; Check arguments and create a simplified tuple.
        arg-tuples (check-function-arg-decl-list arg-decl-list)

        ;; Create a new context.
        new-env (new-block env)
        ;; Add a special variable "__return__" into context
        ;; which helps checking return statements (if any).
        new-env (extend-env new-env "__return__" ret-type)]

    ;; Extend new context with function arguments.
    (let [extended-env (loop [tmp-env    new-env
                              arg-tuples arg-tuples]
                         (if (empty? arg-tuples)
                           tmp-env
                           (let [[at an] (first arg-tuples)]
                             (recur (extend-env tmp-env an at)
                                    (rest arg-tuples)))))]
      ;; Check body within the extended context.
      (check-statement-list extended-env body))

    ;; TODO: Return annotated func def.
    [env func-def]))

(declare func-def->type)
(defn build-ftable
  "Propagate ftable with all the function definitions."
  [env program]
  (let [[ctgy func-def-list] program]
    (assert (= ctgy 'PDefs))
    (loop [func-def-list func-def-list
           env           env]
      (if (empty? func-def-list)
        env
        (recur (rest func-def-list)
               (extend-env env (first func-def-list)))))))

(defn check
  ;; Check program.
  ([program]
   (let [env (empty-env)
         env (build-ftable env program)

         [ctgy func-defs] program]
     ;; Check all function definitions.
     (doseq [fdef func-defs]
       (check env fdef))

     ;; TODO: Return annotated program.
     [env program]))

  ;; Check expression.
  ([env e t]
   [env (check-expression env e t)])

  ;; Check sequence of statements or a function definition.
  ([env stmts-or-func-def]
   (let [first-el (first stmts-or-func-def)]
     (if (= first-el 'DFun)
       (check-function-definition env stmts-or-func-def)
       (check-statement-list env stmts-or-func-def)))))

;; Functions related to env modification and lookup.
;; ---------------------------------------------------------,
(defn empty-env
  []
  {:ftable  {}
   :vtables '()})

(defn new-block
  "Enter a new scope."
  [env]
  ;; Add a vtable on top of the stack.
  (update env :vtables #(cons {} %)))

(defn func-arg-decl->type
  "Get the type of the function argument declaration."
  [[ctgy [arg-type] arg-name]]
  (type-token-map arg-type))

(defn func-def->type
  "Get the type of the function definition."
  [func-def]
  (let [[ctgy [ret-type] func-name arg-decl-list body] func-def

        arg-types (vec (map func-arg-decl->type arg-decl-list))]
    (->FuncType arg-types
                (type-token-map ret-type))))

(defn extend-env
  "Extend `env` with variable or function definition."
  ([env identifier t]
   (let [vtables    (:vtables env)
         top-vtable (first vtables)]
     (if (contains? top-vtable identifier)
       (throw (ex-info (format "Variable %s is already defined in current scope."
                               identifier)
                       {:type :type-error}))
       (let [top-vtable (assoc top-vtable identifier t)]
         (assoc env
                :vtables
                (cons top-vtable (rest vtables)))))))

  ([env func-def]
   (let [t         (func-def->type func-def)
         func-name (nth func-def 2)]
     (if (get-in env [:ftable func-name])
       (throw (ex-info (format "Function %s is already defined"
                               func-name)
                       {:type :type-error}))
       (assoc-in env [:ftable func-name] t)))))

(defn lookup-var
  "Find and return type of a variable by looking up vtables."
  [env var-name]
  (loop [vtables (:vtables env)]
    (let [vt (first vtables)]
      (if (nil? vt)
        (throw (ex-info (format "Reference to undeclared var: %s"
                                var-name)
                        {:type :type-error}))
        (if (contains? vt var-name)
          ;; Return the type of the var-name.
          (vt var-name)

          ;; Lookup on high-level vtables.
          (recur (rest vtables)))))))

(defn lookup-func
  "Find and return type of a declared function."
  [env func-name]
  (let [ftable (:ftable env)]
    (if-not (contains? ftable func-name)
      (throw (ex-info (format "Reference to undeclared function: %s"
                              func-name)
                      {:type :type-error}))
      (ftable func-name))))
;; ---------------------------------------------------------'
