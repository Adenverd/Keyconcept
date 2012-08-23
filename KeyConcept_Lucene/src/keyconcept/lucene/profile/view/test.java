package keyconcept.lucene.profile.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import keyconcept.lucene.profile.Profile;

public class test {
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		long count = 0;
		Profile p = new Profile("Test");
		p.add("425427", (float)7.89);
		p.add("999040", (float)10.08);
		p.add("469556", (float) 2);
		RelatedViewer rv = new RelatedViewer (p, new File("/home/sawyer/Projects/MPSS/content.rdf.u8"));
		
		try{
			List<Topic> topics  = rv.getRelated();
			for (Topic t : topics){
				for (Page page : t.getPages()){
					System.out.println("url: " + page.getUrl() +"\tTitle: " + page.getTitle() + "\tDesc: " + page.getDesc());
				}
			}
			
			long endTime = System.currentTimeMillis();
			long totalTime = endTime - startTime;
			System.out.println("Total run time is " + totalTime +"ms");
		} catch (Exception e){
			System.out.println("Error thrown: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
