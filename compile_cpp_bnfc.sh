#!/usr/bin/bash
set -e
BNFC_OUTPUT_DIR="bnfc_output/"
mkdir -p $BNFC_OUTPUT_DIR

export CLASSPATH=.:/usr/share/java/cup.jar:/usr/share/java/JLex.jar:./$BNFC_OUTPUT_DIR
bnfc -m -java -o $BNFC_OUTPUT_DIR ./CPP.cf
cd $BNFC_OUTPUT_DIR
make
