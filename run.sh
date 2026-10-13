#!/bin/sh
mkdir -p out && javac -d out $(find . -name "*.java") && java -cp out MainApp
