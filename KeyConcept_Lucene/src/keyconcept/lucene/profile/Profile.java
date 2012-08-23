package keyconcept.lucene.profile;

//TODO add functionality for passing in a url, categorizing it, and adding it to a profile.

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import keyconcept.lucene.index.CategoryIndex;

public class Profile {
	private Map<String, Float> catWeights;
	private String name;
	
	public Profile(String name){
		this.name = name;
		catWeights = new HashMap<String, Float>();
	}
	
	/**
	 * Allows profile to be run from the command line. Simply outputs a list of catid-weight pairs to a file. If a file with the name "<Profile Name>.txt" already exists
	 * in the given directory, it is updated at the end of the program with the new category-weights (none will ever be deleted, only new weights will appear and weights
	 * may be updated).
	 * 
	 * 
	 * @param args <Profile Name> <Directory of files to be categorized, or null> <List of filenames to be categorized, or null> <Output directory> <Lucene index directory> <numTopWords> <numTopDocs> <numTopCategories>
	 */
	public static void main(String[] args) {
		/*Assign profile name*/
		Profile p = new Profile (args[0]);
		
		try{
			/*Assign files directory*/
			File filesDir = null;
			if(!(args[1].equals("null"))) filesDir = new File (args[1]);
			
			/*Assign filenames file*/
			File filenames = null;
			if(!(args[2].equals("null"))) filenames = new File (args[2]);
			
			/*Assign output file*/
			File outputFile = new File(args[3].concat("/".concat(p.getName().concat(".txt"))));
			/*If the output file already exists, build a profile from it*/
			if(outputFile.exists()){
				BufferedReader br = new BufferedReader(new FileReader(outputFile));
				String line="";
				while ((line=br.readLine())!=null){
					StringTokenizer strtok = new StringTokenizer(line, " ");
					String catid = strtok.nextToken();
					Float weight = Float.valueOf(strtok.nextToken());
					p.add(catid, weight);
				}
			}
			
			/*Assign lucene index directory*/
			File indexDir = new File(args[4]);
			
			/*Assign information necessary to categorize*/
			int numTopWords = Integer.valueOf(args[5]);
			int numTopDocs = Integer.valueOf(args[6]);		
			int numCategories = Integer.valueOf(args[7]);
			
			BufferedWriter bw = new BufferedWriter (new FileWriter(outputFile));
			
			/*If categorizing files in a directory*/
			if (filesDir!=null){
				/*For each file in the directory*/
				for (File f : filesDir.listFiles()){
					System.out.println("Adding " + f.getName() + " to the profile.");
					p.add(f, indexDir, numTopWords, numTopDocs, numCategories);
					System.out.println("Added.");
				}
			}
			
			/*If categorizing files in a list of filenames*/
			if (filenames!=null){
				BufferedReader br = new BufferedReader(new FileReader(filenames));
				String line = "";
				while ((line = br.readLine())!=null){
					File f = new File(line);
					p.add(f, indexDir, numTopWords, numTopDocs, numCategories);
				}
			}
			
			/*Write each category-weight pair to the output file, one per line*/
			System.out.println("Outputting...");
			for (Map.Entry<String, Float> e : p.getAll().entrySet()){
				System.out.println("Writing: " + e.getKey() + " " + e.getValue() + " to " + outputFile.getAbsolutePath());
				bw.write(e.getKey() + " " + String.valueOf(e.getValue()) + "\n");
			}
			
			bw.close();
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
		
		

	}
	
	public Float get(String catid){
		return catWeights.get(catid);
	}
	
	public Map<String, Float> getAll(){
		return catWeights;
	}
	
	/**
	 * Adds category and weight to the profile, returns the new total weight of catid in this profile.
	 * @param catid
	 * @param weight
	 * @return
	 */
	public Float add (String catid, Float weight){
		/*If the profile already has a weight for this category in it, update the weight*/
		if (catWeights.get(catid)!=null){
			Float w = catWeights.get(catid);
			w += weight;
			catWeights.remove(catid);
			catWeights.put(catid, w);
			return catWeights.get(catid);
		}
		/*Otherwise, just add the new category-weight pair*/
		else {
			catWeights.put(catid, weight);
			return catWeights.get(catid);
		}
	}
	
	/**
	 * Queries the passed document against a CategoryIndex located at indexDirectory.
	 * 
	 * @param queryDoc The document to categorized and added to the profile.
	 * @param indexDirectory The directory of the Lucene index to use to categorize the document.
	 * @param numTopWords The number of top words to use when categorizing the document.
	 * @param numTopDocs The number of top scoring documents to use when determining queryDoc's category
	 * @param numCategories The number of top categories to be added to the index.
	 */
	public void add(File queryDoc, File indexDirectory, int numTopWords, int numTopDocs, int numCategories){
		try{
			CategoryIndex catIndex = new CategoryIndex(indexDirectory);
			Map<String,Float> queryResults = catIndex.queryCategory(queryDoc, numTopWords, numTopDocs, numCategories);
			for (Map.Entry<String, Float> e : queryResults.entrySet()){
				this.add(e.getKey(), e.getValue());
			}
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
}