#!/bin/sh

#File Retrieve.sh
#Use this shell script to query an untrained index and write the query results to a file. Results are sorted
#by weight.

#The following variables need to be modified to suit your specific case:
indexDir='/home/scanders/TestCatIndex'
queryDoc='/home/scanders/QueryDocuments/Arabian_horse.html' #can be '/path/to/queryDoc' or 'null'
filenamesFile='null' #can be '/path/to/filenamesFile' or 'null'
outputFile='/home/scanders/TestOutput/test2.txt'
numTopWords='50'
numResultDocs='5'
projDir='/home/scanders/KeyConcept_Lucene'

#Shouldn't need to modify jarDir
jarDir=$projDir"/libs"

echo "Running..."
java -cp $projDir/src:\
$jarDir/jsoup-1.6.2.jar:\
$jarDir/lucene-core-3.6.0.jar:\
$jarDir/mysql-connector-java-5.1.21-bin.jar \
keyconcept.lucene.index.Retrieve $indexDir $queryDoc $filenamesFile $outputFile $numResultDocs $numTopWords

