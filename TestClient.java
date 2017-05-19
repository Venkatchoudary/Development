import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.json.*;
import sun.misc.BASE64Encoder;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
 
public class TestClient {
 
    public static void main(String a[]) throws SQLException{
       
    	String url1 = "jdbc:postgresql://localhost:5433/siserver612"; 
        Connection conn = DriverManager.getConnection(url1,"postgres","postgres"); 
        Statement st = conn.createStatement(); 
        
        String url =  "http://services.groupkt.com/state/get/IND/all";
        String name1 = "siadmin";
        String password = "admin1";
        String authString = name1 + ":" + password;
        String authStringEnc = new BASE64Encoder().encode(authString.getBytes());
       // System.out.println(" Encoded authentication string: " + authStringEnc);
        Client restClient = Client.create();
        WebResource webResource = restClient.resource(url);
        ClientResponse resp = webResource.accept("application/json")
                                         .header("Authorization", "Basic " + authStringEnc)
                                         .get(ClientResponse.class);
        if(resp.getStatus() != 200){
            System.err.println("Unable to connect to the server");
        }
        String output = resp.getEntity(String.class);
        //System.out.println(output);
   
               
        JSONObject obj;
		try {
			obj = new JSONObject(output);
			String pageName = obj.getJSONObject("RestResponse").getString("result");
			//System.out.println("pageName:"+pageName);
			JSONArray jsonarray = new JSONArray(pageName);
			for (int i = 0; i < jsonarray.length(); i++) {
			    JSONObject jsonobject = jsonarray.getJSONObject(i);
			    String country = jsonobject.getString("country");
			    String name = jsonobject.getString("name");
			    String abbr = jsonobject.getString("abbr");
			    String capital = jsonobject.getString("capital");
			    
			    //System.out.println("name1:"+name1 +" alpha2_code:"+alpha2_code+" alpha3_code:"+alpha3_code+");");
			    String query="INSERT INTO public.ConsumeWebserviceData VALUES ('"+country+"', '"+name+"', '"+abbr+"','"+capital+"');";
			    System.out.println(query);
			    st.executeUpdate(query);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("Got an exception! "); 
            System.err.println(e.getMessage()); 
		}
		
		conn.close(); 
        

        
        
    }
}
