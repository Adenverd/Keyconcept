package keyconcept.lucene.profile.view;

public class Page {
	protected String url;
	protected String title;
	protected String description;
	
	public Page(){
		url = title = description = null;
	}
	
	public Page(String _url){
		url = _url;
		title = description = null;
	}
	
	public void setUrl(String _url){
		url = _url;
	}
	
	public void setTitle(String _title){
		title = _title;
	}
	
	public void setDesc(String _desc){
		description = _desc;
	}
	
	public String getUrl(){
		return url;
	}
	
	public String getTitle(){
		return title;
	}
	
	public String getDesc(){
		return description;
	}
}
