#!/usr/bin/bash
set -e

BNFC_OUTPUT_DIR="./bnfc_output/"
export CLASSPATH=".:/usr/share/java/JLex.jar:/usr/share/java/cup.jar"

echo "Running bnfc on CPP.cf ..."

mkdir -p $BNFC_OUTPUT_DIR
bnfc -m --jlex --java -o $BNFC_OUTPUT_DIR ./CPP.cf
cd $BNFC_OUTPUT_DIR
make
