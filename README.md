# C++ Fragment Type Checker in Clojure

> This project was developed as a graduate course-work assignment for [CSCE 531
> Compiler Construction](https://cse.sc.edu/~mgv/csce531sp21/index.html) course
> in Spring 2023 semester. With an irrational hubris, my initial plan was to
> write a C++ fragment interpreter. The professor instructing the course
> rightfully dissuaded me from going that route, with concerns for time
> constraints and required effort. Instead, a type checker was born as a
> compromise.

This project implements a type-checker for the partial C++ syntax described in
assignment 2 from compiler textbook [Implementing Programming Languages: An
Introduction to Compilers and Interpreters, Aarne
Ranta](https://www.grammaticalframework.org/ipl-book/). The problem statement is
available
[here](https://www.grammaticalframework.org/ipl-book/assignments/assignment2/assignment2.html).


## Installation and Running

Assuming the operating system is a variant of Ubuntu/Debian GNU/Linux:

- Install `bnfc`, `jlex`, `java` and `clojure` with:
      `sudo apt install bnfc jlex default-jdk clojure`
- Compile the CPP fragment syntax description using bnfc:
      `bash compile_cpp_bnfc.sh`.
  This should create a directory named `bnfc_output` with lexers and parsers
  generated for Java language.
- Check syntax using:
       `bash run_tccpp.sh samples/good.cc`.
  There are quite a few files in the `samples/` directory which should trigger
  appropriate type errors.
- To check all samples, just run `make`


## Expected Output

```
+============================================================
| Checking ./samples/argument_conflict.cc ...
+------------------------------------------------------------
TYPE ERROR
Multiple function argument with same name: x
+============================================================
| Checking ./samples/bad.cc ...
+------------------------------------------------------------
TYPE ERROR
Expression 'c'  should have type bool, instead of type double
+============================================================
| Checking ./samples/function_application_type_error.cc ...
+------------------------------------------------------------
TYPE ERROR
Expression (EPlus (EId "x") (EId "y"))  should have type int, instead of type double
+============================================================
| Checking ./samples/function_redefinition.cc ...
+------------------------------------------------------------
TYPE ERROR
Function add is already defined
+============================================================
| Checking ./samples/good.cc ...
+------------------------------------------------------------
OK

+============================================================
| Checking ./samples/invalid_assignment.cc ...
+------------------------------------------------------------
TYPE ERROR
Expression 'hello'  should have type int, instead of type string
+============================================================
| Checking ./samples/invalid_if_cond.cc ...
+------------------------------------------------------------
TYPE ERROR
Expression 'hello'  should have type bool, instead of type string
+============================================================
| Checking ./samples/nested_scopes.cc ...
+------------------------------------------------------------
OK

+============================================================
| Checking ./samples/return_mismatch.cc ...
+------------------------------------------------------------
TYPE ERROR
Expression '444'  should have type int, instead of type string
+============================================================
| Checking ./samples/return_value_missing.cc ...
+------------------------------------------------------------
TYPE ERROR
Return statement missing value of type int
+============================================================
| Checking ./samples/undefined_function.cc ...
+------------------------------------------------------------
TYPE ERROR
Reference to undeclared function: add
+============================================================
| Checking ./samples/undefined_var.cc ...
+------------------------------------------------------------
TYPE ERROR
Reference to undeclared var: u
+============================================================
| Checking ./samples/var_redefinition.cc ...
+------------------------------------------------------------
TYPE ERROR
Variable x is already defined in current scope.
+============================================================
| Checking ./samples/wrong_addition.cc ...
+------------------------------------------------------------
TYPE ERROR
Expression 'b'  should have type int, instead of type string
```

## Code Guide

- The `cpp_parser_loader.clj` contains Java interop code to load the parser and
  generate Java AST from given source file. The AST is then converted to a
  nested native list in Clojure.
- `type_checker.clj` file implements a typical type checker described in chapter
  4 of the book. Initially I thought about writing an annotated type-checker
  which will augment the syntax tree with type information. I coudn't follow
  through due to time constraints.
- The `tccpp.clj` reads a filename from standard input, parses the syntax and
  checks it.
- All the `check-*` variant functions in `type_checker.clj` also returns the
  `env`. This is in contrast to the book. As Clojure is a functional language,
  the idiomatic way is to return a modified `env` with potentially modified
  `vtable` instead of modifying some passed state in place.


## Limitations

- There is some loss of information about original syntax during AST conversion
  from Java to Clojure. As such, I don't have `printTree` function or something
  similar to print the original source where the error occurred.
 

## What is Clojure and Why

Clojure is a modern Lisp variant with focus on immutable data-structures and
efficient parallel computation. It runs on top of quite a lot of VM's - mainly
JVM, but also - Javascript, ErlangVM and Dotnet CLR. Interoperability with Java
was a blessing in this case as I could easily convert the AST from Java to
Clojure.

Being a Lisp, Clojure has excellant API for nested data structure manipulation,
making the whole affair simpler. I had some prior experience with Clojure which
was also a deciding factor.

You can find more about Clojure [here](https://clojure.org/).

 
## License

Copyright © 2026 Titon Barua

Distributed under the MIT Public License.
