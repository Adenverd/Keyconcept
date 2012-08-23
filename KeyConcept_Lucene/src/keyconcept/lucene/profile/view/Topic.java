package keyconcept.lucene.profile.view;

import java.util.ArrayList;
import java.util.List;

public class Topic {
	protected String catid;
	protected List<Page> pages;
	
	public Topic(String _cid){
		catid = _cid;
		pages = new ArrayList<Page>();
	}
	
	public void addPage(Page p){
		pages.add(p);
	}
	
	public void deletePage(Page p){
		pages.remove(p);
	}
	
	public String getCatId(){
		return catid;
	}
	
	public List<Page> getPages(){
		return pages;
	}

}
