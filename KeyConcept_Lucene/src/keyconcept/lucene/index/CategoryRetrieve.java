package keyconcept.lucene.index;
/** 
 * File: CategoryRetrieve.java
 * Author: Sawyer Anderson (June 2012)
 * 
 * This file is just a wrapper class around CategoryIndex.java that uses CategoryIndex.query() to print category-weight pairs
 * for a given document or collection of documents to a file. 
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CategoryRetrieve {

	/**
	 * Queries a document or documents against an index containing category information, then writes the results of the query to a file. Results are sorted
	 * by weight.
	 * @param args Arguments in this order (if you do not want to pass a filenames file or a query document, simply enter "null" for that argument):
	 * <Lucene index directory> <query document or null> <filenames file or null> <output file> <number of categories to write per doc> <number of top words to use in comparison> <number of top documents to use when summing category weights>
	 */
	public static void main(String[] args) {
		/*Assign index directory*/
		File indexDirectory = new File (args[0]);
		
		/*Assign query document, if it exists*/
		File queryDoc = null;
		if (!(args[1].equals("null"))) queryDoc = new File (args[1]);
		
		/*Assign filenames file, if it exists*/
		File queryFilenames = null;
		if (!(args[2].equals("null"))) queryFilenames = new File(args[2]);
		
		/*Assign output file*/
		File queryResults = new File (args[3]);
		
		/*Assign maximum number of result categories*/
		int maxResultCats = Integer.valueOf(args[4]);
		
		/*Assign number of top words to use when querying*/
		int numTopWords = Integer.valueOf(args[5]);
		
		/*Assign number of top documents to use when summing category weights*/
		int numTopDocs = Integer.valueOf(args[6]);
		
		CategoryIndex catIndex = new CategoryIndex(indexDirectory);
		System.out.println("Retrieving categories...");
		
		/*Query using a single document*/
		try{
			if(!(queryDoc==null)){
				System.out.println("Querying...");
				Map<String, Float> queryHits = catIndex.queryCategory(queryDoc, numTopWords, numTopDocs, maxResultCats);
				List<Map.Entry<String, Float>> catWeights = new ArrayList<Map.Entry<String,Float>>();
				for (Map.Entry<String,Float> e : queryHits.entrySet()){
					catWeights.add(e);
				}
				catWeights = sortStringFloatListByValue(catWeights); //sorting weights
				writeCatWeightsToFile(catWeights, queryResults, queryDoc); //writing the weights to file
			}
		/*Query using a list of filenames*/
			else if (!(queryFilenames==null)){
				BufferedReader br = new BufferedReader(new FileReader(queryFilenames));
				String line;
				List<File> queryDocs = new ArrayList<File>();
				while ((line=br.readLine())!=null){
					File tempFile = new File (line);
					queryDocs.add(tempFile);
				}
				for (File f : queryDocs) {
					System.out.println("Querying " + f.getAbsolutePath());
					Map<String, Float> queryHits = catIndex.queryCategory(f, numTopWords, numTopDocs, maxResultCats);
					List<Map.Entry<String, Float>> catWeights = new ArrayList<Map.Entry<String,Float>>();
					for (Map.Entry<String,Float> e : queryHits.entrySet()){
						catWeights.add(e);
					}
					catWeights = sortStringFloatListByValue(catWeights); //sorting weights
					writeCatWeightsToFile(catWeights, queryResults, f); //writing the weights to file
				}
			}
			
			System.out.println("Category Retrieval completed. Results are stored in " + queryResults.getAbsolutePath());
			
		} catch (Exception e){
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}

	}
	
	/** 
	 * Sorts the passed List of Map.Entry<String, Float> by value.
	 * @param entriesList The List of Map.Entry<String,Float> to be sorted
	 */
	private static ArrayList<Map.Entry<String,Float>> sortStringFloatListByValue(List<Map.Entry<String, Float>> entriesList){
		ArrayList<Map.Entry<String, Float>> sortedList = new ArrayList<Map.Entry<String,Float>>();
		for (Map.Entry<String, Float> e : entriesList){
			sortedList.add(e);
		}
		WeightComparator wc = new WeightComparator();
		Collections.sort(sortedList, wc);
		return sortedList;
	}
	
	/**
	 * Writes the name of the query doc, followed by all categoryIDs and weights of the results of a query, one category ID and weight per line.
	 * @param weights Sorted List of Map.Entry<String,Float> that map category IDs to weights
	 * @param outputFile The File to write category IDs and weights to.
	 * @param queryDoc The File that was queried against the index
	 * @throws IOException
	 */
	private static void writeCatWeightsToFile(List<Map.Entry<String, Float>> weights, File outputFile, File queryDoc) throws IOException{
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, true));
		bw.write(queryDoc.getAbsolutePath() + "\n");
		for (Map.Entry<String, Float> e : weights){
			bw.write(e.getKey() + " " + e.getValue()+ "\n");
		}
		bw.close();
	}

}
