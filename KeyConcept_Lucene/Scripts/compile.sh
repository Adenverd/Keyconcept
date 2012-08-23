#!/bin/sh

#File: compile.sh
#Use this shell script to compile the KeyConcept_Lucene project.
#You must modify the projDir variable below to reflect the top directory of the project, i.e. the one that contains the src and libs subdirectories.

projDir='/home/sawyer/Projects/sharedWorkspace/KeyConcept_Lucene'

jarDir=$projDir"/libs"

echo "Compiling index/*.java with project and jars."
javac -cp $projDir/src:\
$jarDir/jsoup-1.6.2.jar:\
$jarDir/lucene-core-3.6.0.jar:\
$jarDir/mysql-connector-java-5.1.21-bin.jar \
$projDir/src/keyconcept/lucene/index/*.java

echo "Compiling web/*.java with project and jars."
javac -cp $projDir/src:\
$jarDir/jsoup-1.6.2.jar:\
$jarDir/lucene-core-3.6.0.jar:\
$jarDir/mysql-connector-java-5.1.21-bin.jar \
$projDir/src/keyconcept/lucene/web/*.java

echo "Compiling profile/*.java with project and jars."
javac -cp $projDir/src:\
$jarDir/jsoup-1.6.2.jar:\
$jarDir/lucene-core-3.6.0.jar:\
$jarDir/mysql-connector-java-5.1.21-bin.jar \
$projDir/src/keyconcept/lucene/profile/*.java

echo "Compiling profile/view/*.java with project and jars."
javac -cp $projDir/src:\
$jarDir/jsoup-1.6.2.jar:\
$jarDir/lucene-core-3.6.0.jar:\
$jarDir/mysql-connector-java-5.1.21-bin.jar \
$projDir/src/keyconcept/lucene/profile/view/*.java