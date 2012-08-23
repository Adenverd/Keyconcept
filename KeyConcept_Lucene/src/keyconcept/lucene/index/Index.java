package keyconcept.lucene.index;

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
import java.util.Scanner;
import java.util.Set;

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

public class Index {
	protected File indexPath; //The directory of the index
	protected Directory indexDir; //Lucene Directory for storing the index
	protected IndexWriterConfig iwc;
	
	/**
	 * Constructor for using an existing index.
	 * @param indexDirectory The directory of the existing index.
	 */
	public Index(File indexDirectory){
		indexPath = indexDirectory;
		iwc = null;
	}
	
	/**
	 * Constructor for creating a new index
	 * @param outputDirectory The directory to which the index is to be written.
	 * @param indexWriterConfig The configuration for the IndexWriter. Should almost always be one configured with StandardAnalyzer and Version.LUCENE_36.
	 */
	public Index(File outputDirectory, IndexWriterConfig indexWriterConfig){
		indexPath = outputDirectory;
		iwc = indexWriterConfig;
	}
	/**
	 * 
	 * @param args Arguments, in the following order:
	 * <Lucene index directory> <directory that contains files to index, or null> <absolute path of file that contains files to index, or null> <memory flag>
	 * 5 arguments must be passed, even if they are null. Sample run command:
	 * java Index /home/sawyer/Projects/MPSS/TestOutput /home/sawyer/Projects/MPSS/New_ODPFiles null null false
	 */
	public static void main(String[] args) {
		File outputDirectory = new File (args[0]);
		
		File filesDirectory = null;
		if (!args[1].equals("null"))filesDirectory = new File (args[1]);
		
		//TODO add filename queuing
		File filenames = null;
		if (!args[2].equals("null")) filenames = new File (args[2]);
		
		boolean memoryFlag = Boolean.valueOf(args[3]);
		
//		File queryDoc = new File ("/home/sawyer/Projects/MPSS/QueryDocuments/Arabian_horse.html");
//		File weightsOutputFile = new File ("/home/sawyer/Projects/MPSS/TrainerTest/weights.txt");
//		Map<String,Float> weights = null;
//		List<Map.Entry<String, Float>> sortWeights = new ArrayList<Map.Entry<String, Float>>();
		
		
		Set<File> files = new HashSet<File>();
		if (!(filesDirectory == null)) queueFiles(files, filesDirectory);
		if (!(filenames==null)) queueFilenames(files, filenames);
		
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_36, analyzer);
		
		Index index = new Index(outputDirectory, iwc);
		try {
			index.index(files, memoryFlag);
		} catch (IOException e) {
			System.out.println("IO error: " + e.getMessage());
			e.printStackTrace();
		}		

	}
	
	/**
	 * Recursively add all files below a directory to the queue. Note that hidden files will be added as well.
	 * @param file the top-most directory whose child files will be queued and child directories will be called with queueFiles(childDirectory), until the specified number of levels (numLevels) is reached
	 */
	public static void queueFiles (Set<File> files, File file){
			if(!file.exists()){
				System.out.println(file + " does not exist.");
			}
			if(file.isDirectory()){
				for (File f : file.listFiles()) { //recurse on all child files/directories
					queueFiles(files, f);
				}
			} 
			else {
				files.add(file);
			}
	}
	
	/**
	 * Adds the files in filenames file to the files queue
	 * @param files the Set of files to be added to the index
	 * @param filenames the text file containing filenames, one per line, of files to be added to Set of files
	 */
	public static void queueFilenames(Set<File> files, File filenames){
		try {
			BufferedReader br = new BufferedReader(new FileReader(filenames));
			String line;
			while ((line = br.readLine())!=null){
				File tempFile = new File (line);
				files.add(tempFile);
			}
		} catch (Exception e) {
			System.out.println("Error queuing from filenames file: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds files to the index. If no index is present in the directory represented by indexPath, creates a new index.
	 * @param files Files to be added to the index.
	 * @param memFlag If true, indexing will be done using Lucene's MMapDirectory, which utilizes m. If false, Lucene's FSDirectory.open(File) selects the optimal directory
	 * type for indexing, which may still be MMapDirectory. See the Lucene Javadoc for Directory, MMapDirectory, and FSDirectory.
	 * @throws IOException
	 */
	public void index(Set<File> files, boolean memFlag) throws IOException{
		System.out.println("Indexing...");
		if (memFlag){
			indexDir = new MMapDirectory(indexPath);
		}
		else {
			indexDir = FSDirectory.open(indexPath);
		}
		IndexWriter indexWriter = new IndexWriter(indexDir, iwc);
		int x = 0;
		
		for (File file : files) {
			/* Document to be added to the index */
			Document doc = new Document();
			
			/*Convert the file to a string, strip any HTML tags from it using JSoup*/
			String fileStripped = Jsoup.parse(readFile(file)).text();
			
			/*Store the size in bytes of the stripped string*/
			int size = fileStripped.getBytes().length;
			
			/*Store the stripped contents (which will be analzyed and indexed), path, and size (string representation of number of bytes) of file as fields in doc*/
			doc.add(new Field("contents", fileStripped, Field.Store.YES, Field.Index.ANALYZED));
			doc.add(new Field("path", file.getAbsolutePath(), Field.Store.YES, Field.Index.NOT_ANALYZED));
			doc.add(new Field("size", String.valueOf(size), Field.Store.YES, Field.Index.NOT_ANALYZED));
			
			/*Write to the index*/
			indexWriter.addDocument(doc);
			//System.out.println(x + ": " + file.getAbsolutePath());
			x++;
		}
		
		/*Commits don't occur until the IndexWriter is closed*/
		indexWriter.close();
		System.out.println("Indexing completed. " + files.size() + " documents added to the index.");
	}
	
	/**
	 * Queries the passed queryDoc against the index. This is done by creating a new temporary index in memory, indexing the query document, then submitting a query to
	 * the permanent index using the top terms and their weights.
	 * @param queryDoc The document to query against the index
	 * @param maxDocs The number of result documents and weights to be returned
	 * @param numTopWords The number of top words in the query document to be used in the query
	 * @return a Map<String, Float> mapping document ids to their weights.
	 * @throws IOException
	 */
	public Map<String,Float> query(File queryDoc, int maxDocs, int numTopWords) throws IOException{
		Map<String, Float> weights = new HashMap<String, Float>();
		
		FSDirectory fsDirectory = FSDirectory.open(indexPath); 
		IndexReader indexReader = IndexReader.open(fsDirectory); //used for reading from the index
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_36); //used to extract terms and omit stopwords from a document
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_36, analyzer); //used to configure the IndexWriter
		IndexSearcher indexSearcher = new IndexSearcher(indexReader); //used to search the Lucene index
		
		Directory queryDirectory = new RAMDirectory();
		IndexWriter queryIndexWriter = new IndexWriter(queryDirectory, indexWriterConfig);
		
		String queryStr = "";
		
		/*Create a new document for the query, strip the HTML from it, and store it in the queryIndex*/
		Document doc = new Document();
		String fStripped = Jsoup.parse(readFile(queryDoc)).text();
		doc.add(new Field("contents", fStripped, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
		queryIndexWriter.addDocument(doc);
		queryIndexWriter.close();
		
		/*Get the term frequencies for all of the terms in the document just written to the index*/
		IndexReader queryIndexReader = IndexReader.open(queryDirectory);
		TermFreqVector termFreqVector = queryIndexReader.getTermFreqVector(0, "contents");
		if (termFreqVector == null) {
			System.out.println("queryIndexReader.getTermFreqVector returned null");
		}
		
		Map<String,Integer> termFreqs = new HashMap<String,Integer>(); //Map to store all of the terms and their frequencies
		ArrayList<Map.Entry<String,Integer>> entriesList = new ArrayList<Map.Entry<String, Integer>>(); //ArrayList for storing the terms sorted by frequency
		
		String[] terms = termFreqVector.getTerms(); //returns all terms in a document from a to z
		int[] termFrequencies = termFreqVector.getTermFrequencies(); //stores all of the frequencies of the terms, corresponding one to one with TermFreqVector.getTerms()
		
		for (int x=0; x<terms.length; x++){
			/*populate the Map of term frequencies*/
			termFreqs.put(terms[x], termFrequencies[x]);
		}
		
		for (Map.Entry<String, Integer> e : termFreqs.entrySet()){
			/*Add all of the entries in the Map termFreqs to the ArrayList entriesList*/
			entriesList.add(e);
		}
		
		FreqComparator fc = new FreqComparator(); //custom comparator to compare Map.Entry<String, Integer> by value, rather than by key
		/*Sort the ArrayList of Map.Entry<String,Integer> using the above frequency comparator*/
		Collections.sort(entriesList, fc);
		
		/*Get the top n words in the document, put them into a queryStr and weight them according to their frequency*/
		if (numTopWords > entriesList.size()) numTopWords = entriesList.size(); //accounting for documents with fewer than numTopWords words
		for (int x=0; x<numTopWords; x++){
			queryStr=queryStr.concat("\""+entriesList.get(x).getKey() +"\"^"+entriesList.get(x).getValue()+" ");
		}
		
		/*Create a collector to collect the top scoring documents that will result from the search*/
		TopScoreDocCollector collector = TopScoreDocCollector.create(maxDocs, true);
		Query q = null;
		
		/*Parse the query*/
//		System.out.println("Parsing");
		try{
			q = new QueryParser(Version.LUCENE_36, "contents", analyzer).parse(queryStr);
		} catch (ParseException e) {
			System.out.println("Error in parsing: " + e.getMessage());
			e.printStackTrace();
		}
		
		/*run the query*/
		indexSearcher.search(q, collector);
		
		/*For each hit (document) in the collector, store the doc id and its weight in the Map of weights*/
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		for (int i=0; i<hits.length; i++) {
			weights.put(String.valueOf(hits[i].doc), hits[i].score);
		}
		
		return weights;
	}
	
	/**
	 * Queries the passed query string against the index. This is done by creating a new temporary index in memory, indexing the query document, then submitting a query to
	 * the permanent index using the top terms and their weights.
	 * @param query The String to query against the index
	 * @param maxDocs The number of result documents and weights to be returned
	 * @param numTopWords The number of top words in the query document to be used in the query
	 * @return a Map<String, Float> mapping document ids to their weights.
	 * @throws IOException
	 */
	public Map<String,Float> query(String query, int maxDocs, int numTopWords) throws IOException{
		Map<String, Float> weights = new HashMap<String, Float>();
		
		FSDirectory fsDirectory = FSDirectory.open(indexPath); 
		IndexReader indexReader = IndexReader.open(fsDirectory); //used for reading from the index
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_36); //used to extract terms and omit stopwords from a document
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_36, analyzer); //used to configure the IndexWriter
		IndexSearcher indexSearcher = new IndexSearcher(indexReader); //used to search the Lucene index
		
		Directory queryDirectory = new RAMDirectory();
		IndexWriter queryIndexWriter = new IndexWriter(queryDirectory, indexWriterConfig);
		
		String queryStr = "";
		
		/*Create a new document for the query, strip the HTML from it, and store it in the queryIndex*/
		Document doc = new Document();
		String fStripped = Jsoup.parse(query).text();
		doc.add(new Field("contents", fStripped, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
		queryIndexWriter.addDocument(doc);
		queryIndexWriter.close();
		
		/*Get the term frequencies for all of the terms in the document just written to the index*/
		IndexReader queryIndexReader = IndexReader.open(queryDirectory);
		TermFreqVector termFreqVector = queryIndexReader.getTermFreqVector(0, "contents");
		if (termFreqVector == null) {
			System.out.println("queryIndexReader.getTermFreqVector returned null");
		}
		
		Map<String,Integer> termFreqs = new HashMap<String,Integer>(); //Map to store all of the terms and their frequencies
		ArrayList<Map.Entry<String,Integer>> entriesList = new ArrayList<Map.Entry<String, Integer>>(); //ArrayList for storing the terms sorted by frequency
		
		String[] terms = termFreqVector.getTerms(); //returns all terms in a document from a to z
		int[] termFrequencies = termFreqVector.getTermFrequencies(); //stores all of the frequencies of the terms, corresponding one to one with TermFreqVector.getTerms()
		
		for (int x=0; x<terms.length; x++){
			/*populate the Map of term frequencies*/
			termFreqs.put(terms[x], termFrequencies[x]);
		}
		
		for (Map.Entry<String, Integer> e : termFreqs.entrySet()){
			/*Add all of the entries in the Map termFreqs to the ArrayList entriesList*/
			entriesList.add(e);
		}
		
		FreqComparator fc = new FreqComparator(); //custom comparator to compare Map.Entry<String, Integer> by value, rather than by key
		/*Sort the ArrayList of Map.Entry<String,Integer> using the above frequency comparator*/
		Collections.sort(entriesList, fc);
		
		/*Get the top n words in the document, put them into a queryStr and weight them according to their frequency*/
		if (numTopWords > entriesList.size()) numTopWords = entriesList.size(); //accounting for documents with fewer than numTopWords words
		for (int x=0; x<numTopWords; x++){
			queryStr=queryStr.concat("\""+entriesList.get(x).getKey() +"\"^"+entriesList.get(x).getValue()+" ");
		}
		
		/*Create a collector to collect the top scoring documents that will result from the search*/
		TopScoreDocCollector collector = TopScoreDocCollector.create(maxDocs, true);
		Query q = null;
		
		/*Parse the query*/
//		System.out.println("Parsing");
		try{
			q = new QueryParser(Version.LUCENE_36, "contents", analyzer).parse(queryStr);
		} catch (ParseException e) {
			System.out.println("Error in parsing: " + e.getMessage());
			e.printStackTrace();
		}
		
		/*run the query*/
		indexSearcher.search(q, collector);
		
		/*For each hit (document) in the collector, store the doc id and its weight in the Map of weights*/
		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		for (int i=0; i<hits.length; i++) {
			weights.put(String.valueOf(hits[i].doc), hits[i].score);
		}
		
		return weights;
	}
//TODO add specialized analyzer/preprocessing	
//	private Map<String, Float> query(File queryDoc, int maxDocs, Analyzer analyzer){
//		
//	}
	/**
	 * Reads the text from a file and returns it in String format
	 * @param file the file to be read
	 * @return A string representation of the text of the file
	 * @throws IOException
	 */
	protected static String readFile(File file){
		try{
		    StringBuilder fileContents = new StringBuilder((int)file.length());
		    Scanner scanner = new Scanner(file);
		    String lineSeparator = System.getProperty("line.separator");
	
		    try {
		        while(scanner.hasNextLine()) {        
		            fileContents.append(scanner.nextLine() + lineSeparator);
		        }
		        return fileContents.toString();
		    } finally {
		        scanner.close();
		    }
		} catch (Exception ioe){
			System.out.println("Error reading file " + file.getAbsolutePath() + ": " + ioe.getMessage());
			ioe.printStackTrace();
		}
		return "";
	}
	
	/**
	 * Sorts the passed List of Map.Entry<String, Float> by value.
	 * @param entriesList The List of Map.Entry<String,Float> to be sorted
	 */
	protected static ArrayList<Map.Entry<String,Float>> sortStringFloatListByValue(List<Map.Entry<String, Float>> entriesList){
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
	protected static void writeWeightsToFile(List<Map.Entry<String, Float>> weights, File outputFile, File _indexPath) throws IOException{
		FSDirectory fsDirectory = FSDirectory.open(_indexPath); 
		IndexReader indexReader = IndexReader.open(fsDirectory); //used for reading from the index
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
		for (Map.Entry<String, Float> e : weights){
			String path = indexReader.document(Integer.valueOf(e.getKey())).get("path");
			bw.write(e.getKey() + " " + e.getValue()+ " " + path + "\n");
		}
		bw.close();
	}

}
