package keyconcept.lucene.web;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import keyconcept.lucene.index.Index;


public class cgiInterface{
	
	protected static File indexDirectory = new File("/home/scanders/KeyConcept_Lucene/Web/index");
	protected static File stdTree = new File("/home/scanders/KeyConcept_Lucene/Web/index/trimmed_standard_tree.txt");
	
	public static void main(String[] args) throws IOException{
	
		Index index = new Index(indexDirectory);
		
		System.out.println(cgi_lib.Header());
		
		Hashtable form_data = cgi_lib.ReadParse(System.in);
		
		int maxDocs = Integer.valueOf((String)form_data.get("maxDocs"));
		int numTopWords = Integer.valueOf((String)form_data.get("numTopWords"));
		
		String queryStr = (String)form_data.get("queryStr");
		
		Map<String, Float> weights = new HashMap<String,Float>();
		weights = index.query(queryStr, maxDocs, numTopWords);
		
		System.out.println("<table border = \"1\">");
		System.out.println("<th>docId</th>");
		System.out.println("<th>weight</th>");
		for (Map.Entry<String, Float> e : weights.entrySet()){
			System.out.println("<tr><td>"+e.getKey()+"</td><td>" + e.getValue() + "</td></tr>");
		}
		
	
	}

}
