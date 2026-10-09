#!/usr/bin/bash
set -e

if [ $# -ne 1 ]; then
    echo USAGE: $0 cpp-fragment-source-file
    exit 1
fi

BNFC_OUTPUT_DIR="bnfc_output/"
export CLASSPATH=.:/usr/share/java/cup.jar:/usr/share/java/JLex.jar:./$BNFC_OUTPUT_DIR
clojure tccpp.clj $1 

