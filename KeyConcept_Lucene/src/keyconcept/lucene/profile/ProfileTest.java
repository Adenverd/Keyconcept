package keyconcept.lucene.profile;

import java.util.Map;


public class ProfileTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Profile p = null;
		ProfileManager pm = new ProfileManager("mysql://localhost:3306/", "testdb", "root", "");
		//System.out.println("p.get(\"5\"): " + p.get("5"));
		try{
			p = pm.get("Test");
			System.out.println("p.name is " + p.getName());
			Map<String, Float> map = p.getAll();
			for (Map.Entry<String, Float> e: map.entrySet()){
				System.out.println(e.getKey() + ": " + e.getValue());
			}
			System.out.println("pm.contains(\"Test\") returns " + pm.contains("Test"));
			System.out.println("pm.contains(\"Test1\") returns " + pm.contains("Test1"));
			System.out.println("Dropping Test1");
			pm.delete("Test1");
			System.out.println("pm.contains(\"Test1\") returns " + pm.contains("Test1"));

		} catch (Exception e) {
			System.out.println("Closing error: " + e.getMessage());
			e.printStackTrace();
		}

	}

}
