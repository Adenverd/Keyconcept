#!/bin/sh

#File CategoryRetrieve.sh
#Use this shell script to query a trainedindex and write the query results to a file. Result categories are sorted
#by weight in the output file.

#The following variables need to be modified to suit your specific case:
indexDir='/home/scanders/TestIndex'
queryDoc='/home/scanders/QueryDocuments/Arabian_horse.html'
filenamesFile='null'
outputFile='/home/scanders/TestOutput/test1.txt'
numCategories='2'
numTopWords='50'
numTopDocs='15'
projDir='/home/scanders/KeyConcept_Lucene'

#Shouldn't need to modify jarDir
jarDir=$projDir"/libs"

echo "Running..."
java -cp $projDir/src:\
$jarDir/jsoup-1.6.2.jar:\
$jarDir/lucene-core-3.6.0.jar:\
$jarDir/mysql-connector-java-5.1.21-bin.jar \
keyconcept.lucene.index.CategoryRetrieve $indexDir $queryDoc $filenamesFile $outputFile $numCategories $numTopWords $numTopDocs
