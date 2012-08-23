package keyconcept.lucene.profile.view;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import keyconcept.lucene.profile.Profile;

public class RelatedViewer {
	File content = null;;
	Profile profile = null;
	protected List<Topic> topicList;
	
	
	public RelatedViewer(Profile _p, File _content){
		profile = _p;
		content = _content;
	}
	
	/**
	 * Uses content.rdf.u8 from ODP data (dmoz.org) to return links, titles, and descriptions of all files that fall within the same categories as the categories
	 * in which the profile has a recorded weight.
	 * 
	 * @return A list of Topic objects, which can be used to get the associated urls, titles, and descriptions of pages.
	 * @throws IOException
	 */
	public List<Topic> getRelated() throws IOException{
		List<Topic> topics = new ArrayList<Topic>();
		if (profile!=null){
			Map<String, Float> catweights = profile.getAll();
			/*For each category in the user profile */
			for (Map.Entry<String, Float> e : catweights.entrySet()){
//				System.out.println("e.getKey() is " + e.getKey());
				String catid = e.getKey();
				Topic t = new Topic(catid);
				
				BufferedReader br = new BufferedReader (new FileReader(content));
				String line = "";
				/*Search until this category's section is found.*/
//				System.out.println("Searching for " + catid);
				while (!line.equals("    <catid>"+catid+"</catid>")){
					line = br.readLine();
				}
//				System.out.println(catid + " found.");
				
				Page p = new Page();
				
				/*Until the next category is found, add all pages with their urls, titles, and descriptions to Topic t*/
				while (!line.contains("<Topic")){
//					System.out.println("Adding pages, line is " + line);
					if (line.contains("<ExternalPage")){
						int urlIndex = line.indexOf("about=\"")+7;
						int urlEndIndex = line.lastIndexOf("\"");
						p.setUrl(line.substring(urlIndex, urlEndIndex));
//						System.out.println("url: " + p.getUrl());
					}
					else if (line.contains("<d:Title>")){
						int titleIndex = line.indexOf("<d:Title>")+9;
						int titleEndIndex = line.indexOf("</d:Title>");
						p.setTitle(line.substring(titleIndex, titleEndIndex));
//						System.out.println("title: " + p.getTitle());
					}
					else if (line.contains("<d:Description>")){
						int descIndex = line.indexOf("<d:Description>")+15;
						int descEndIndex = line.indexOf("</d:Description>");
						p.setDesc(line.substring(descIndex,descEndIndex));
//						System.out.println("description: " + p.getDesc());
					}
					else if (line.contains("</ExternalPage>")){
						t.addPage(p);
						p = new Page();
					}
					line = br.readLine();
				}
				topics.add(t);
			}
			return topics;
		}
		else {
			return null;
		}
	}
}
