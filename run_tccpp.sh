#!/usr/bin/bash
set -e

if [ $# -ne 1 ]; then
    echo USAGE: $0 \<cpp-fragment-source-file\>
    echo Example: $0 samples/undefined_var.cc
    exit 1
fi

clj -M -m tccpp $1 
