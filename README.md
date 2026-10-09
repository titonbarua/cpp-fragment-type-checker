# CPP Fragment Type Checker in Clojure

This project implements a type-checker for the syntax described in assignment 2 from
textbook [R]. The problem statement is available in this [url](https://www.grammaticalframework.org/ipl-book/assignments/assignment2/assignment2.html).


## Installation and running

Assuming the operating system is a variant of Ubuntu/Debian GNU/Linux:

- Follow all the steps described in [R] book to get `bnfc` up and running with `java`.
- Install clojure with `sudo apt install clojure`.
- Compile the CPP fragment syntax description using bnfc: `bash compile_cpp_bnfc.sh`.
  This should create a directory named `bnfc_output` with lexers and parsers generated
  for Java language.
- Check syntax using `bash run_tccpp.sh samples/good.cc`. There are quite a few
  files in the `samples/` directory which should trigger appropriate type errors.


## What is Clojure and Why

Clojure is a modern Lisp variant with focus on immutable data-structures and efficient parallel
computation. It runs on top of quite a lot of VM's - mainly JVM, but also - Javascript, ErlangVM
and Dotnet CLR. Interoperability with Java was a blessing in this case as I could easily convert
the AST from Java to Clojure.

Being a Lisp, Clojure has excellant API for nested data structure manipulation, making the whole
affair simpler. I have some prior experience with Clojure which was also a deciding factor.

You can find more about Clojure [here](https://clojure.org/).


## Code Guide

- The `cpp_parser_loader.clj` contains Java interop code to load the parser and generate
  Java AST from given source file. The AST is then converted to a nested native list in
  Clojure.
- `type_checker.clj` file implements a typical type checker described in chaper 4 of the
  [R] book. Initially I thought about writing an annotated type-checker which will augment
  the syntax tree with type information. I coudn't follow through due to time constraints.
- The `tccpp.clj` reads a filename from standard output, parses the syntax and checks it.
- All the `check-*` variant functions in `type_checker.clj` also returns the `env`. This is
  in contrast to the book. As Clojure is a functional language, the idiomatic way is to
  return a modified `env` with potentially modified `vtable` instead of modifying some passed
  state in place.


## Limitations
- There is some loss of information about original syntax during AST conversion from
  Java to Clojure. As such, I don't have 'printTree' function or something similar to
  print the original source where the error occured.
