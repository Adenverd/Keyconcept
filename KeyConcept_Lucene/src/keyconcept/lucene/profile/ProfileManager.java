package keyconcept.lucene.profile;

import java.awt.Cursor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;


public class ProfileManager {
	public static final String COLUMN_CATEGORY = "category_id";
	public static final String COLUMN_WEIGHT = "weight";
	
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "profilesDB";
	
	private Connection connection;
	
	private String url, user, password, dbName;
	
	/**
	 * 
	 * @param _url The url to be used to connect to the database manager service. For example, if the database is a mysql database on the localhost using the default port, "mysql://localhost:3306/"
	 * @param _dbName The name of the database used to store profiles
	 * @param _user The name of a user who has read/write permissions on the profiles database
	 * @param _password The password of the user
	 */
	public ProfileManager(String _url, String _dbName, String _user, String _password) {
		url = _url;
		dbName = _dbName;
		user = _user;
		password = _password;
		
		try{	
			Properties connectionProps = new Properties();
			connectionProps.put("user", user);
			connectionProps.put("password", password);
			
			connection = DriverManager.getConnection("jdbc:"+url+dbName, connectionProps);
		} catch (Exception e){
			System.out.println("Connection Error: " + e.getMessage());
			e.printStackTrace();
		}
		
	}

	
	/**
	 * If p does not already exist in the database, creates a new Profile and stores all category IDs and weights. If it does exist, adds all
	 * category-weight pairs to the table, updating (merging) any categories that already have a value and adding any new categories. To replace
	 * a profile, first use delete (p), then add (p).
	 * @param p
	 */
	public void add (Profile p) throws SQLException{
		/*Get all the category-weight pairs stored in p*/
		Map<String, Float> catWeights = p.getAll();
		
		ResultSet resultSet = null;
		Statement statement = null;
		PreparedStatement preparedStatement = null;
		PreparedStatement ps = null;
		
		try{
			/*Create the table if it doesn't already exist*/
			String createTableStatement = "CREATE TABLE IF NOT EXISTS " +p.getName()+ " ( " + COLUMN_CATEGORY + " VARCHAR(255) PRIMARY KEY, " + COLUMN_WEIGHT + " REAL);";
			preparedStatement = connection.prepareStatement(createTableStatement);
			preparedStatement.execute();

			/*For each category-weight pair*/
			for (Map.Entry<String, Float> e: catWeights.entrySet()){
				
//				System.out.println("p.getName(): " + p.getName() + "\te.getKey(): " + e.getKey());
				
				/*Query the table to see if it contains a row corresponding to this category*/
				statement = connection.createStatement();
				resultSet = statement.executeQuery("SELECT * FROM " + p.getName() + " WHERE " + COLUMN_CATEGORY + "='" + e.getKey()+"';");
				/*If the table DOES already contain a weight for this category*/
				if (resultSet.next()){
					/*Get the weight before the update*/
					float weight = resultSet.getFloat(COLUMN_WEIGHT);
					
					/*Add the new value to the previous value*/
					weight += e.getValue();
					
					/*Update the row in the table to reflect the new weight*/
					ps = connection.prepareStatement("UPDATE " + p.getName() + 
							" SET " + COLUMN_WEIGHT + "='" + String.valueOf(weight) + 
							"' WHERE " + COLUMN_CATEGORY +"='" + e.getKey() +"';");
//					System.out.println("ps is " + ps.toString());
					ps.execute();					
				}
				/*If the table does NOT already contain a row for this category*/
				else{
					/*Insert category and weight as a row into the table*/
					ps = connection.prepareStatement("INSERT INTO " + p.getName() +
							" VALUES('"+e.getKey()+"', '" + String.valueOf(e.getValue()) + "');");
//					System.out.println("ps is " + ps.toString());
					ps.execute();
				}
				
			}
		
		
		} catch (SQLException e){
			System.out.println("SQLException: " + e.getMessage());
			e.printStackTrace();
		}
		finally {
			if (preparedStatement != null)
				preparedStatement.close();
			if(ps != null)
				ps.close();
		}
		
	}
	
	/**
	 * Queries the database for a profile indicated by name. If it exists, creates a new profile containing all category IDs and weights, then returns that profile. Else returns null.
	 * @param name the name of the profile to query against the database.
	 * @return A profile representation of the table indicated by name, or null.
	 */
	public Profile get(String name){	
		
		
		Profile p = null;
		ResultSet resultSet = null;
		Statement statement = null;
		PreparedStatement preparedStatement = null;
		PreparedStatement ps = null;
		
		try{	
			statement = connection.createStatement();
			resultSet = statement.executeQuery("SHOW TABLES LIKE '"+name+"';");
			/*If the table exists*/
			if(resultSet.next()){
				/*Get all of the rows in the table*/
				p = new Profile(name);
				resultSet = statement.executeQuery("SELECT * FROM " + name + ";");
				resultSet.next();
				/*Add every row in the table to the Profile */
				while (!resultSet.isAfterLast()){
					String category = resultSet.getString(COLUMN_CATEGORY);
					Float weight = resultSet.getFloat(COLUMN_WEIGHT);
					p.add(category, weight);
					resultSet.next();
				}
			}
		} catch (SQLException e){
			System.out.println("SQLException: " + e.getMessage());
			e.printStackTrace();
		}
		
		return p;
	}
	
	/**
	 * Queries the list of tables in the database for name. If one or more rows are returned (as indicated by using resultSet.next()), returns true, else returns false.
	 * @param the name to query against the database
	 * @return true if the database contains a table indicated by name, else false
	 */
	public boolean contains (String name){
		try{
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("SHOW TABLES like '" + name + "';");
			if (resultSet.next()) return true;
		} catch (SQLException e){
			System.out.println("SQLException: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Deletes a profile from the database by dropping its table.
	 * @param name the name of the profile (table) to be dropped.
	 */
	public void delete(String name){
		try{
			Statement statement = connection.createStatement();
			statement.executeUpdate("DROP TABLE IF EXISTS " + name + ";");
		} catch (SQLException e){
			System.out.println("SQLException: " + e.getMessage());
			e.printStackTrace();
		}
		
	}
}
