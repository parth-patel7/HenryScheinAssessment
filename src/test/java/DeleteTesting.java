import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;
public class DeleteTesting {

    DBConnection connection = new DBConnection();

    public Object connect(String link, String username, String password) throws IOException {
        URL url = new URL(link);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if(username != null && password != null && username != "" && password != "") {
            String authorization =
                    Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("authorization", "Basic " + authorization);
        }
        connection.setRequestMethod("DELETE");
        if (connection.getResponseCode() == 200) {
            InputStream responseStream = connection.getInputStream();
            String s = new BufferedReader(
                    new InputStreamReader(responseStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            return s;
        } else {
            String errorData = null;
            if(connection.getErrorStream() != null) {
                errorData = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));
            }
            return new ErrorObject(connection.getResponseCode(), errorData);
        }
    }


    @Before
    public void createPerson(){
        String query = "Insert into Person (ID, FIRST_NAME, LAST_NAME, PHONE_NUMBER)" +
                " values (20, 'Test', 'Testing', '1231231231')";
        connection.runQuery(query);
    }

    // With no authentication/authorization and ID exists
    @Test
    public void deletePersonWithoutAuth() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/delete-person/20", "", "");
        Assert.assertEquals(401, eo.responseCode);
        Assert.assertEquals(true, connection.checkIfDataExists("Select * from Person Where ID = 20"));
        // Message and code
    }

    // With no authentication/authorization and ID does not exists
    @Test
    public void deletePersonWithoutAuthAndID() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/delete-person/20", "", "");
        Assert.assertEquals(401, eo.responseCode);
        Assert.assertEquals(true, connection.checkIfDataExists("Select * from Person Where ID = 20"));
    }

    // With all inputs in proper format [Write access]
    @Test
    public void deletePersonWithWriteAccess() throws IOException{
        String st = (String) connect("http://localhost:8083/v1/delete-person/20", "admin", "testPassword");
        Assert.assertEquals(false, connection.checkIfDataExists("Select * from Person Where ID = 20"));
        Assert.assertEquals("deleted person with Id: 20", st);
    }

    // With 'Write' access but ID not in DB
    @Test
    public void deletePersonWithWriteAndIdNotPresent() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/delete-person/21", "admin", "testPassword");
        Assert.assertEquals(404, eo.responseCode);
        Assert.assertEquals("Cannot find Person with id: 21", eo.errorData);
        Assert.assertEquals(true, connection.checkIfDataExists("Select * from Person Where ID = 20"));
    }



    // With 'Read' access and ID present in DB
    @Test
    public void deletePersonWithReadAndIdPresent() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/delete-person/20", "testUsername", "testPassword");
        Assert.assertEquals(true, connection.checkIfDataExists("Select * from Person Where ID = 20"));
        Assert.assertEquals(401, eo.responseCode);
    }


    // With 'Read' access and ID not present in DB
    @Test
    public void deletePersonWithReadAndIdNotPresent() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/delete-person/21", "testUsername", "testPassword");
        Assert.assertEquals(404, eo.responseCode);
        Assert.assertEquals("Cannot find Person with id: 21", eo.errorData);
    }


    // With incorrect authentication (Both password and username are incorrect) ID present in DB
    @Test
    public void deletePersonWithIncorrectAuthIdPresent() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/delete-person/20", "Test", "Test");
        Assert.assertEquals(true, connection.checkIfDataExists("Select * from Person Where ID = 20"));
        Assert.assertEquals(401, eo.responseCode);
    }

    // With incorrect authentication (Both password and username are incorrect) ID not present in DB
    @Test
    public void deletePersonWithIncorrectAuthIdNotPresent() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/delete-person/21", "Test", "Test");
        Assert.assertEquals(401, eo.responseCode);
    }


    // With incorrect 'Read' and 'Write' authentication (Correct password and incorrect username - both Read and Write have same password)
    @Test
    public void deletePersonWithIncorrectUsername() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/delete-person/20", "Test", "testPassword");
        Assert.assertEquals(401, eo.responseCode);
    }

    // Incorrect auth credentials 'Write' [Incorrect Username but correct password, ID present in DB]
    @Test
    public void deletePeronWithIncorrectWritePassAuth() throws IOException {
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/delete-person/20", "test", "testPassword");
        Assert.assertEquals(401, eo.responseCode);
    }

    // With incorrect 'Write' authentication (Incorrect password and incorrect username)
    @Test
    public void deletePersonWithIncorrectWritePass() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/delete-person/20", "admin", "Testing");
        Assert.assertEquals(401, eo.responseCode);
    }

    // With incorrect 'Write' authentication (Incorrect password and incorrect username)
    @Test
    public void postPersonWithIncorrectReadPass() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/delete-person/20", "testUsername", "Testing");
        Assert.assertEquals(401, eo.responseCode);
    }


    @After
    public void clearDB(){
        String query = "Delete from Person";
        connection.runQuery(query);
    }

}
