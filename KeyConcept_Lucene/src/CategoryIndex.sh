#!/bin/sh

#File CategoryIndex.sh
#Use this shell script to create a trained index using the CategoryIndex class.

#The following variables need to be modified to suit your specific case:
indexDir='/home/scanders/TestCatIndex'
filesDir='/home/scanders/TestDirectory'
stdTree='/home/scanders/test_standard_tree.txt'
memFlag='false'
projDir='/home/scanders/KeyConcept_Lucene'

#Shouldn't need to modify jarDir
jarDir=$projDir"/libs"

echo "Running..."
java -cp $projDir/src:\
$jarDir/jsoup-1.6.2.jar:\
$jarDir/lucene-core-3.6.0.jar:\
$jarDir/mysql-connector-java-5.1.21-bin.jar \
keyconcept.lucene.index.CategoryIndex $indexDir $filesDir $stdTree $memFlag


