package keyconcept.lucene.index;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException{
		try{
			File retFile = getFile("www.google.com");
			System.out.println("retFile.exists() is " + retFile.exists());
		} catch(Exception e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
		}

	}
	
	private static File getFile(String urlString) throws Exception{
        String contextPath = "C:\\Sawyer\\Dropbox\\sharedWorkspace";

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String now = sdf.format(cal.getTime());

        File file = new File(contextPath + "/WEB-INF/tmp/" + now);
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
        return file;
}


}
