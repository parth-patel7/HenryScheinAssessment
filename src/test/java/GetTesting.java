import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;

public class GetTesting {

    DBConnection connection = new DBConnection();

    public Object connect(String link, String username, String password) throws IOException {
        URL url = new URL(link);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if(username != null && password != null && username != "" && password != "") {
            String authorization =
                    Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("authorization", "Basic " + authorization);
        }
        connection.setRequestMethod("GET");
        ObjectMapper mapper = new ObjectMapper();
        if (connection.getResponseCode() == 200) {
            InputStream responseStream = connection.getInputStream();
            Person p = mapper.readValue(responseStream, Person.class);
            return p;
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


    // With no authentication/authorization [ID present in DB]
    @Test
    public void getPersonWithoutAuthAndIdInDB() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/get-person/20", "", "");
        Assert.assertEquals(401, eo.responseCode);
        //Assert.assertEquals(true, connection.checkIfDataExists("Select * from Person Where ID = " + p.getId()));
    }


    // With no authentication/authorization [ID not present in DB]
    @Test
    public void getPersonWithoutAuthAndIdNotInDB() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/get-person/21", "", "");
        Assert.assertEquals(401, eo.responseCode);
    }


    // With 'Write' authentication/authorization [ID present in DB]
    @Test
    public void getPersonWithWriteAuthAndIdInDB() throws IOException{
        Person p = (Person) connect("http://localhost:8083/v1/get-person/20", "admin", "testPassword");
        Assert.assertEquals(20, p.getId());
        Assert.assertEquals("Test", p.getFirstName());
        Assert.assertEquals("Testing", p.getLastName());
        Assert.assertEquals("1231231231", p.getPhoneNumber());
    }


    // With 'Write' authentication/authorization [ID not present in DB]
    @Test
    public void getPersonWithWriteAuthButIdNotInDB() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/get-person/21", "admin", "testPassword");
        Assert.assertEquals(404, eo.responseCode);
        Assert.assertEquals("Cannot find Person with id: " + 21, eo.errorData);
    }


    // With 'Read' authentication/authorization [ID present in DB]
    @Test
    public void getPersonWithReadAuthAndIdInDB() throws IOException{
        Person p = (Person) connect("http://localhost:8083/v1/get-person/20", "testUsername", "testPassword");
        Assert.assertEquals(20, p.getId());
        Assert.assertEquals("Test", p.getFirstName());
        Assert.assertEquals("Testing", p.getLastName());
        Assert.assertEquals("1231231231", p.getPhoneNumber());
    }


    // With 'Read' authentication/authorization [ID not present in DB]
    @Test
    public void getPersonWithReadAuthButIdNotInDB() throws IOException{
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/get-person/21", "testUsername", "testPassword");
        Assert.assertEquals(404, eo.responseCode);
        Assert.assertEquals("Cannot find Person with id: " + 21, eo.errorData);
    }

    // Incorrect auth credentials [Both username and password are incorrect, ID present in DB]
    @Test
    public void getPeronWithIncorrectAuthAndIdInDB() throws IOException {
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/get-person/20", "test", "Test");
        Assert.assertEquals(401, eo.responseCode);
    }

    // Incorrect auth credentials [Both username and password are incorrect, ID not present in DB]
    @Test
    public void getPeronWithIncorrectAuthAndIdNotInDB() throws IOException {
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/get-person/21", "test", "Test");
        Assert.assertEquals(401, eo.responseCode);
    }


    // Incorrect auth credentials 'Write' and 'Read' [Incorrect Username but correct password, ID present in DB]
    @Test
    public void getPeronWithIncorrectUsernameAndIdInDB() throws IOException {
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/get-person/20", "test", "testPassword");
        Assert.assertEquals(401, eo.responseCode);
    }


    // Incorrect auth credentials 'Write' and 'Read' [Incorrect Username but correct password, ID not present in DB]
    @Test
    public void getPeronWithIncorrectUsernameButIdNotInDB() throws IOException {
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/get-person/21", "test", "testPassword");
        Assert.assertEquals(401, eo.responseCode);
    }


    // Incorrect auth credentials 'Write' [Correct Username but incorrect password, ID present in DB]
    @Test
    public void getPeronWithIncorrectWritePassAndIdInDB() throws IOException {
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/get-person/20", "admin", "testing");
        Assert.assertEquals(401, eo.responseCode);
    }

    // Incorrect auth credentials 'Write' [Correct Username but incorrect password, ID not present in DB]
    @Test
    public void getPeronWithIncorrectWritePassButIdNotInDB() throws IOException {
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/get-person/21", "admin", "testing");
        Assert.assertEquals(401, eo.responseCode);
    }

    // Incorrect auth credentials 'Read' [Correct Username but incorrect password, ID present in DB]
    @Test
    public void getPeronWithIncorrectReadPassAndIdInDB() throws IOException {
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/get-person/20", "testUsername", "testing");
        Assert.assertEquals(401, eo.responseCode);
    }

    // Incorrect auth credentials 'Read' [Correct Username but incorrect password, ID not present in DB]
    @Test
    public void getPeronWithIncorrectReadPassButIdNotInDB() throws IOException {
        ErrorObject eo = (ErrorObject) connect("http://localhost:8083/v1/get-person/21", "testUsername", "testing");
        Assert.assertEquals(401, eo.responseCode);
    }


    @After
    public void clearDB(){
        String query = "Delete from Person";
        connection.runQuery(query);
    }

}
