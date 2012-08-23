/**
 * 
 */
package keyconcept.lucene.index;

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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

/**
 * @author Sawyer Anderson
 *
 */
public class Retrieve {

	/**
	 * Retrieves the docid, weight, and path of the documents most closely matching the query document(s) and outputs them to a file.
	 * @param args <Lucene index> <query document, or null> <file with filenames of query documents, or null> <output file> <maximum number of result documents per query doc> <number of top words to use in comparison>
	 */
	public static void main(String[] args)throws IOException{
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
		
		/*Assign maximum number of result documents*/
		int maxResultDocs = Integer.valueOf(args[4]);
		
		/*Assign number of top words to use when querying*/
		int numTopWords = Integer.valueOf(args[5]);
		
		Index index = new Index(indexDirectory);
		System.out.println("Retrieving...");
		/*Query using a single document*/
		if(!(queryDoc==null)){
			Map<String, Float> queryHits = index.query(queryDoc, maxResultDocs, numTopWords); //querying the index, storing results
			List<Map.Entry<String, Float>> weights = new ArrayList<Map.Entry<String,Float>>(); //list to hold sorted weights
			for (Map.Entry<String, Float> e: queryHits.entrySet()){ //adding weights in unsorted order
				weights.add(e);
			}
			weights = sortStringFloatListByValue(weights); //sorting weights
			writeWeightsToFile(weights, queryResults, indexDirectory, queryDoc); //writing the weights to file
		}
		/*Query using filenames*/
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
				Map<String, Float> queryHits = index.query(f, maxResultDocs, numTopWords);
				List<Map.Entry<String, Float>> weights = new ArrayList<Map.Entry<String,Float>>();
				for (Map.Entry<String, Float> e: queryHits.entrySet()){
					weights.add(e);
				}
				weights = sortStringFloatListByValue(weights);
				writeWeightsToFile(weights, queryResults, indexDirectory, f);
			}
		}
		System.out.println("Finished retrieving. Results are stored in " + queryResults.getAbsolutePath());
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
	 * Writes the docid, weight, and filepath of the results of a query
	 * @param weights The list of docids and their weights
	 * @param outputFile The file to write to
	 * @param _indexPath The path of the index (used to get paths)
	 * @throws IOException
	 */
	private static void writeWeightsToFile(List<Map.Entry<String, Float>> weights, File outputFile, File _indexPath, File queryDoc) throws IOException{
		FSDirectory fsDirectory = FSDirectory.open(_indexPath); 
		IndexReader indexReader = IndexReader.open(fsDirectory); //used for reading from the index
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, true));
		bw.write(queryDoc.getAbsolutePath() + "\n");
		for (Map.Entry<String, Float> e : weights){
			String path = indexReader.document(Integer.valueOf(e.getKey())).get("path");
			bw.write(e.getKey() + " " + e.getValue()+ " " + path + "\n");
		}
		bw.close();
	}


}
