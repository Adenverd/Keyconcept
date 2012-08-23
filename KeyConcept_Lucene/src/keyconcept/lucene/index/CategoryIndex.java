package keyconcept.lucene.index;
/**
 * File: CategoryIndex.java
 * Author: Sawyer Anderson (June 2012)
 * This is one of two core classes of the keyconcept project. CategoryIndex provides an interface for querying documents and
 * strings against a Lucene index. The index must contain 
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;

public class CategoryIndex extends Index {
	private File stdTree=null;
	private File filesDir = null;
	private static StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);
	private static IndexWriterConfig inwc = new IndexWriterConfig(Version.LUCENE_36, analyzer);
	
	/**
	 * Should be used to open an existing CategoryIndex on disk for querying purposes. Prints an error message if no file named "trimmed_standard_tree.txt" is found in indexDirectory.
	 * @param indexDirectory The directory containing the Lucene index
	 */
	public CategoryIndex(File indexDirectory) {
		super(indexDirectory);
		File[] indexFiles = indexDirectory.listFiles();
		for (File f : indexFiles) {
			if (f.getName().equals("trimmed_standard_tree.txt")) stdTree = f;
		}
		if (stdTree==null){
			System.out.println("Error: no trimmed_standard_tree.txt found in index directory");
		}
	}
	
	/**
	 * Should be used to create a new CategoryIndex. Allows for querying, indexing, and categorizing operations to be performed.
	 * @param outputDirectory
	 * @param iwc
	 */
	public CategoryIndex(File outputDirectory, File _stdTree){
		super(outputDirectory, inwc);
		stdTree = _stdTree;
	}

	/**
	 * Creates a new categorized Lucene index
	 * @param args Arguments, in the following order:
	 * 	<Lucene index directory> <directory that contains files to index> <standard tree> <memory flag>
	 */
	public static void main(String[] args) {
		File outputDirectory = new File (args[0]);
		
		File filesDirectory = new File (args[1]);
		
		File standardTree = new File (args[2]);
		
		boolean memoryFlag = Boolean.valueOf(args[3]);		
		
		Set<File> files = new HashSet<File>();
		queueFiles(files, filesDirectory);
		
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_36, analyzer);
		
		CategoryIndex catIndex = new CategoryIndex(outputDirectory, standardTree);
		try {
			System.out.println("Training...");
			catIndex.train(files, filesDirectory, memoryFlag);
		} catch (IOException e) {
			System.out.println("IO error: " + e.getMessage());
			e.printStackTrace();
		}	
	}
	
	/**
	 * Indexes and adds category information for every file in a passed Set of files using stdTree to provide category IDs for the parent directory of each file.
	 * @param files the files to be indexed and categorized
	 * @param top The top directory of the collection of documents' hierarchy. This file is necessary because without it there is no way to determine the category ID
	 * of a file (folder names != category IDs in ODP data).
	 * @param memFlag true if indexing should be done in memory using MMapDirectory, false to allow FSDirectory.open() to choose the optimal type of Directory.
	 * @throws IOException
	 */
	public void train(Set<File> files, File top, boolean memFlag) throws IOException{
		if (memFlag){
			indexDir = new MMapDirectory(indexPath);
		}
		else {
			indexDir = FSDirectory.open(indexPath);
		}
		List<String> catIDList = new ArrayList<String>();
		IndexWriter indexWriter = new IndexWriter(indexDir, iwc);
		int x = 0;
		
		for (File file : files) {
			/* Document to be added to the index */
			Document doc = new Document();
			
			/* Get the category ID of the file's directory. If no corresponding category ID is found in the standard tree, stores the empty string in cat_id field*/
			String catID = getCatID(file.getParentFile(), top);
			catIDList.add(catID);
			
			/*Convert the file to a string, strip any HTML tags from it using JSoup*/
			String fileStripped = Jsoup.parse(readFile(file)).text();
			
			/*Store the size in bytes of the stripped string*/
			int size = fileStripped.getBytes().length;
			
			/*Store the stripped contents (which will be analyzed and indexed), path, and size (string representation of number of bytes) of file as fields in doc*/
			doc.add(new Field("contents", fileStripped, Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new Field("path", file.getAbsolutePath(), Field.Store.YES, Field.Index.NOT_ANALYZED));
			doc.add(new Field("size", String.valueOf(size), Field.Store.YES, Field.Index.NOT_ANALYZED));
			doc.add(new Field("cat_id", catID, Field.Store.YES, Field.Index.NOT_ANALYZED));
			
			/*Write to the index*/
			indexWriter.addDocument(doc);
			x++;
		}
		
		/*Commits don't occur until the IndexWriter is closed*/
		indexWriter.close();
		trimStandardTree(catIDList);
		System.out.println("Training completed. " + files.size() + " documents added to the index.");
	}
	
	/**
	 * Queries the passed queryDoc against an index with category information in order to learn which categories the document best fits into. 
	 * This is done by creating a new temporary index in memory, indexing the document, then submitting a query to the permanent index using the
	 * top terms and their weights.
	 * @param queryDoc The document to query against the categorized index
	 * @param numTopWords The number of top words in the query document to be used in the query
	 * @param numTopDocs The number of top documents to be used when summing up the weights of documents to determine best category fit
	 * @param numCategories The number of top categories to be returned
	 * @return A Map<String,Float> of the top n best matching categories (note that this map is unsorted) and their weights
	 */
	public Map<String,Float> queryCategory(File queryDoc, int numTopWords, int numTopDocs, int numCategories)throws IOException{
		FSDirectory fsDirectory = FSDirectory.open(indexPath);
		IndexReader indexReader = IndexReader.open(fsDirectory); //used for reading from the index		
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_36); //used to extract terms from a document
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_36, analyzer); //used to configure the indexWriter
		IndexSearcher indexSearcher = new IndexSearcher(indexReader); //used to search the index
		
		/* queryDirectory: this directory is used to index a query document. Many (most) html pages contain too many terms to simply be stripped of html and then used as a query. Thus,
		 * documents have to be indexed in order to find the top n terms, which can then be used as a query against the trained index.
		 */
		Directory queryDirectory = new RAMDirectory();
		IndexWriter queryIndexWriter = new IndexWriter(queryDirectory,indexWriterConfig); //writer for the query index
		
		String queryStr = "";
		
		//Create a new document for the query, strip the HTML from it, and store it in the queryIndex
		Document doc = new Document();
		String fStripped = Jsoup.parse(readFile(queryDoc)).text();
		doc.add(new Field("contents", fStripped, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
		queryIndexWriter.addDocument(doc);
		queryIndexWriter.close();
		
		//Count the frequency of each term in the new index (which only contains one document, our query document) and store them in a Map<String,Integer>
		IndexReader queryIndexReader = IndexReader.open(queryDirectory);
		TermFreqVector termFreqVector = queryIndexReader.getTermFreqVector(0, "contents");
		if (termFreqVector == null){
			System.out.println("queryIndexReader.getTermFreqVector returned null");
		}
		
		Map<String,Integer> termFreqs = new HashMap<String,Integer>(); //Map to store all of the terms and their frequencies
		ArrayList<Map.Entry<String, Integer>> entriesList = new ArrayList<Map.Entry<String, Integer>>(); //ArrayList for storing the terms sorted by frequency
		
		String[] terms = termFreqVector.getTerms(); //returns all terms in a document from a to z
		int[] termFrequencies = termFreqVector.getTermFrequencies(); //returns all of the frequencies of the terms, corresponding one to one with .getTerms()
		
		for (int x=0; x<terms.length; x++){ //populate the map
			termFreqs.put(terms[x], termFrequencies[x]);
		}
		
		for (Map.Entry<String, Integer> e : termFreqs.entrySet()){ //add all of the entries in the Map termFreqs to the ArrayList entriesList
			entriesList.add(e);
		}

		FreqComparator fc = new FreqComparator(); //compares Map.Entry<String,Integer> by value, rather than key
		Collections.sort(entriesList, fc); //sorts the ArrayList of Map.Entry<String,Integer> using the above frequency comparator

		//Getting the top n words in the document, putting them into a queryStr and accounting for weight
		if (numTopWords > entriesList.size()) numTopWords = entriesList.size(); //accounting for documents that may have fewer terms than the passed number of top terms to use
		for (int x=0; x<numTopWords; x++){
//			System.out.println(entriesList.get(x).getKey());
			queryStr=queryStr.concat("\""+entriesList.get(x).getKey() +"\"^"+entriesList.get(x).getValue()+" ");
		}
		
		//Collector to collect the top hits from querying the index (those documents that most closely match the query string)
		TopScoreDocCollector collector = TopScoreDocCollector.create(numTopDocs, true);
		Query q = null;
		//System.out.println("Parsing...");
		try{
			//parse the query
			q = new QueryParser(Version.LUCENE_36, "contents", analyzer).parse(queryStr);
		} catch (ParseException e) {
			System.out.println("Parse Error: " + e.getMessage());
		}
		//run the query
		indexSearcher.search(q, collector);
		
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		Map<String, Float> hm = new HashMap<String, Float>(); //maps category IDs to the sum of their weights in hits
		for (int i=0; i<hits.length; i++){
			//get the doc corresponding to this hit, store it in doc
			doc = indexSearcher.doc(hits[i].doc); 
			//store the category id of this document
			String catID = doc.get("cat_id");
			//if this category already has a weight in the hashmap, add the new document's weight to the previous weight and replace it in the hashmap
			if(hm.containsKey(catID)){ 
				float sumWeight = (Float) hm.get(catID);
				float iWeight = hits[i].score;
				hm.put(catID, sumWeight+iWeight);
			} 
			else { //if this is the first time a document from this category has been added to the hashmap
				hm.put(catID, hits[i].score);
			}
		} 
		
		List<Map.Entry<String, Float>> sortedCatIDs = mapToSortedList(hm); //convert from map to a sorted ArrayList
		
		/*return the correct number of top documents*/
		Map <String, Float> topNWeights = new HashMap<String,Float>();
		if (sortedCatIDs.size() < numCategories) numCategories = sortedCatIDs.size();
		for (int x=0; x<numCategories; x++){
			topNWeights.put(sortedCatIDs.get(x).getKey(), sortedCatIDs.get(x).getValue());
		}
		
		return topNWeights;
	}
	
	public Map<String,Float> queryCategory(String query, int numTopWords, int numTopDocs, int numCategories)throws IOException{
		FSDirectory fsDirectory = FSDirectory.open(indexPath);
		IndexReader indexReader = IndexReader.open(fsDirectory); //used for reading from the index		
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_36); //used to extract terms from a document
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_36, analyzer); //used to configure the indexWriter
		IndexSearcher indexSearcher = new IndexSearcher(indexReader); //used to search the index
		
		/* queryDirectory: this directory is used to index a query document. Many (most) html pages contain too many terms to simply be stripped of html and then used as a query. Thus,
		 * documents have to be indexed in order to find the top n terms, which can then be used as a query against the trained index.
		 */
		Directory queryDirectory = new RAMDirectory();
		IndexWriter queryIndexWriter = new IndexWriter(queryDirectory,indexWriterConfig); //writer for the query index
		
		String queryStr = "";
		
		//Create a new document for the query, strip the HTML from it, and store it in the queryIndex
		Document doc = new Document();
		String fStripped = Jsoup.parse(query).text();
		doc.add(new Field("contents", fStripped, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
		queryIndexWriter.addDocument(doc);
		queryIndexWriter.close();
		
		//Count the frequency of each term in the new index (which only contains one document, our query document) and store them in a Map<String,Integer>
		IndexReader queryIndexReader = IndexReader.open(queryDirectory);
		TermFreqVector termFreqVector = queryIndexReader.getTermFreqVector(0, "contents");
		if (termFreqVector == null){
			System.out.println("queryIndexReader.getTermFreqVector returned null");
		}
		
		Map<String,Integer> termFreqs = new HashMap<String,Integer>(); //Map to store all of the terms and their frequencies
		ArrayList<Map.Entry<String, Integer>> entriesList = new ArrayList<Map.Entry<String, Integer>>(); //ArrayList for storing the terms sorted by frequency
		
		String[] terms = termFreqVector.getTerms(); //returns all terms in a document from a to z
		int[] termFrequencies = termFreqVector.getTermFrequencies(); //returns all of the frequencies of the terms, corresponding one to one with .getTerms()
		
		for (int x=0; x<terms.length; x++){ //populate the map
			termFreqs.put(terms[x], termFrequencies[x]);
		}
		
		for (Map.Entry<String, Integer> e : termFreqs.entrySet()){ //add all of the entries in the Map termFreqs to the ArrayList entriesList
			entriesList.add(e);
		}

		FreqComparator fc = new FreqComparator(); //compares Map.Entry<String,Integer> by value, rather than key
		Collections.sort(entriesList, fc); //sorts the ArrayList of Map.Entry<String,Integer> using the above frequency comparator

		//Getting the top n words in the document, putting them into a queryStr and accounting for weight
		if (numTopWords > entriesList.size()) numTopWords = entriesList.size(); //accounting for documents that may have fewer terms than the passed number of top terms to use
		for (int x=0; x<numTopWords; x++){
//			System.out.println(entriesList.get(x).getKey());
			queryStr=queryStr.concat("\""+entriesList.get(x).getKey() +"\"^"+entriesList.get(x).getValue()+" ");
		}
		
		//Collector to collect the top hits from querying the index (those documents that most closely match the query string)
		TopScoreDocCollector collector = TopScoreDocCollector.create(numTopDocs, true);
		Query q = null;
//		System.out.println("Parsing...");
		try{
			//parse the query
			q = new QueryParser(Version.LUCENE_36, "contents", analyzer).parse(queryStr);
		} catch (ParseException e) {
			System.out.println("Parse Error: " + e.getMessage());
		}
		//run the query
		indexSearcher.search(q, collector);
		
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		Map<String, Float> hm = new HashMap<String, Float>(); //maps category IDs to the sum of their weights in hits
		for (int i=0; i<hits.length; i++){
			//get the doc corresponding to this hit, store it in doc
			doc = indexSearcher.doc(hits[i].doc); 
			//store the category id of this document
			String catID = doc.get("cat_id");
			//if this category already has a weight in the hashmap, add the new document's weight to the previous weight and replace it in the hashmap
			if(hm.containsKey(catID)){ 
				float sumWeight = (Float) hm.get(catID);
				float iWeight = hits[i].score;
				hm.put(catID, sumWeight+iWeight);
			} 
			else { //if this is the first time a document from this category has been added to the hashmap
				hm.put(catID, hits[i].score);
			}
		} 
		
		List<Map.Entry<String, Float>> sortedCatIDs = mapToSortedList(hm); //convert from map to a sorted ArrayList
		
		/*return the correct number of top documents*/
		Map <String, Float> topNWeights = new HashMap<String,Float>();
		if (sortedCatIDs.size() < numCategories) numCategories = sortedCatIDs.size();
		for (int x=0; x<numCategories; x++){
			topNWeights.put(sortedCatIDs.get(x).getKey(), sortedCatIDs.get(x).getValue());
		}
		
		return topNWeights;
	}
	
	/**
	 * Finds the corresponding catid to a filepath in the StandardTree
	 * @param path the absolute path of the category of the catID to be returned
	 * @return the catID corresponding to the path
	 */
	protected String getCatID(File f, File top) {
		String path = f.getAbsolutePath();
		String pathToken = "";
		String line = "";
		String catID = "";
		
		try{
			BufferedReader br = new BufferedReader(new FileReader(stdTree));
			while ((line=br.readLine())!=null){
				StringTokenizer strtok = new StringTokenizer(line);
				catID = strtok.nextToken();
				strtok.nextToken();
				pathToken = strtok.nextToken();
//				System.out.println("path is " + path +" and top + pathToken is " + top.getAbsolutePath()+"/"+pathToken);
				if (path.equals(top.getAbsolutePath()+"/"+pathToken)){
//					System.out.println("returning " + catID + "for path " + path);
					return catID;
				}
			}
			
		} catch (Exception e) {
			System.out.println("catID not found for path " + path + ": " + e.getMessage());
		}
		System.out.println("Returning empty string for " + f.getAbsolutePath());
		return "";
		
	}
	
	/**
	 * Converts a Map<String,Float> to a List<Map.Entry<String, Float>> and sorts it using collections.sort() and a comparator that compares Map.Entry by value rather than by key.
	 * @param map The map to be converted and sorted
	 * @return A sorted List<Map.Entry<String, Float>
	 */
	protected static List<Map.Entry<String,Float>> mapToSortedList(Map<String, Float> map){
		List<Map.Entry<String, Float>> entriesList = new ArrayList<Map.Entry<String, Float>>();
		
		for (Map.Entry<String, Float> e : map.entrySet()){
			/*Add all of the entries in the Map termFreqs to the ArrayList entriesList*/
			entriesList.add(e);
		}
		
		WeightComparator wc = new WeightComparator(); //use weight comparator because it uses floats
		Collections.sort(entriesList, wc);
		return entriesList;
	}
	
	/**
	 * Using an ArrayList of category IDs, this function creates a new standard tree in the output directory containing only categories that documents were added to.
	 * @param catIDList ArrayList of category IDs added to the index during train()
	 */
	private void trimStandardTree(List<String> catIDList) {
		try{
			
			BufferedWriter fout = new BufferedWriter (new FileWriter(indexPath+"/trimmed_standard_tree.txt")); //writer for outputting a trimmed standard tree
			for (String catID : catIDList) {
				FileReader fr = new FileReader(stdTree);
				BufferedReader br = new BufferedReader (fr);
				String line = "";
				String token = "";
				while ((line=br.readLine())!=null){
					StringTokenizer strtok = new StringTokenizer(line, " ");
					token = strtok.nextToken();
					if (token.equals(catID)) break;
					else if (strtok.hasMoreTokens()) token = strtok.nextToken();
				}
				if (catID.equals(token)){
					fout.write(line+"\n");
				}
				else {
					System.out.println ("Unable to find catID " + catID + " in untrimmed StandardTree.");
				}
				fr.close();
				br.close();
			}
			fout.close();
		}
		catch (Exception e) {
			System.out.println("Error in trimming standard tree: " + e.getMessage());
		}
	}
	

}
