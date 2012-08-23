#!/bin/sh

#File CategoryIndex.sh
#Use this shell script to create an (untrained) index using the Index class.

#The following variables need to be modified to suit your specific case:
indexDir='/home/scanders/TestIndex'
filesDir='/home/scanders/TestDirectory' #can be '/path/to/files' or 'null'
filenamesFile='null' #can be '/path/to/filesnameFile' or 'null'
memFlag='false' #can be 'false' or 'true'
projDir='/home/scanders/KeyConcept_Lucene'

#Shouldn't need to modify jarDir
jarDir=$projDir"/libs"

echo "Running..."
java -cp $projDir/src:\
$jarDir/jsoup-1.6.2.jar:\
$jarDir/lucene-core-3.6.0.jar:\
$jarDir/mysql-connector-java-5.1.21-bin.jar \
keyconcept.lucene.index.Index $indexDir $filesDir $filenamesFile $memFlag
