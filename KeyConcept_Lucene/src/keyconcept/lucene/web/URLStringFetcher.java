package keyconcept.lucene.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.StringTokenizer;

public class URLStringFetcher {
	
	private File getFile(String urlString) throws IOException{
		File file = null; 
		try{
			file = File.createTempFile("wpage", null);
			URL url = new URL(urlString);
			
			InputStream is = url.openStream();
			OutputStream os = new FileOutputStream(file);
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = is.read(buffer))!= -1) {
				os.write (buffer, 0, bytesRead);
			}
			is.close();
			os.close();
		} catch (Exception e){
			
		}
		
		String line = "www.google.com\nwww.yahoo.com\nwww.amazon.com";
		StringTokenizer strtok = new StringTokenizer(line, "\n");
		while (strtok.hasMoreTokens()){
			File f = getFile(strtok.nextToken());
			if (f!=null){
				
			}
		}
		
		return file;
	}

}
